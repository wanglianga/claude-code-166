package com.townclinic.homebed.service;

import com.townclinic.homebed.common.ApiException;
import com.townclinic.homebed.common.DomainEnums.*;
import com.townclinic.homebed.dto.GeneratePlanReq;
import com.townclinic.homebed.dto.PlanCancelReq;
import com.townclinic.homebed.entity.*;
import com.townclinic.homebed.repo.*;
import com.townclinic.homebed.security.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * 巡诊排班与上门路线。
 * 在有限的护士资源下，综合 上门半径、护理耗时、护士负荷、患者风险、家属期望频次
 * 给出可解释的安排（scheduleNote 记录选择理由与冲突提示）。
 */
@Service
public class SchedulingService {

    /** 卫生院标准上门半径（公里） */
    private static final double SERVICE_RADIUS_KM = 8.0;
    /** 每公里单程折算分钟（村道/山路较慢） */
    private static final double MIN_PER_KM_ONE_WAY = 3.0;
    private static final int MIN_TRAVEL_MIN = 12;
    private static final LocalTime SHIFT_START = LocalTime.of(8, 30);

    private final VisitPlanRepository planRepo;
    private final BedApplicationRepository appRepo;
    private final AppUserRepository userRepo;
    private final CatalogItemRepository catalogRepo;
    private final AuditService audit;

    public SchedulingService(VisitPlanRepository planRepo, BedApplicationRepository appRepo,
                             AppUserRepository userRepo, CatalogItemRepository catalogRepo,
                             AuditService audit) {
        this.planRepo = planRepo;
        this.appRepo = appRepo;
        this.userRepo = userRepo;
        this.catalogRepo = catalogRepo;
        this.audit = audit;
    }

    // ============ 批量生成计划并自动排班 ============

    @Transactional
    public Map<String, Object> generate(GeneratePlanReq req) {
        Roles.require(Role.DOCTOR, Role.ADMIN);
        BedApplication a = appRepo.findById(req.applicationId())
                .orElseThrow(() -> new ApiException("申请不存在"));
        if (a.getStatus() != ApplicationStatus.ACTIVE) {
            throw new ApiException("仅在建病床可生成巡诊计划，当前状态：" + a.getStatus());
        }
        if (req.endDate().isBefore(req.startDate())) throw new ApiException("结束日期不能早于开始日期");

        String services = req.services() == null || req.services().isEmpty()
                ? a.getApprovedServices() : String.join(",", req.services());
        int created = 0;
        List<Long> planIds = new ArrayList<>();
        for (LocalDate d = req.startDate(); !d.isAfter(req.endDate()); d = d.plusDays(1)) {
            int dow = d.getDayOfWeek().getValue(); // 1..7
            if (req.weekdays() != null && !req.weekdays().isEmpty() && !req.weekdays().contains(dow)) continue;
            final LocalDate day = d;
            // 同日不重复排
            boolean exists = planRepo.findByApplicationIdOrderByPlanDateAsc(a.getId()).stream()
                    .anyMatch(p -> p.getPlanDate().equals(day));
            if (exists) continue;
            VisitPlan p = new VisitPlan();
            p.setApplicationId(a.getId());
            p.setPatientId(a.getPatientId());
            p.setPlanDate(d);
            p.setServices(services);
            p.setStatus(PlanStatus.ACTIVE);
            p.setDistanceKm(a.getDistanceKm() == null ? 0 : a.getDistanceKm());
            p.setDurationMin(estimateDuration(services, p.getDistanceKm()));
            p = planRepo.save(p);
            planIds.add(p.getId());
            created++;
        }
        audit.log(a.getId(), "SCHEDULE",
                "生成巡诊计划 " + created + " 次（" + req.startDate() + " ~ " + req.endDate() + "）");

        // 对涉及的每一天做护士分配与路线
        Set<LocalDate> dates = new TreeSet<>();
        for (Long id : planIds) dates.add(planRepo.findById(id).orElseThrow().getPlanDate());
        List<String> dayNotes = new ArrayList<>();
        for (LocalDate d : dates) dayNotes.add(assignDate(d));
        return Map.of("created", created, "planIds", planIds, "assignments", dayNotes);
    }

