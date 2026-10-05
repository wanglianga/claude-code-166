package com.townclinic.homebed.service;

import com.townclinic.homebed.common.DomainEnums.*;
import com.townclinic.homebed.entity.*;
import com.townclinic.homebed.repo.*;
import com.townclinic.homebed.security.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** 卫生院管理者看板：巡诊负荷、上门半径、耗材消耗、风险事件、结算概况。 */
@Service
public class StatsService {

    private final BedApplicationRepository appRepo;
    private final VisitPlanRepository planRepo;
    private final VisitRepository visitRepo;
    private final VisitItemRepository visitItemRepo;
    private final CatalogItemRepository catalogRepo;
    private final AppUserRepository userRepo;
    private final RiskEventRepository riskRepo;
    private final BillRepository billRepo;
    private final BillItemRepository billItemRepo;

    public StatsService(BedApplicationRepository appRepo, VisitPlanRepository planRepo,
                        VisitRepository visitRepo, VisitItemRepository visitItemRepo,
                        CatalogItemRepository catalogRepo, AppUserRepository userRepo,
                        RiskEventRepository riskRepo, BillRepository billRepo,
                        BillItemRepository billItemRepo) {
        this.appRepo = appRepo;
        this.planRepo = planRepo;
        this.visitRepo = visitRepo;
        this.visitItemRepo = visitItemRepo;
        this.catalogRepo = catalogRepo;
        this.userRepo = userRepo;
        this.riskRepo = riskRepo;
        this.billRepo = billRepo;
        this.billItemRepo = billItemRepo;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        Roles.require(Role.ADMIN, Role.DOCTOR, Role.INSURANCE);
        LocalDate today = LocalDate.now();
        Map<String, Object> out = new LinkedHashMap<>();

        // 床位/申请
        long active = appRepo.countByStatus(ApplicationStatus.ACTIVE);
        long paused = appRepo.countByStatus(ApplicationStatus.PAUSED);
        long submitted = appRepo.countByStatus(ApplicationStatus.SUBMITTED);
        long assessed = appRepo.countByStatus(ApplicationStatus.ASSESSED);
        out.put("beds", Map.of("active", active, "paused", paused,
                "submitted", submitted, "assessed", assessed));

        // 今日护士负荷
        List<VisitPlan> todayPlans = planRepo
                .findByPlanDateAndStatusOrderByNurseIdAscRouteOrderAsc(today, PlanStatus.ACTIVE);
        Map<Long, int[]> loadMap = new HashMap<>(); // [minutes, stops]
        Map<Long, Double> kmMap = new HashMap<>();
        for (VisitPlan p : todayPlans) {
            if (p.getNurseId() == null) continue;
            int[] arr = loadMap.computeIfAbsent(p.getNurseId(), k -> new int[2]);
            arr[0] += p.getDurationMin() == null ? 0 : p.getDurationMin();
            arr[1]++;
            kmMap.merge(p.getNurseId(), p.getDistanceKm() == null ? 0.0 : p.getDistanceKm(), Double::sum);
        }
        List<Map<String, Object>> loads = new ArrayList<>();
        for (AppUser n : userRepo.findByRole(Role.NURSE)) {
            int[] arr = loadMap.getOrDefault(n.getId(), new int[2]);
            int cap = n.getDailyCapacityMin() == null ? 480 : n.getDailyCapacityMin();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nurseName", n.getName());
            m.put("plannedMin", arr[0]);
            m.put("stops", arr[1]);
            m.put("capacityMin", cap);
            m.put("loadRatio", Math.round(arr[0] * 100.0 / cap));
            m.put("roundTripKm", kmMap.getOrDefault(n.getId(), 0.0));
            m.put("overload", arr[0] > cap);
            loads.add(m);
        }
        out.put("nurseLoadToday", loads);

        // 上门半径
        DoubleSummaryStatistics dist = appRepo.findAll().stream()
                .filter(a -> a.getDistanceKm() != null)
                .mapToDouble(BedApplication::getDistanceKm).summaryStatistics();
        Map<String, Object> radius = new LinkedHashMap<>();
        radius.put("averageKm", Math.round(dist.getAverage() * 10) / 10.0);
        radius.put("maxKm", dist.getMax() == Double.NEGATIVE_INFINITY ? 0 : dist.getMax());
        radius.put("overRadiusBeds", appRepo.findAll().stream()
                .filter(a -> a.getDistanceKm() != null && a.getDistanceKm() > 8.0).count());
        out.put("radius", radius);

        // 巡诊执行
        List<Visit> visits = visitRepo.findAll();
        long completed = visits.stream().filter(v -> v.getStatus() == VisitStatus.COMPLETED).count();
        long aborted = visits.stream().filter(v -> v.getStatus() == VisitStatus.CANCELED).count();
        long overtime = visits.stream().filter(v -> v.getOvertimeMin() != null && v.getOvertimeMin() >= 20).count();
        long pendingReview = visits.stream().filter(v -> v.getReviewStatus() == ReviewStatus.PENDING).count();
        out.put("visits", Map.of("completed", completed, "aborted", aborted,
                "overtime", overtime, "pendingReview", pendingReview, "total", visits.size()));

        // 耗材/药品消耗（全部已完成巡诊）
        List<Long> completedVisitIds = visits.stream()
                .filter(v -> v.getStatus() == VisitStatus.COMPLETED).map(Visit::getId).toList();
        Map<Long, BigDecimal> qtyByItem = new LinkedHashMap<>();
        BigDecimal coldAmount = BigDecimal.ZERO;
        if (!completedVisitIds.isEmpty()) {
            for (VisitItem vi : visitItemRepo.findByVisitIdIn(completedVisitIds)) {
                if (vi.getKind() != UsageKind.SERVICE) {
                    qtyByItem.merge(vi.getItemId(), vi.getQuantity(), BigDecimal::add);
                }
                if (vi.isColdChain()) {
                    coldAmount = coldAmount.add(vi.getUnitPrice().multiply(vi.getQuantity()));
                }
            }
        }
        List<Map<String, Object>> consumption = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : qtyByItem.entrySet()) {
            CatalogItem c = catalogRepo.findById(e.getKey()).orElse(null);
            if (c == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", c.getName());
            m.put("category", c.getCategory());
            m.put("usedQty", e.getValue());
            m.put("unit", c.getUnit());
            m.put("stockQty", c.getStockQty());
            m.put("safetyStock", c.getSafetyStock());
            m.put("lowStock", c.getStockQty() <= c.getSafetyStock());
            m.put("coldChain", c.isColdChain());
            consumption.add(m);
        }
        consumption.sort((x, y) -> ((BigDecimal) y.get("usedQty")).compareTo((BigDecimal) x.get("usedQty")));
        out.put("consumption", consumption);
        out.put("coldChainAmount", coldAmount);

        // 风险事件
        List<RiskEvent> events = riskRepo.findAll();
        Map<String, Long> byType = new LinkedHashMap<>();
        for (RiskEventType t : RiskEventType.values()) {
            byType.put(t.name(), events.stream().filter(e -> e.getEventType() == t).count());
        }
        out.put("riskEvents", Map.of(
                "open", events.stream().filter(e -> e.getStatus() != EventStatus.CLOSED).count(),
                "total", events.size(), "byType", byType));

        // 结算概况
        List<Bill> bills = billRepo.findAll();
        BigDecimal total = bills.stream().map(Bill::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ins = bills.stream().map(Bill::getInsuranceAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal self = bills.stream().map(Bill::getSelfPayAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long flagged = 0;
        for (Bill b : bills) flagged += billItemRepo.findByBillId(b.getId()).stream()
                .filter(bi -> bi.getComplianceFlag() != null).count();
        Map<String, Object> billStat = new LinkedHashMap<>();
        billStat.put("count", bills.size());
        billStat.put("totalAmount", total);
        billStat.put("insuranceAmount", ins);
        billStat.put("selfPayAmount", self);
        billStat.put("questioned", bills.stream().filter(b -> b.getStatus() == BillStatus.QUESTIONED).count());
        billStat.put("settled", bills.stream().filter(b -> b.getStatus() == BillStatus.SETTLED).count());
        billStat.put("complianceFlaggedItems", flagged);
        out.put("billing", billStat);
        return out;
    }
}
