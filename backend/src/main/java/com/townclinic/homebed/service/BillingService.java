package com.townclinic.homebed.service;

import com.townclinic.homebed.common.ApiException;
import com.townclinic.homebed.common.DomainEnums.*;
import com.townclinic.homebed.dto.BillGenerateReq;
import com.townclinic.homebed.dto.ConfirmBillReq;
import com.townclinic.homebed.dto.QuestionReplyReq;
import com.townclinic.homebed.dto.QuestionReq;
import com.townclinic.homebed.entity.*;
import com.townclinic.homebed.repo.*;
import com.townclinic.homebed.security.CurrentUser;
import com.townclinic.homebed.security.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * 统一账单：把耗材领用、冷链药品、护理收费、医保可报销项与自费项、
 * 家属确认汇总在同一张账单中；生成时即做重复收费/超频次/目录变化合规校验。
 */
@Service
public class BillingService {

    private final BillRepository billRepo;
    private final BillItemRepository billItemRepo;
    private final VisitItemRepository visitItemRepo;
    private final VisitRepository visitRepo;
    private final BedApplicationRepository appRepo;
    private final CatalogItemRepository catalogRepo;
    private final ChargeQuestionRepository questionRepo;
    private final RiskEventRepository riskRepo;
    private final AuditService audit;

    public BillingService(BillRepository billRepo, BillItemRepository billItemRepo,
                          VisitItemRepository visitItemRepo, VisitRepository visitRepo,
                          BedApplicationRepository appRepo, CatalogItemRepository catalogRepo,
                          ChargeQuestionRepository questionRepo, RiskEventRepository riskRepo,
                          AuditService audit) {
        this.billRepo = billRepo;
        this.billItemRepo = billItemRepo;
        this.visitItemRepo = visitItemRepo;
        this.visitRepo = visitRepo;
        this.appRepo = appRepo;
        this.catalogRepo = catalogRepo;
        this.questionRepo = questionRepo;
        this.riskRepo = riskRepo;
        this.audit = audit;
    }

    // ============ 生成账单 ============