    /**
     * 某日自动排班：
     * 1) 高风险、超半径患者优先锁定；2) 贪心选择当前负荷最低且可按时返回的护士；
     * 3) 护士内按 风险优先 + 最近邻 排路线与时间；4) 记录冲突与解释。
     */
    @Transactional
    public String assignDate(LocalDate date) {
        List<VisitPlan> dayPlans = new ArrayList<>(
                planRepo.findByPlanDateAndStatusOrderByNurseIdAscRouteOrderAsc(date, PlanStatus.ACTIVE));
        List<AppUser> nurses = userRepo.findByRoleAndEnabledTrue(Role.NURSE);
        if (nurses.isEmpty()) {
            for (VisitPlan p : dayPlans) {
                p.setScheduleNote("无可用护士：基层人力不足，需协调外部支援或改期");
                planRepo.save(p);
            }
            return date + "：无可用护士";
        }

        // 负荷（分钟）与已分配点
        Map<Long, Integer> load = new HashMap<>();
        Map<Long, List<VisitPlan>> buckets = new HashMap<>();
        for (AppUser n : nurses) { load.put(n.getId(), 0); buckets.put(n.getId(), new ArrayList<>()); }

        // 优先：风险高 + 距离远
        dayPlans.sort((x, y) -> {
            BedApplication ax = appRepo.findById(x.getApplicationId()).orElse(null);
            BedApplication ay = appRepo.findById(y.getApplicationId()).orElse(null);
            int rx = riskRank(ax), ry = riskRank(ay);
            if (rx != ry) return Integer.compare(ry, rx);
            return Double.compare(nz(y.getDistanceKm()), nz(x.getDistanceKm()));
        });

        StringBuilder dayWarn = new StringBuilder();
        for (VisitPlan p : dayPlans) {
            BedApplication a = appRepo.findById(p.getApplicationId()).orElse(null);
            int need = nz(p.getDurationMin());
            AppUser best = null;
            int bestLoad = Integer.MAX_VALUE;
            for (AppUser n : nurses) {
                int l = load.get(n.getId());
                if (l + need <= nz(n.getDailyCapacityMin()) && l < bestLoad) {
                    best = n; bestLoad = l;
                }
            }
            List<String> why = new ArrayList<>();
            if (best == null) {
                // 资源不足：仍分给负荷最低者，并明确提示超负荷
                best = nurses.stream().min(Comparator.comparingInt(n -> load.get(n.getId())))
                        .orElse(nurses.get(0));
                why.add("⚠ 护士人力不足：当日全部护士已达负荷，本单分配后将超时，建议医生远程复核或与家属协商改期");
                dayWarn.append("超负荷;");
            }
            int before = load.get(best.getId());
            load.put(best.getId(), before + need);
            buckets.get(best.getId()).add(p);
            p.setNurseId(best.getId());

            double dist = nz(p.getDistanceKm());
            why.add("分配给护士 " + best.getName() + "：其当时已排 " + before
                    + " 分钟，为在岗护士中负荷最低");
            if (dist > SERVICE_RADIUS_KM) {
                why.add("距卫生院 " + dist + "km，超出标准上门半径 " + SERVICE_RADIUS_KM
                        + "km，已把路程时间计入并优先排在前段");
            }
            if (a != null && a.getRiskLevel() == RiskLevel.HIGH || a != null && a.getRiskLevel() == RiskLevel.CRITICAL) {
                why.add("风险等级 " + (a == null ? null : a.getRiskLevel()) + "，优先锁定排班");
            }
            if (a != null && a.getApprovedFrequency() != null) {
                why.add("频次按医嘱 " + a.getApprovedFrequency() + " 安排");
            }
            p.setScheduleNote(String.join("；", why));
            planRepo.save(p);
        }

        // 每位护士内部排路线（高风险先访，其余最近邻）与时间
        for (Map.Entry<Long, List<VisitPlan>> e : buckets.entrySet()) {
            List<VisitPlan> stops = orderRoute(e.getValue());
            int cursor = SHIFT_START.toSecondOfDay() / 60;
            int order = 1;
            for (VisitPlan p : stops) {
                p.setRouteOrder(order++);
                p.setPlannedTime(LocalTime.of(cursor / 60, cursor % 60));
                cursor += nz(p.getDurationMin());
                planRepo.save(p);
            }
        }
        return date + " 排班完成，护士 " + nurses.size() + " 人，任务 " + dayPlans.size()
                + " 单" + (dayWarn.length() > 0 ? "，存在超负荷提示" : "");
    }

