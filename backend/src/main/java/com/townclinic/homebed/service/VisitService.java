package com.townclinic.homebed.service;

import com.townclinic.homebed.common.ApiException;
import com.townclinic.homebed.common.DomainEnums.*;
import com.townclinic.homebed.dto.ReviewReq;
import com.townclinic.homebed.dto.VisitCompleteReq;
import com.townclinic.homebed.entity.*;
import com.townclinic.homebed.repo.*;
import com.townclinic.homebed.security.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 护士上门执行：签到、体征/护理记录、耗材领用出库、冷链温度、
 * 家属签字、医生远程复核；异常（超时/病情恶化/冷链断链）自动生成风险事件。
 */
@Service
public class VisitService {

    /** 冷链药品合法保存温度区间 ℃ */
    private static final double COLD_MIN = 2.0;
    private static final double COLD_MAX = 8.0;
    /** 超过计划耗时多少分钟记为上门超时事件 */
    private static final int OVERTIME_THRESHOLD_MIN = 20;

    private final VisitRepository visitRepo;
    private final VisitPlanRepository planRepo;
    private final VisitItemRepository itemRepo;
    private final VisitPhotoRepository photoRepo;
    private final CatalogItemRepository catalogRepo;
    private final BedApplicationRepository appRepo;
    private final RiskEventRepository riskRepo;
    private final AuditService audit;

    public VisitService(VisitRepository visitRepo, VisitPlanRepository planRepo,
                        VisitItemRepository itemRepo, VisitPhotoRepository photoRepo,
                        CatalogItemRepository catalogRepo, BedApplicationRepository appRepo,
                        RiskEventRepository riskRepo, AuditService audit) {
        this.visitRepo = visitRepo;
        this.planRepo = planRepo;
        this.itemRepo = itemRepo;
        this.photoRepo = photoRepo;
        this.catalogRepo = catalogRepo;
        this.appRepo = appRepo;
        this.riskRepo = riskRepo;
        this.audit = audit;
    }

    @Transactional
    public Visit start(Long planId) {
        AppUser nurse = Roles.require(Role.NURSE);
        VisitPlan plan = planRepo.findById(planId).orElseThrow(() -> new ApiException("巡诊计划不存在"));
        if (plan.getStatus() != PlanStatus.ACTIVE) throw new ApiException("该计划不在执行状态：" + plan.getStatus());
        if (plan.getNurseId() != null && !plan.getNurseId().equals(nurse.getId())) {
            throw new ApiException(403, "该上门任务分配给其他护士，请联系管理员改派");
        }
        Visit v = visitRepo.findByPlanId(planId).orElseGet(Visit::new);
        if (v.getId() != null && v.getStatus() == VisitStatus.COMPLETED) {
            throw new ApiException("该任务已完成，不能重复签到");
        }
        LocalDateTime now = LocalDateTime.now();
        v.setPlanId(planId);
        v.setApplicationId(plan.getApplicationId());
        v.setPatientId(plan.getPatientId());
        v.setNurseId(nurse.getId());
        v.setNurseSignName(nurse.getName());
        v.setStatus(VisitStatus.IN_PROGRESS);
        v.setStartedAt(now);
        v.setScheduledAt(plan.getPlanDate().atTime(plan.getPlannedTime() == null
                ? java.time.LocalTime.of(9, 0) : plan.getPlannedTime()));
        v.setReviewStatus(ReviewStatus.NONE);
        v = visitRepo.save(v);
        audit.log(plan.getApplicationId(), v.getId(), null, "VISIT_START",
                "护士 " + nurse.getName() + " 签到上门（计划时间 " + v.getScheduledAt() + "）");
        return v;
    }

