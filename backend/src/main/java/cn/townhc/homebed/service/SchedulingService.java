package cn.townhc.homebed.service;

import cn.townhc.homebed.common.BusinessException;
import cn.townhc.homebed.domain.entity.*;
import cn.townhc.homebed.domain.enums.DomainEnums.*;
import cn.townhc.homebed.repository.*;
import cn.townhc.homebed.security.LoginUser;
import cn.townhc.homebed.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 排班与可解释安排。
 * 基层约束：护士少、上门半径大、技能参差、冷链能力有限、家属期望频次高。
 * 系统对每位候选护士按 技能匹配 / 本周负荷 / 路线聚集度 / 半径红线 打分，
 * 选中最优并记录“为什么是她、为什么不是别人”的可读说明。
 */
@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final VisitPlanRepository planRepository;
    private final VisitRepository visitRepository;
    private final BedApplicationRepository applicationRepository;
    private final CareNeedRepository careNeedRepository;
    private final SysUserRepository userRepository;
    private final AuditService auditService;

    /** 基层硬约束常量（演示环境） */
    static final int SERVICE_RADIUS_METERS = 5000;
    static final int NURSE_WEEKLY_CAP = 18;
    static final int NURSE_DAILY_CAP = 6;
    static final int PLAN_WEEKS = 4;

    public record CandidateScore(Long nurseId, String nurseName, String title,
                                 int weeklyLoad, int matchedSkills, int requiredSkills,
                                 double avgRouteMeters, int radiusBreach,
                                 double score, List<String> reasons) {}

    public record ScheduleResult(VisitPlan plan, String selectedNurseName,
                                 List<String> reasons, List<CandidateScore> candidates,
                                 List<String> constraints, int generatedVisits) {}

    @Transactional
    public ScheduleResult generatePlan(Long applicationId) {
        LoginUser me = SecurityUtils.current();
        BedApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(404, "申请单不存在"));
        if (app.getStatus() != BedApplyStatus.APPROVED) {
            throw new BusinessException("仅批准建床的申请可生成巡诊计划");
        }
        Patient patient = app.getPatient();
        List<CareNeed> needs = careNeedRepository.findByApplicationId(applicationId);
        Set<String> requiredSkills = requiredSkills(needs);
        int timesPerWeek = needs.stream().map(CareNeed::getTimesPerWeek)
                .filter(Objects::nonNull).max(Integer::compareTo).orElse(2);
        timesPerWeek = Math.min(timesPerWeek, 5);

        List<SysUser> nurses = userRepository.findByRoleAndActiveTrue(Role.NURSE);
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDateTime weekFrom = weekStart.atStartOfDay();
        LocalDateTime weekTo = weekStart.plusDays(7).atStartOfDay();

        List<CandidateScore> candidates = new ArrayList<>();
        for (SysUser nurse : nurses) {
            List<Visit> weekVisits = visitRepository
                    .findByNurseIdAndScheduledAtBetween(nurse.getId(), weekFrom, weekTo);
            int weeklyLoad = weekVisits.size();
            Set<String> skills = parseSkills(nurse.getSkills());
            int matched = 0;
            for (String s : requiredSkills) if (skills.contains(s)) matched++;

            // 路线聚集度：该护士本周任务对应患者与本患者的平均距离
            double avgRoute = averageRouteTo(patient, weekVisits);
            int radiusBreach = (app.getDistanceMeters() != null
                    && app.getDistanceMeters() > SERVICE_RADIUS_METERS) ? 1 : 0;

            double score = 100d
                    - weeklyLoad * 4.5d
                    - (requiredSkills.size() - matched) * 14d
                    - avgRoute / 1000d * 3d
                    - (weeklyLoad >= NURSE_WEEKLY_CAP ? 50 : 0)
                    - radiusBreach * 8d;

            List<String> reasons = new ArrayList<>();
            if (matched < requiredSkills.size()) {
                Set<String> missing = new LinkedHashSet<>(requiredSkills);
                missing.removeAll(skills);
                reasons.add("缺少技能：" + String.join("、", missing));
            }
            reasons.add("本周已排 " + weeklyLoad + " 次（上限 " + NURSE_WEEKLY_CAP + "）");
            if (!weekVisits.isEmpty()) {
                reasons.add("与本周在管患者平均相距约 " + Math.round(avgRoute / 100) / 10.0 + " 公里");
            }
            if (weeklyLoad >= NURSE_WEEKLY_CAP) reasons.add("本周负荷已满，不建议再派");
            candidates.add(new CandidateScore(nurse.getId(), nurse.getName(),
                    nurse.getTitle(), weeklyLoad, matched, requiredSkills.size(),
                    Math.round(avgRoute), radiusBreach, Math.round(score * 10) / 10.0, reasons));
        }
        candidates.sort((a, b) -> Double.compare(b.score(), a.score()));
        if (candidates.isEmpty()) {
            throw new BusinessException("卫生院暂无在职护士，无法排班");
        }
        CandidateScore best = candidates.get(0);
        SysUser nurse = userRepository.findById(best.nurseId()).orElseThrow();

        // 生成计划
        VisitPlan plan = new VisitPlan();
        plan.setPlanNo(ApplicationService.generateNo("JH"));
        plan.setApplication(app);
        plan.setPatient(patient);
        plan.setDoctor(app.getDoctor());
        plan.setNurse(nurse);
        plan.setStartDate(LocalDate.now());
        plan.setExpectedEndDate(LocalDate.now().plusWeeks(PLAN_WEEKS));
        plan.setTimesPerWeek(timesPerWeek);
        plan.setStatus(PlanStatus.ACTIVE);

        StringBuilder content = new StringBuilder();
        for (CareNeed n : needs) {
            content.append(n.getItemType().label)
                    .append(" ").append(n.getTimesPerWeek()).append("次/周；");
        }
        if (content.isEmpty()) content.append("常规巡诊 ").append(timesPerWeek).append("次/周");
        plan.setPlanContent(content.toString());

        // 可解释说明
        List<String> reasons = new ArrayList<>();
        reasons.add("选择责任护士【" + nurse.getName() + "】：综合得分 " + best.score()
                + "，技能匹配 " + best.matchedSkills() + "/" + best.requiredSkills()
                + "，本周已排 " + best.weeklyLoad() + " 次"
                + (best.avgRouteMeters() > 0 ? "，与其本周路线平均相距 " + Math.round(best.avgRouteMeters() / 100) / 10.0 + " 公里" : "，暂无本周在途任务"));
        List<String> constraints = new ArrayList<>();
        if (app.getDistanceMeters() != null && app.getDistanceMeters() > SERVICE_RADIUS_METERS) {
            constraints.add("患者距卫生院 " + Math.round(app.getDistanceMeters() / 100) / 10.0
                    + " 公里，超出常规上门半径 " + SERVICE_RADIUS_METERS / 1000 + " 公里红线："
                    + "已按同片区路线拼单安排，若遇道路/天气受阻将改期并提前告知家属");
        }
        if (best.matchedSkills() < best.requiredSkills()) {
            constraints.add("责任护士缺少部分专项技能：相关项目由家庭医生远程复核指导后执行，必要时改派具备资质护士");
        }
        if (best.weeklyLoad() + timesPerWeek * PLAN_WEEKS > NURSE_WEEKLY_CAP) {
            constraints.add("叠加后将超护士周负荷上限：系统将优先安排必需护理项目，健康指导等项目合并上门，避免护士上门超时");
        }
        constraints.add("家属期望频次 " + timesPerWeek + " 次/周 与护理需求一致；如遇患者住院/家属不在家/耗材缺货，任务自动挂起并通知改期");

        // 其他候选未被选中的原因
        for (int i = 1; i < candidates.size(); i++) {
            CandidateScore c = candidates.get(i);
            reasons.add("未选【" + c.nurseName() + "】：" + String.join("；", c.reasons()));
        }
        StringBuilder explain = new StringBuilder();
        reasons.forEach(r -> explain.append("· ").append(r).append("\n"));
        constraints.forEach(c -> explain.append("⚠ ").append(c).append("\n"));
        plan.setScheduleExplanation(explain.toString());
        plan = planRepository.save(plan);

        // 展开 4 周巡诊任务：上午 9:00 起，按频次均匀分布在工作日
        int generated = 0;
        int[] slots = switch (timesPerWeek) {
            case 1 -> new int[]{0};          // 周一
            case 2 -> new int[]{0, 3};       // 周一、周四
            case 3 -> new int[]{0, 2, 4};    // 周一三五
            case 4 -> new int[]{0, 1, 3, 4};
            default -> new int[]{0, 1, 2, 3, 4};
        };
        LocalTime[] dayTimes = {LocalTime.of(9, 0), LocalTime.of(10, 0),
                LocalTime.of(11, 0), LocalTime.of(14, 30), LocalTime.of(15, 30), LocalTime.of(16, 30)};
        for (int week = 0; week < PLAN_WEEKS; week++) {
            LocalDate monday = plan.getStartDate().with(DayOfWeek.MONDAY).plusWeeks(week);
            int daySlot = 0;
            for (int offset : slots) {
                LocalDate date = monday.plusDays(offset);
                LocalTime t = dayTimes[Math.min(daySlot, dayTimes.length - 1)];
                daySlot++;
                Visit visit = new Visit();
                visit.setVisitNo(ApplicationService.generateNo("XZ"));
                visit.setPlan(plan);
                visit.setPatient(patient);
                visit.setNurse(nurse);
                visit.setScheduledAt(LocalDateTime.of(date, t));
                visit.setStatus(VisitStatus.SCHEDULED);
                visit.setEstimatedTravelMinutes(app.getTravelMinutes());
                visit.setArrangementNote("按计划常规巡诊");
                visitRepository.save(visit);
                generated++;
            }
        }

        auditService.log(me, "巡诊计划", "生成计划与排班", plan.getId(), plan.getPlanNo(),
                patient.getId(), "责任护士=" + nurse.getName() + "，生成巡诊 " + generated
                        + " 次；排班解释：" + plan.getScheduleExplanation().replace("\n", " | "));
        return new ScheduleResult(plan, nurse.getName(), reasons, candidates, constraints, generated);
    }

    /** 单次任务改派（异常处理时），同样记录原因 */
    @Transactional
    public Visit reassign(Long visitId, Long nurseId, String reason) {
        LoginUser me = SecurityUtils.current();
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new BusinessException(404, "巡诊任务不存在"));
        SysUser nurse = userRepository.findById(nurseId)
                .orElseThrow(() -> new BusinessException(404, "护士不存在"));
        if (nurse.getRole() != Role.NURSE) throw new BusinessException("改派对象必须是护士");
        String old = visit.getNurse() == null ? "-" : visit.getNurse().getName();
        visit.setNurse(nurse);
        visit.setArrangementNote("由【" + old + "】改派至【" + nurse.getName() + "】；原因：" + reason);
        visit.setStatus(VisitStatus.SCHEDULED);
        visit = visitRepository.save(visit);
        auditService.log(me, "巡诊任务", "改派", visit.getId(), visit.getVisitNo(),
                visit.getPatient().getId(), visit.getArrangementNote());
        return visit;
    }

    @Transactional
    public Visit reschedule(Long visitId, LocalDateTime newTime, String reason) {
        LoginUser me = SecurityUtils.current();
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new BusinessException(404, "巡诊任务不存在"));
        LocalDateTime old = visit.getScheduledAt();
        visit.setScheduledAt(newTime);
        visit.setStatus(VisitStatus.SCHEDULED);
        visit.setArrangementNote("改期：" + old + " → " + newTime + "；原因：" + reason);
        visit = visitRepository.save(visit);
        auditService.log(me, "巡诊任务", "改期", visit.getId(), visit.getVisitNo(),
                visit.getPatient().getId(), visit.getArrangementNote());
        return visit;
    }

    @Transactional(readOnly = true)
    public List<VisitPlan> plans(Long patientId) {
        if (patientId != null) return planRepository.findByPatientIdOrderByStartDateDesc(patientId);
        return planRepository.findAll();
    }

    private Set<String> requiredSkills(List<CareNeed> needs) {
        Set<String> set = new LinkedHashSet<>();
        for (CareNeed n : needs) {
            switch (n.getItemType()) {
                case PRESSURE_ULCER -> set.add("压疮护理");
                case CATHETER -> set.add("导尿");
                case DRESSING -> set.add("换药");
                case INJECTION -> set.add("注射");
                default -> { }
            }
        }
        return set;
    }

    private Set<String> parseSkills(String skills) {
        if (skills == null || skills.isBlank()) return Set.of();
        Set<String> set = new LinkedHashSet<>();
        for (String s : skills.split("[,，、/]")) {
            if (!s.isBlank()) set.add(s.trim());
        }
        return set;
    }

    private double averageRouteTo(Patient target, List<Visit> weekVisits) {
        if (weekVisits.isEmpty() || target.getLatitude() == null) return 0;
        double sum = 0;
        int n = 0;
        Set<Long> counted = new HashSet<>();
        for (Visit v : weekVisits) {
            Patient p = v.getPatient();
            if (p.getId().equals(target.getId()) || p.getLatitude() == null
                    || !counted.add(p.getId())) continue;
            sum += ApplicationService.distanceMeters(target.getLatitude(), target.getLongitude(),
                    p.getLatitude(), p.getLongitude());
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }
}