    /** 高风险首访，之后从卫生院出发做最近邻。 */
    private List<VisitPlan> orderRoute(List<VisitPlan> plans) {
        List<VisitPlan> result = new ArrayList<>();
        List<VisitPlan> rest = new ArrayList<>(plans);
        rest.sort((x, y) -> riskRank(appRepo.findById(x.getApplicationId()).orElse(null))
                != riskRank(appRepo.findById(y.getApplicationId()).orElse(null))
                ? Integer.compare(
                    riskRank(appRepo.findById(y.getApplicationId()).orElse(null)),
                    riskRank(appRepo.findById(x.getApplicationId()).orElse(null)))
                : 0);
        // 第一个取风险最高
        VisitPlan first = rest.stream()
                .max(Comparator.comparingInt(p -> riskRank(appRepo.findById(p.getApplicationId()).orElse(null))))
                .orElse(null);
        if (first != null) {
            result.add(first);
            rest.remove(first);
        }
        double curDist = first == null ? 0 : nz(first.getDistanceKm());
        while (!rest.isEmpty()) {
            final double cur = curDist;
            VisitPlan next = rest.stream()
                    .min(Comparator.comparingDouble(p -> Math.abs(nz(p.getDistanceKm()) - cur)))
                    .orElse(rest.get(0));
            result.add(next);
            rest.remove(next);
            curDist = nz(next.getDistanceKm());
        }
        return result;
    }

    // ============ 查询某日路线看板 ============

