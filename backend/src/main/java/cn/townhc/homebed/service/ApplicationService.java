package cn.townhc.homebed.service;

import cn.townhc.homebed.common.BusinessException;
import cn.townhc.homebed.domain.entity.BedApplication;
import cn.townhc.homebed.domain.entity.CareNeed;
import cn.townhc.homebed.domain.entity.Patient;
import cn.townhc.homebed.domain.entity.SysUser;
import cn.townhc.homebed.domain.enums.DomainEnums.*;
import cn.townhc.homebed.repository.BedApplicationRepository;
import cn.townhc.homebed.repository.CareNeedRepository;
import cn.townhc.homebed.repository.PatientRepository;
import cn.townhc.homebed.repository.SysUserRepository;
import cn.townhc.homebed.security.LoginUser;
import cn.townhc.homebed.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final BedApplicationRepository applicationRepository;
    private final CareNeedRepository careNeedRepository;
    private final PatientRepository patientRepository;
    private final SysUserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BedApplication> list(String status) {
        LoginUser me = SecurityUtils.current();
        if (me.role() == Role.FAMILY) {
            List<BedApplication> all = applicationRepository.findByApplicantUserIdOrderByCreatedAtDesc(me.id());
            return filterStatus(all, status);
        }
        if (status != null && !status.isBlank()) {
            return applicationRepository.findByStatusOrderByCreatedAtDesc(BedApplyStatus.valueOf(status));
        }
        List<BedApplication> all = applicationRepository.findAll();
        all.sort(Comparator.comparing(BedApplication::getCreatedAt).reversed());
        return all;
    }

    private List<BedApplication> filterStatus(List<BedApplication> list, String status) {
        if (status == null || status.isBlank()) return list;
        BedApplyStatus s = BedApplyStatus.valueOf(status);
        return list.stream().filter(a -> a.getStatus() == s).toList();
    }

    @Transactional(readOnly = true)
    public BedApplication get(Long id) {
        BedApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "申请单不存在"));
        LoginUser me = SecurityUtils.current();
        if (me.role() == Role.FAMILY
                && !app.getApplicantUser().getId().equals(me.id())) {
            throw new BusinessException(403, "只能查看本人提交的申请");
        }
        return app;
    }

    @Transactional(readOnly = true)
    public List<CareNeed> careNeeds(Long applicationId) {
        return careNeedRepository.findByApplicationId(applicationId);
    }

    @Transactional
    public BedApplication submit(Long patientId, String reason, String expectedItems,
                                 List<CareItemType> careItems, String attachmentsJson) {
        LoginUser me = SecurityUtils.current();
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new BusinessException(404, "患者不存在"));
        if (me.role() == Role.FAMILY
                && (patient.getFamilyUser() == null || !patient.getFamilyUser().getId().equals(me.id()))) {
            throw new BusinessException(403, "只能为本人绑定的患者提交申请");
        }
        SysUser applicant = userRepository.findById(me.id()).orElseThrow();

        BedApplication app = new BedApplication();
        app.setApplyNo(generateNo("FB"));
        app.setPatient(patient);
        app.setApplicantUser(applicant);
        app.setStatus(BedApplyStatus.SUBMITTED);
        app.setApplyReason(reason);
        app.setExpectedItems(expectedItems);
        app.setAttachmentsJson(attachmentsJson);
        BedApplication saved = applicationRepository.save(app);

        if (careItems != null) {
            for (CareItemType t : careItems) {
                CareNeed need = new CareNeed();
                need.setApplication(saved);
                need.setItemType(t);
                need.setTimesPerWeek(defaultFrequency(t));
                careNeedRepository.save(need);
            }
        }
        auditService.log(me, "建床申请", "家属提交", saved.getId(), saved.getApplyNo(),
                patient.getId(), "家属为患者[" + patient.getName() + "]提交家庭病床申请");
        return saved;
    }

    private Integer defaultFrequency(CareItemType t) {
        return switch (t) {
            case VITALS, BLOOD_PRESSURE -> 3;
            case BLOOD_GLUCOSE -> 4;
            case PRESSURE_ULCER, DRESSING, CATHETER -> 2;
            case INJECTION -> 1;
            default -> 1;
        };
    }

    /**
     * 家庭医生评估：病情、距离、照护人、医保资格、护理项目、风险等级。
     * 距离按经纬度与卫生院近似坐标计算，基层资源约束在评估结论中给出提示。
     */
    @Transactional
    public BedApplication assess(Long applicationId, AssessmentCommand cmd) {
        LoginUser me = SecurityUtils.current();
        BedApplication app = get(applicationId);
        if (me.role() != Role.FAMILY_DOCTOR && me.role() != Role.ADMIN) {
            throw new BusinessException(403, "仅家庭医生可评估");
        }
        if (app.getStatus() != BedApplyStatus.SUBMITTED) {
            throw new BusinessException("当前申请状态[" + app.getStatus().label + "]不可评估");
        }
        SysUser doctor = userRepository.findById(me.id()).orElseThrow();
        Patient patient = app.getPatient();

        // 距离由坐标自动测算（未给坐标时使用医生录入值）
        Integer distance = cmd.distanceMeters();
        if (distance == null && patient.getLatitude() != null && patient.getLongitude() != null) {
            distance = (int) Math.round(distanceMeters(
                    TOWN_HC_LAT, TOWN_HC_LNG, patient.getLatitude(), patient.getLongitude()));
        }
        app.setDistanceMeters(distance);
        app.setTravelMinutes(distance == null ? null : Math.max(5, Math.round(distance / 500f)));
        app.setDoctor(doctor);
        app.setAssessedAt(LocalDateTime.now());
        app.setCaregiverCondition(cmd.caregiverCondition());
        app.setLivingCondition(cmd.livingCondition());
        app.setInsuranceEligible(cmd.insuranceEligible());
        app.setInsuranceNote(cmd.insuranceNote());
        app.setRiskLevel(cmd.riskLevel());
        app.setRiskNotes(cmd.riskNotes());
        app.setAssessmentConclusion(cmd.conclusion());

        // 重建护理需求清单
        careNeedRepository.deleteByApplicationId(app.getId());
        if (cmd.careItems() != null) {
            for (CareItemCommand c : cmd.careItems()) {
                CareNeed need = new CareNeed();
                need.setApplication(app);
                need.setItemType(c.itemType());
                need.setTimesPerWeek(c.timesPerWeek() == null ? 1 : c.timesPerWeek());
                need.setNote(c.note());
                careNeedRepository.save(need);
            }
        }

        if (Boolean.FALSE.equals(cmd.approved())) {
            app.setStatus(BedApplyStatus.REJECTED);
            app.setRejectReason(cmd.rejectReason());
            applicationRepository.save(app);
            auditService.log(me, "建床申请", "评估驳回", app.getId(), app.getApplyNo(),
                    patient.getId(), "驳回原因：" + cmd.rejectReason());
            return app;
        }

        app.setStatus(BedApplyStatus.APPROVED);
        app.setApprovedAt(LocalDateTime.now());
        app.setBedNo("JC-" + String.format("%04d", app.getId()));
        applicationRepository.save(app);
        auditService.log(me, "建床申请", "评估批准建床", app.getId(), app.getApplyNo(),
                patient.getId(), "风险等级=" + cmd.riskLevel().label
                        + "，距离=" + app.getDistanceMeters() + "米，医保资格="
                        + (Boolean.TRUE.equals(cmd.insuranceEligible()) ? "符合" : "不符合")
                        + "；结论：" + cmd.conclusion());
        return app;
    }

    @Transactional
    public BedApplication cancel(Long applicationId) {
        LoginUser me = SecurityUtils.current();
        BedApplication app = get(applicationId);
        if (me.role() == Role.FAMILY && !app.getApplicantUser().getId().equals(me.id())) {
            throw new BusinessException(403, "只能撤销本人申请");
        }
        if (app.getStatus() == BedApplyStatus.APPROVED) {
            throw new BusinessException("已批准建床，不可撤销，请联系卫生院办理撤床");
        }
        app.setStatus(BedApplyStatus.CANCELLED);
        applicationRepository.save(app);
        auditService.log(me, "建床申请", "撤销申请", app.getId(), app.getApplyNo(),
                app.getPatient().getId(), "家属撤销申请");
        return app;
    }

    public record AssessmentCommand(
            Boolean approved,
            String rejectReason,
            Integer distanceMeters,
            String caregiverCondition,
            String livingCondition,
            Boolean insuranceEligible,
            String insuranceNote,
            RiskLevel riskLevel,
            String riskNotes,
            String conclusion,
            List<CareItemCommand> careItems
    ) {}

    public record CareItemCommand(CareItemType itemType, Integer timesPerWeek, String note) {}

    // ===== 距离与编号工具 =====

    /** 乡镇卫生院坐标（演示数据以华东某镇为基准） */
    public static final double TOWN_HC_LAT = 30.6586;
    public static final double TOWN_HC_LNG = 117.4952;

    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static final java.util.concurrent.atomic.AtomicLong NO_SEQ =
            new java.util.concurrent.atomic.AtomicLong(0);

    public static String generateNo(String prefix) {
        long seq = NO_SEQ.incrementAndGet() % 1000;
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", seq)
                + (int) ((Math.random() * 90) + 10);
    }
}