    @Transactional
    public Visit complete(Long visitId, VisitCompleteReq req) {
        AppUser nurse = Roles.require(Role.NURSE);
        Visit v = visitRepo.findById(visitId).orElseThrow(() -> new ApiException("巡诊记录不存在"));
        if (!nurse.getId().equals(v.getNurseId())) throw new ApiException(403, "只能完成本人的上门任务");
        if (v.getStatus() == VisitStatus.COMPLETED) throw new ApiException("该巡诊已完成，不能重复提交");
        VisitPlan plan = planRepo.findById(v.getPlanId()).orElseThrow(() -> new ApiException("计划缺失"));

        LocalDateTime now = LocalDateTime.now();
        boolean aborted = req.abortType() != null && req.abortType() != VisitAbortType.NONE;

        // 体征
        v.setTemperature(req.temperature());
        v.setSystolic(req.systolic());
        v.setDiastolic(req.diastolic());
        v.setHeartRate(req.heartRate());
        v.setBloodGlucose(req.bloodGlucose());
        v.setSpo2(req.spo2());
        v.setPressureUlcer(req.pressureUlcer());
        v.setDressingChange(req.dressingChange());
        v.setCatheterCare(req.catheterCare());
        v.setMedicationReminder(req.medicationReminder());
        v.setNursingNote(req.nursingNote());
        v.setAbnormalNote(req.abnormalNote());
        v.setAbortType(req.abortType() == null ? VisitAbortType.NONE : req.abortType());
        v.setAbortNote(req.abortNote());
        v.setColdChainTemp(req.coldChainTemp());
        v.setCompletedAt(now);

        if (aborted) {
            // 服务未实际提供：不记收费、不扣库存、无家属签字
            v.setStatus(VisitStatus.CANCELED);
            v.setReviewStatus(ReviewStatus.NONE);
            plan.setStatus(PlanStatus.SUSPENDED);
            plan.setCancelReason("未完成：" + req.abortType() + " " + nz(req.abortNote()));
            planRepo.save(plan);
            visitRepo.save(v);
            audit.log(v.getApplicationId(), v.getId(), null, "VISIT_ABORT",
                    "护士 " + nurse.getName() + " 上门未完成，原因 " + req.abortType() + "：" + nz(req.abortNote())
                            + "（不计费、不扣耗材，等待卫生院重新安排）");
            // 家属不在/患者住院类事件提示管理
            if (req.abortType() == VisitAbortType.FAMILY_ABSENT
                    || req.abortType() == VisitAbortType.PATIENT_HOSPITALIZED) {
                createRisk(v, RiskEventType.OTHER,
                        "上门未完成（" + req.abortType() + "）：" + nz(req.abortNote()), nurse);
            }
            return v;
        }

        // 正常完成：必须家属签字
        if (req.familySignature() == null || req.familySignature().isBlank()
                || req.familySignName() == null || req.familySignName().isBlank()) {
            throw new ApiException("服务已完成，必须留存家属电子签字与签字人姓名");
        }
        v.setFamilySignature(truncateSignature(req.familySignature()));
        v.setFamilySignName(req.familySignName());
        v.setSignedAt(now);
        v.setStatus(VisitStatus.COMPLETED);

        // 耗时与超时
        int actual = (int) Duration.between(v.getStartedAt() == null ? now : v.getStartedAt(), now).toMinutes();
        v.setActualDurationMin(Math.max(actual, 1));
        int planned = plan.getDurationMin() == null ? 0 : plan.getDurationMin();
        int overtime = Math.max(0, v.getActualDurationMin() - planned);
        v.setOvertimeMin(overtime);

        // 领用与出库
        List<String> usedNames = new ArrayList<>();
        boolean coldUsed = false;
        if (req.items() != null) {
            for (VisitCompleteReq.ItemUseReq u : req.items()) {
                if (u.quantity() == null || u.quantity().compareTo(BigDecimal.ZERO) <= 0) continue;
                CatalogItem c = catalogRepo.findById(u.itemId())
                        .orElseThrow(() -> new ApiException("目录项目不存在：" + u.itemId()));
                if (!c.isEnabled()) throw new ApiException("项目已停用，不能领用：" + c.getName());
                if (c.getCategory() == ItemCategory.CONSUMABLE
                        || c.getCategory() == ItemCategory.DRUG
                        || c.getCategory() == ItemCategory.DRUG_COLD) {
                    int need = u.quantity().intValue();
                    if (c.getStockQty() < need) {
                        throw new ApiException("耗材/药品库存不足：" + c.getName()
                                + "（库存 " + c.getStockQty() + "，需 " + need + "），请先补充库存或更换方案");
                    }
                    c.setStockQty(c.getStockQty() - need);
                    catalogRepo.save(c);
                }
                VisitItem vi = new VisitItem();
                vi.setVisitId(v.getId());
                vi.setItemId(c.getId());
                vi.setItemCode(c.getCode());
                vi.setItemName(c.getName());
                vi.setKind(mapKind(c.getCategory()));
                vi.setQuantity(u.quantity());
                vi.setUnit(c.getUnit());
                vi.setUnitPrice(c.getPrice());
                vi.setInsuranceCovered(c.isInsuranceCovered());
                vi.setReimburseRatio(c.getReimburseRatio());
                vi.setColdChain(c.isColdChain());
                vi.setStockDeducted(true);
                itemRepo.save(vi);
                usedNames.add(c.getName() + "x" + u.quantity().stripTrailingZeros().toPlainString());
                if (c.isColdChain()) coldUsed = true;
            }
        }

        // 远程复核：完成即待医生复核
        v.setReviewStatus(ReviewStatus.PENDING);
        visitRepo.save(v);

        audit.log(v.getApplicationId(), v.getId(), null, "VISIT_COMPLETE",
                "护士 " + nurse.getName() + " 完成巡诊，家属签字：" + req.familySignName()
                        + "；项目：" + (usedNames.isEmpty() ? "无" : String.join("、", usedNames))
                        + (overtime > 0 ? "；超时" + overtime + "分钟" : ""));

        // ===== 自动风险事件 =====
        if (overtime >= OVERTIME_THRESHOLD_MIN) {
            createRisk(v, RiskEventType.OVERTIME,
                    "护士上门超时：实际 " + v.getActualDurationMin() + " 分钟，计划 " + planned
                            + " 分钟，超时 " + overtime + " 分钟", nurse);
        }
        if (req.abnormalNote() != null && !req.abnormalNote().isBlank()) {
            createRisk(v, RiskEventType.DETERIORATION,
                    "巡诊发现病情异常：" + req.abnormalNote(), nurse);
        }
        if (coldUsed) {
            if (req.coldChainTemp() == null) {
                createRisk(v, RiskEventType.COLD_CHAIN,
                        "本次使用冷链药品但未记录交接温度，需补录并核查冷链", nurse);
            } else if (req.coldChainTemp() < COLD_MIN || req.coldChainTemp() > COLD_MAX) {
                createRisk(v, RiskEventType.COLD_CHAIN,
                        "冷链药品交接温度 " + req.coldChainTemp() + "℃ 超出 "
                                + COLD_MIN + "~" + COLD_MAX + "℃ 区间，药品有效性存疑", nurse);
            }
        }
        return v;
    }