    @Transactional
    public Map<String, Object> generate(BillGenerateReq req) {
        AppUser u = Roles.require(Role.ADMIN, Role.INSURANCE, Role.DOCTOR);
        BedApplication a = appRepo.findById(req.applicationId())
                .orElseThrow(() -> new ApiException("申请不存在"));

        LocalDate start = req.start() == null
                ? a.getBedStartDate() == null ? LocalDate.now().withDayOfMonth(1) : a.getBedStartDate()
                : req.start();
        LocalDate end = req.end() == null ? LocalDate.now() : req.end();

        // 取周期内已完成巡诊
        List<Visit> visits = visitRepo.findByApplicationIdOrderByCreatedAtDesc(a.getId()).stream()
                .filter(v -> v.getStatus() == VisitStatus.COMPLETED)
                .filter(v -> !v.getCompletedAt().toLocalDate().isBefore(start)
                        && !v.getCompletedAt().toLocalDate().isAfter(end))
                .toList();
        List<Long> visitIds = visits.stream().map(Visit::getId).toList();

        List<VisitItem> all = visitIds.isEmpty() ? List.of() : visitItemRepo.findByVisitIdIn(visitIds);
        // 仅归集尚未进入任何账单的领用记录，避免重复收费
        List<VisitItem> unbilled = all.stream()
                .filter(vi -> !billItemRepo.existsByVisitItemId(vi.getId())).toList();
        if (unbilled.isEmpty()) throw new ApiException("周期内没有新的待结算项目（可能已生成过账单）");

        Bill bill = new Bill();
        bill.setBillNo("ZF" + System.currentTimeMillis());
        bill.setApplicationId(a.getId());
        bill.setPatientId(a.getPatientId());
        bill.setFamilyUserId(a.getFamilyUserId());
        bill.setPeriodStart(start);
        bill.setPeriodEnd(end);
        bill.setStatus(BillStatus.DRAFT);
        bill = billRepo.save(bill);

        Map<String, CatalogItem> currentCatalog = new HashMap<>();
        for (CatalogItem c : catalogRepo.findAll()) currentCatalog.put(c.getCode(), c);

        // 合规统计：编码 -> 按日/按周/按次/按周期计数
        Map<String, Integer> perPeriod = new HashMap<>();
        Map<String, Map<LocalDate, Integer>> perDay = new HashMap<>();
        Map<String, Map<Integer, Integer>> perWeek = new HashMap<>();
        Map<Long, Map<String, Integer>> perVisit = new HashMap<>();

        BigDecimal total = BigDecimal.ZERO, ins = BigDecimal.ZERO, self = BigDecimal.ZERO, cold = BigDecimal.ZERO;
        List<String> issues = new ArrayList<>();
        for (VisitItem vi : unbilled) {
            Visit v = visits.stream().filter(x -> x.getId().equals(vi.getVisitId())).findFirst().orElse(null);
            BillItem bi = new BillItem();
            bi.setBillId(bill.getId());
            bi.setVisitId(vi.getVisitId());
            bi.setVisitItemId(vi.getId());
            bi.setItemCode(vi.getItemCode());
            bi.setItemName(vi.getItemName());
            bi.setKind(vi.getKind());
            bi.setQuantity(vi.getQuantity());
            bi.setUnit(vi.getUnit());
            bi.setUnitPrice(vi.getUnitPrice());
            BigDecimal amount = vi.getUnitPrice().multiply(vi.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            bi.setAmount(amount);
            bi.setInsuranceCovered(vi.isInsuranceCovered());
            bi.setReimburseRatio(vi.getReimburseRatio());
            bi.setColdChain(vi.isColdChain());
            bi.setFamilyAck(false);
            bi.setServiceTime(v == null ? null : v.getCompletedAt());

            BigDecimal insPart = BigDecimal.ZERO;
            if (vi.isInsuranceCovered() && vi.getReimburseRatio() != null && vi.getReimburseRatio() > 0) {
                insPart = amount.multiply(BigDecimal.valueOf(vi.getReimburseRatio()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            bi.setInsuranceAmount(insPart);
            bi.setSelfPayAmount(amount.subtract(insPart));

            // ===== 合规规则判定（仅标记，不阻断；医保经办复核） =====
            List<String> flags = new ArrayList<>();
            CatalogItem nowItem = currentCatalog.get(vi.getItemCode());
            if (!vi.isInsuranceCovered() || vi.getReimburseRatio() == null || vi.getReimburseRatio() == 0) {
                flags.add("自费项目");
            }
            if (nowItem != null && nowItem.isEnabled()
                    && (nowItem.isInsuranceCovered() != vi.isInsuranceCovered()
                        || !Objects.equals(nowItem.getReimburseRatio(), vi.getReimburseRatio())
                        || nowItem.getPrice().compareTo(vi.getUnitPrice()) != 0)) {
                flags.add("医保目录/价格已变化（按服务当日标准结算）");
            }
            if (nowItem == null || !nowItem.isEnabled()) {
                flags.add("项目已移出当前目录");
            }

            // 频次限制
            String freq = nowItem == null ? null : nowItem.getFrequencyLimit();
            int q = vi.getQuantity().intValue();
            perPeriod.merge(vi.getItemCode(), q, Integer::sum);
            LocalDate day = v == null ? null : v.getCompletedAt().toLocalDate();
            if (day != null) perDay.computeIfAbsent(vi.getItemCode(), k -> new HashMap<>()).merge(day, q, Integer::sum);
            int week = day == null ? 0 : day.get(WeekFields.ISO.weekOfWeekBasedYear());
            if (day != null) perWeek.computeIfAbsent(vi.getItemCode(), k -> new HashMap<>()).merge(week, q, Integer::sum);
            Map<String, Integer> pv = perVisit.computeIfAbsent(vi.getVisitId(), k -> new HashMap<>());
            pv.merge(vi.getItemCode(), q, Integer::sum);
            if (freq != null) {
                String over = checkFrequency(freq, vi.getItemCode(), day, week, v == null ? null : v.getId(),
                        perPeriod, perDay, perWeek, perVisit);
                if (over != null) flags.add(over);
            }
            if (!flags.isEmpty()) {
                bi.setComplianceFlag(String.join("；", flags));
                issues.add(vi.getItemName() + "：" + String.join("、", flags));
            }
            billItemRepo.save(bi);

            total = total.add(amount);
            ins = ins.add(insPart);
            self = self.add(amount.subtract(insPart));
            if (vi.isColdChain()) cold = cold.add(amount);
        }

        bill.setTotalAmount(total);
        bill.setInsuranceAmount(ins);
        bill.setSelfPayAmount(self);
        bill.setColdChainAmount(cold);
        bill.setComplianceIssues(issues.isEmpty() ? null : String.join("\n", dedup(issues)));
        bill = billRepo.save(bill);

        audit.log(a.getId(), null, bill.getId(), "BILL_GENERATE",
                "生成账单 " + bill.getBillNo() + "：合计 " + total + " 元，医保报销 " + ins
                        + " 元，自付 " + self + " 元，冷链药费 " + cold + " 元"
                        + (issues.isEmpty() ? "" : "；命中合规提示 " + dedup(issues).size() + " 条"));
        return billDetail(bill.getId());
    }

    private String checkFrequency(String freq, String code, LocalDate day, int week, Long visitId,
                                  Map<String, Integer> perPeriod,
                                  Map<String, Map<LocalDate, Integer>> perDay,
                                  Map<String, Map<Integer, Integer>> perWeek,
                                  Map<Long, Map<String, Integer>> perVisit) {
        String[] parts = freq.split("/");
        if (parts.length != 2) return null;
        int limit;
        try { limit = Integer.parseInt(parts[0].trim()); } catch (Exception e) { return null; }
        String unit = parts[1].trim();
        int actual = switch (unit) {
            case "日" -> day == null ? 0 : perDay.getOrDefault(code, Map.of()).getOrDefault(day, 0);
            case "周" -> day == null ? 0 : perWeek.getOrDefault(code, Map.of()).getOrDefault(week, 0);
            case "周期" -> perPeriod.getOrDefault(code, 0);
            case "次" -> visitId == null ? 0 : perVisit.getOrDefault(visitId, Map.of()).getOrDefault(code, 0);
            default -> -1;
        };
        if (actual > limit) return "超频次收费（限 " + freq + "，实际 " + actual + "）";
        return null;
    }

    // ============ 家属确认 / 质疑 ============

    @Transactional
    public Map<String, Object> confirm(Long billId, ConfirmBillReq req) {
        AppUser family = Roles.require(Role.FAMILY);
        Bill bill = mustBill(billId);
        if (!bill.getFamilyUserId().equals(family.getId())) throw new ApiException(403, "只能确认本家庭账单");
        if (bill.getStatus() == BillStatus.SETTLED) throw new ApiException("账单已结算，无需确认");
        if (req.ackItemIds() != null) {
            for (BillItem bi : billItemRepo.findByBillId(billId)) {
                if (req.ackItemIds().contains(bi.getId())) bi.setFamilyAck(true);
                billItemRepo.save(bi);
            }
        }
        bill.setStatus(BillStatus.CONFIRMED);
        bill.setFamilyConfirmNote(req.familyConfirmNote());
        bill.setConfirmedAt(LocalDateTime.now());
        billRepo.save(bill);
        audit.log(bill.getApplicationId(), null, bill.getId(), "BILL_CONFIRM",
                "家属 " + family.getName() + " 确认账单 " + bill.getBillNo()
                        + "，意见：" + (req.familyConfirmNote() == null ? "" : req.familyConfirmNote()));
        return billDetail(billId);
    }

    @Transactional
    public ChargeQuestion ask(QuestionReq req) {
        AppUser family = Roles.require(Role.FAMILY);
        Bill bill = req.billId() == null ? null : billRepo.findById(req.billId()).orElse(null);
        Long applicationId;
        if (bill != null) {
            if (!bill.getFamilyUserId().equals(family.getId())) throw new ApiException(403, "只能质疑本家庭账单");
            applicationId = bill.getApplicationId();
            bill.setStatus(BillStatus.QUESTIONED);
            billRepo.save(bill);
        } else {
            if (req.applicationId() == null) throw new ApiException("请选择账单或对应的家庭病床申请");
            BedApplication a = appRepo.findById(req.applicationId())
                    .orElseThrow(() -> new ApiException("申请不存在"));
            if (!a.getFamilyUserId().equals(family.getId())) throw new ApiException(403, "只能对本家庭申请提出质疑");
            applicationId = a.getId();
        }
        ChargeQuestion q = new ChargeQuestion();
        q.setApplicationId(applicationId);
        q.setBillId(req.billId());
        q.setBillItemId(req.billItemId());
        q.setFamilyUserId(family.getId());
        q.setContent(req.content());
        q.setStatus(QuestionStatus.OPEN);
        q = questionRepo.save(q);
        // 收费投诉同时形成风险事件，供管理者跟踪
        if (bill != null) {
            RiskEvent e = new RiskEvent();
            e.setApplicationId(bill.getApplicationId());
            e.setPatientId(bill.getPatientId());
            e.setEventType(RiskEventType.COMPLAINT);
            e.setDescription("家属质疑收费（账单 " + bill.getBillNo() + "）：" + req.content());
            e.setReporterId(family.getId());
            e.setStatus(EventStatus.OPEN);
            riskRepo.save(e);
        }
        audit.log(q.getApplicationId(), null, req.billId(), "BILL_QUESTION",
                "家属 " + family.getName() + " 质疑收费：" + req.content());
        return q;
    }

    @Transactional
    public ChargeQuestion reply(QuestionReplyReq req) {
        AppUser u = Roles.require(Role.ADMIN, Role.INSURANCE, Role.DOCTOR);
        ChargeQuestion q = questionRepo.findById(req.questionId())
                .orElseThrow(() -> new ApiException("质疑记录不存在"));
        q.setReply(req.reply());
        q.setReplyUserId(u.getId());
        q.setRepliedAt(LocalDateTime.now());
        if (req.refundAmount() != null && req.refundAmount().compareTo(BigDecimal.ZERO) > 0) {
            q.setRefundAmount(req.refundAmount());
            Bill bill = billRepo.findById(q.getBillId()).orElse(null);
            if (bill != null) {
                BigDecimal refund = req.refundAmount().min(bill.getSelfPayAmount());
                bill.setSelfPayAmount(bill.getSelfPayAmount().subtract(refund));
                bill.setTotalAmount(bill.getTotalAmount().subtract(refund));
                billRepo.save(bill);
            }
        }
        q.setStatus(req.resolved() ? QuestionStatus.RESOLVED : QuestionStatus.REPLIED);
        questionRepo.save(q);
        // 该账单无未解决质疑时恢复为已确认
        if (q.getStatus() == QuestionStatus.RESOLVED) {
            boolean stillOpen = questionRepo.findByApplicationIdOrderByCreatedAtDesc(q.getApplicationId()).stream()
                    .anyMatch(x -> x.getBillId() != null && x.getBillId().equals(q.getBillId())
                            && x.getStatus() != QuestionStatus.RESOLVED && x.getStatus() != QuestionStatus.REJECTED);
            if (!stillOpen && q.getBillId() != null) {
                billRepo.findById(q.getBillId()).ifPresent(b -> {
                    if (b.getStatus() == BillStatus.QUESTIONED) {
                        b.setStatus(BillStatus.CONFIRMED);
                        billRepo.save(b);
                    }
                });
            }
        }
        audit.log(q.getApplicationId(), null, q.getBillId(), "QUESTION_REPLY",
                u.getName() + " 答复收费质疑：" + req.reply()
                        + (req.refundAmount() != null && req.refundAmount().signum() > 0
                            ? "；退费 " + req.refundAmount() + " 元" : "")
                        + (req.resolved() ? "；已解决" : ""));
        return q;
    }

    @Transactional
    public Bill settle(Long billId) {
        Roles.require(Role.ADMIN, Role.INSURANCE);
        Bill bill = mustBill(billId);
        if (bill.getStatus() != BillStatus.CONFIRMED) {
            throw new ApiException("仅家属已确认的账单可结算，当前状态：" + bill.getStatus());
        }
        bill.setStatus(BillStatus.SETTLED);
        bill.setSettledAt(LocalDateTime.now());
        billRepo.save(bill);
        audit.log(bill.getApplicationId(), null, bill.getId(), "BILL_SETTLE",
                "账单 " + bill.getBillNo() + " 医保结算完成：医保支付 " + bill.getInsuranceAmount()
                        + " 元，个人自付 " + bill.getSelfPayAmount() + " 元");
        return bill;
    }

    // ============ 查询 ============

    @Transactional(readOnly = true)
    public Map<String, Object> billDetail(Long billId) {
        Roles.requireLogin();
        Bill bill = mustBill(billId);
        AppUser me = CurrentUser.get();
        if (me.getRole() == Role.FAMILY && !bill.getFamilyUserId().equals(me.getId())) {
            throw new ApiException(403, "只能查看本家庭账单");
        }
        List<BillItem> items = billItemRepo.findByBillId(billId);
        boolean allAck = !items.isEmpty() && items.stream().allMatch(BillItem::isFamilyAck);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bill", bill);
        m.put("items", items);
        m.put("allItemAck", allAck);
        m.put("questions", questionRepo.findByApplicationIdOrderByCreatedAtDesc(bill.getApplicationId()).stream()
                .filter(q -> billId.equals(q.getBillId())).toList());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Bill> myBills() {
        AppUser me = Roles.require(Role.FAMILY);
        return billRepo.findByFamilyUserIdOrderByCreatedAtDesc(me.getId());
    }

    @Transactional(readOnly = true)
    public List<Bill> allBills(String status) {
        Roles.require(Role.ADMIN, Role.INSURANCE, Role.DOCTOR);
        if (status == null || status.isBlank()) return billRepo.findAll();
        return billRepo.findByStatus(BillStatus.valueOf(status));
    }

    /** 医保合规总览：所有命中过合规标记的账单明细 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> complianceList() {
        Roles.require(Role.INSURANCE, Role.ADMIN);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Bill b : billRepo.findAll()) {
            for (BillItem bi : billItemRepo.findByBillId(b.getId())) {
                if (bi.getComplianceFlag() != null) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("billId", b.getId());
                    m.put("billNo", b.getBillNo());
                    m.put("billStatus", b.getStatus());
                    m.put("applicationId", b.getApplicationId());
                    m.put("itemName", bi.getItemName());
                    m.put("itemCode", bi.getItemCode());
                    m.put("amount", bi.getAmount());
                    m.put("insuranceAmount", bi.getInsuranceAmount());
                    m.put("serviceTime", bi.getServiceTime());
                    m.put("familyAck", bi.isFamilyAck());
                    m.put("flag", bi.getComplianceFlag());
                    out.add(m);
                }
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<ChargeQuestion> allQuestions() {
        Roles.require(Role.ADMIN, Role.INSURANCE, Role.DOCTOR);
        return questionRepo.findAllByOrderByCreatedAtDesc();
    }

    private Bill mustBill(Long id) {
        return billRepo.findById(id).orElseThrow(() -> new ApiException("账单不存在"));
    }

    private List<String> dedup(List<String> in) {
        return new ArrayList<>(new LinkedHashSet<>(in));
    }
}