    @Transactional(readOnly = true)
    public Map<String, Object> dayBoard(LocalDate date) {
        Roles.requireLogin();
        List<VisitPlan> plans = planRepo
                .findByPlanDateAndStatusOrderByNurseIdAscRouteOrderAsc(date, PlanStatus.ACTIVE);
        Map<Long, List<VisitPlan>> byNurse = new LinkedHashMap<>();
        for (VisitPlan p : plans) byNurse.computeIfAbsent(p.getNurseId(), k -> new ArrayList<>()).add(p);

        List<Map<String, Object>> nurses = new ArrayList<>();
        int totalKm = 0;
        for (Map.Entry<Long, List<VisitPlan>> e : byNurse.entrySet()) {
            AppUser n = e.getKey() == null ? null : userRepo.findById(e.getKey()).orElse(null);
            List<VisitPlan> stops = e.getValue();
            stops.sort(Comparator.comparingInt(p -> p.getRouteOrder() == null ? 999 : p.getRouteOrder()));
            int minutes = stops.stream().mapToInt(p -> nz(p.getDurationMin())).sum();
            int cap = n == null ? 480 : nz(n.getDailyCapacityMin());
            double km = stops.stream().mapToDouble(p -> nz(p.getDistanceKm())).sum();
            totalKm += (int) Math.round(km);
            List<Map<String, Object>> stopViews = new ArrayList<>();
            for (VisitPlan p : stops) stopViews.add(stopView(p));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nurse", n == null ? null : Map.of("id", n.getId(), "name", n.getName(), "phone", n.getPhone() == null ? "" : n.getPhone()));
            m.put("stops", stopViews);
            m.put("totalMin", minutes);
            m.put("capacityMin", cap);
            m.put("loadRatio", Math.round(minutes * 100.0 / cap));
            m.put("roundTripKm", km);
            m.put("overload", minutes > cap);
            nurses.add(m);
        }

        // 挂起/取消的计划也要可见（临时住院、家属不在、缺货）
        List<VisitPlan> suspended = new ArrayList<>();
        for (PlanStatus st : List.of(PlanStatus.SUSPENDED, PlanStatus.FINISHED)) {
            suspended.addAll(planRepo.findByPlanDateBetweenAndStatus(date, date, st));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", date);
        out.put("nurses", nurses);
        out.put("totalRoundTripKm", totalKm);
        out.put("suspended", suspended.stream().map(this::stopView).toList());
        return out;
    }

    private Map<String, Object> stopView(VisitPlan p) {
        BedApplication a = appRepo.findById(p.getApplicationId()).orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("planId", p.getId());
        m.put("patientId", p.getPatientId());
        m.put("applicationId", p.getApplicationId());
        m.put("bedNo", a == null ? null : a.getBedNo());
        m.put("riskLevel", a == null ? null : a.getRiskLevel());
        m.put("caregiverName", a == null ? null : a.getCaregiverName());
        m.put("caregiverPhone", a == null ? null : a.getCaregiverPhone());
        m.put("plannedTime", p.getPlannedTime());
        m.put("routeOrder", p.getRouteOrder());
        m.put("durationMin", p.getDurationMin());
        m.put("distanceKm", p.getDistanceKm());
        m.put("services", p.getServices());
        m.put("status", p.getStatus());
        m.put("scheduleNote", p.getScheduleNote());
        m.put("cancelReason", p.getCancelReason());
        return m;
    }

    // ============ 人工改派 ============

    @Transactional
    public VisitPlan reassign(Long planId, Long nurseId, String note) {
        AppUser u = Roles.require(Role.ADMIN, Role.DOCTOR);
        VisitPlan p = planRepo.findById(planId).orElseThrow(() -> new ApiException("计划不存在"));
        AppUser n = userRepo.findById(nurseId).orElseThrow(() -> new ApiException("护士不存在"));
        if (n.getRole() != Role.NURSE) throw new ApiException("目标人员不是护士");
        p.setNurseId(nurseId);
        p.setScheduleNote("【人工改派】" + (note == null ? "" : note)
                + "；由 " + u.getName() + " 调整给 " + n.getName());
        planRepo.save(p);
        assignDate(p.getPlanDate()); // 重排路线与时间
        audit.log(p.getApplicationId(), "REASSIGN",
                "计划 #" + p.getId() + " 改派给护士 " + n.getName() + "，原因：" + note);
        return p;
    }

    @Transactional
    public VisitPlan cancelPlan(Long planId, PlanCancelReq req) {
        AppUser u = Roles.require(Role.NURSE, Role.DOCTOR, Role.ADMIN);
        VisitPlan p = planRepo.findById(planId).orElseThrow(() -> new ApiException("计划不存在"));
        p.setStatus(PlanStatus.FINISHED);
        p.setCancelReason((req.abortType() == null ? "OTHER" : req.abortType().name())
                + "：" + (req.note() == null ? "" : req.note()));
        p.setCanceledAt(java.time.LocalDateTime.now());
        planRepo.save(p);
        assignDate(p.getPlanDate());
        audit.log(p.getApplicationId(), "PLAN_CANCEL",
                u.getName() + " 取消/终止上门计划 #" + p.getId() + "：" + p.getCancelReason());
        return p;
    }

    // ============ 估算 ============

    private int estimateDuration(String serviceCodes, double distanceKm) {
        int service = 0;
        if (serviceCodes != null && !serviceCodes.isBlank()) {
            Map<String, Integer> dur = new HashMap<>();
            for (CatalogItem c : catalogRepo.findByEnabledTrue()) dur.put(c.getCode(), nz(c.getDurationMin()));
            for (String code : serviceCodes.split(",")) {
                Integer d = dur.get(code.trim());
                if (d != null) service += d;
            }
        }
        if (service == 0) service = 40;
        int travel = Math.max(MIN_TRAVEL_MIN, (int) Math.round(distanceKm * MIN_PER_KM_ONE_WAY * 2));
        return service + travel;
    }

    private int riskRank(BedApplication a) {
        if (a == null || a.getRiskLevel() == null) return 0;
        return switch (a.getRiskLevel()) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private int nz(Integer v) { return v == null ? 0 : v; }
    private double nz(Double v) { return v == null ? 0 : v; }
}