    @Transactional
    public VisitPhoto uploadPhoto(Long visitId, String category, String remark,
                                  org.springframework.web.multipart.MultipartFile file,
                                  FileStorageService storage) {
        AppUser nurse = Roles.require(Role.NURSE, Role.DOCTOR);
        Visit v = visitRepo.findById(visitId).orElseThrow(() -> new ApiException("巡诊记录不存在"));
        String url = storage.storeImage(file);
        VisitPhoto p = new VisitPhoto();
        p.setVisitId(visitId);
        p.setUrl(url);
        p.setCategory(category);
        p.setRemark(remark);
        p.setUploaderId(nurse.getId());
        p = photoRepo.save(p);
        audit.log(v.getApplicationId(), visitId, null, "PHOTO",
                nurse.getName() + " 上传现场照片（" + nz(category) + "）：" + nz(remark));
        return p;
    }

    @Transactional
    public Visit review(ReviewReq req) {
        AppUser doctor = Roles.require(Role.DOCTOR);
        Visit v = visitRepo.findById(req.visitId()).orElseThrow(() -> new ApiException("巡诊记录不存在"));
        if (v.getStatus() != VisitStatus.COMPLETED) {
            throw new ApiException("仅已完成的巡诊记录可远程复核，当前状态：" + v.getStatus());
        }
        v.setReviewStatus(req.escalated() ? ReviewStatus.ESCALATED : ReviewStatus.REVIEWED);
        v.setReviewDoctorId(doctor.getId());
        v.setReviewedAt(LocalDateTime.now());
        v.setReviewNote(req.reviewNote());
        visitRepo.save(v);
        audit.log(v.getApplicationId(), v.getId(), null, "DOCTOR_REVIEW",
                "医生 " + doctor.getName() + " 远程复核：" + nz(req.reviewNote())
                        + (req.escalated() ? "（已升级，建议转诊/上级会诊）" : ""));
        return v;
    }

    public List<Visit> pendingReviews() {
        Roles.require(Role.DOCTOR, Role.ADMIN);
        return visitRepo.findByReviewStatus(ReviewStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> nurseVisits(Long nurseId) {
        AppUser me = Roles.requireLogin();
        if (me.getRole() == Role.NURSE) nurseId = me.getId();
        return visitRepo.findByNurseIdOrderByCreatedAtDesc(nurseId).stream().map(this::visitBundle).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getVisitBundle(Long visitId) {
        Roles.requireLogin();
        Visit v = visitRepo.findById(visitId).orElseThrow(() -> new ApiException("巡诊记录不存在"));
        return visitBundle(v);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> visitBundle(Visit v) {
        Roles.requireLogin();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("visit", v);
        m.put("items", itemRepo.findByVisitId(v.getId()));
        m.put("photos", photoRepo.findByVisitId(v.getId()));
        return m;
    }

    private void createRisk(Visit v, RiskEventType type, String desc, AppUser reporter) {
        RiskEvent e = new RiskEvent();
        e.setApplicationId(v.getApplicationId());
        e.setVisitId(v.getId());
        e.setPatientId(v.getPatientId());
        e.setEventType(type);
        e.setDescription(desc);
        e.setReporterId(reporter.getId());
        e.setStatus(EventStatus.OPEN);
        riskRepo.save(e);
    }

    private UsageKind mapKind(ItemCategory c) {
        return switch (c) {
            case SERVICE -> UsageKind.SERVICE;
            case CONSUMABLE -> UsageKind.CONSUMABLE;
            case DRUG, DRUG_COLD -> UsageKind.DRUG;
        };
    }

    /** 控制签字 dataURL 落库长度 */
    private String truncateSignature(String s) {
        int max = 200_000;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private String nz(String s) { return s == null ? "" : s; }
}
