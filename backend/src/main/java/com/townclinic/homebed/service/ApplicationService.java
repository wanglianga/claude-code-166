package com.townclinic.homebed.service;

import com.townclinic.homebed.common.ApiException;
import com.townclinic.homebed.common.DomainEnums.ApplicationStatus;
import com.townclinic.homebed.common.DomainEnums.PlanStatus;
import com.townclinic.homebed.common.DomainEnums.Role;
import com.townclinic.homebed.dto.AssessReq;
import com.townclinic.homebed.dto.PatientReq;
import com.townclinic.homebed.dto.SubmitApplicationReq;
import com.townclinic.homebed.entity.*;
import com.townclinic.homebed.repo.*;
import com.townclinic.homebed.security.CurrentUser;
import com.townclinic.homebed.security.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final BedApplicationRepository appRepo;
    private final PatientRepository patientRepo;
    private final AppUserRepository userRepo;
    private final CatalogItemRepository catalogRepo;
    private final AuditService audit;
    private final AuditLogRepository auditLogRepo;
    private final VisitPlanRepository planRepo;
    private final VisitRepository visitRepo;
    private final VisitItemRepository visitItemRepo;
    private final VisitPhotoRepository photoRepo;
    private final DoctorOrderRepository orderRepo;
    private final BillRepository billRepo;
    private final RiskEventRepository riskRepo;

    public ApplicationService(BedApplicationRepository appRepo, PatientRepository patientRepo,
                              AppUserRepository userRepo, CatalogItemRepository catalogRepo,
                              AuditService audit, AuditLogRepository auditLogRepo,
                              VisitPlanRepository planRepo, VisitRepository visitRepo,
                              VisitItemRepository visitItemRepo, VisitPhotoRepository photoRepo,
                              DoctorOrderRepository orderRepo, BillRepository billRepo,
                              RiskEventRepository riskRepo) {
        this.appRepo = appRepo;
        this.patientRepo = patientRepo;
        this.userRepo = userRepo;
        this.catalogRepo = catalogRepo;
        this.audit = audit;
        this.auditLogRepo = auditLogRepo;
        this.planRepo = planRepo;
        this.visitRepo = visitRepo;
        this.visitItemRepo = visitItemRepo;
        this.photoRepo = photoRepo;
        this.orderRepo = orderRepo;
        this.billRepo = billRepo;
        this.riskRepo = riskRepo;
    }

    // ============ 患者档案 ============

    @Transactional
    public Patient createPatient(PatientReq req) {
        Roles.require(Role.FAMILY, Role.DOCTOR, Role.ADMIN);
        Patient p = new Patient();
        copyPatient(p, req);
        return patientRepo.save(p);
    }

    @Transactional
    public Patient updatePatient(Long id, PatientReq req) {
        Roles.require(Role.FAMILY, Role.DOCTOR, Role.ADMIN);
        Patient p = patientRepo.findById(id).orElseThrow(() -> new ApiException("患者不存在"));
        copyPatient(p, req);
        return patientRepo.save(p);
    }

    private void copyPatient(Patient p, PatientReq req) {
        p.setName(req.name());
        p.setGender(req.gender());
        p.setAge(req.age());
        p.setIdCard(req.idCard());
        p.setPhone(req.phone());
        p.setAddress(req.address());
        p.setLng(req.lng());
        p.setLat(req.lat());
        p.setInsuranceType(req.insuranceType());
        p.setInsuranceNo(req.insuranceNo());
        p.setDiagnosis(req.diagnosis());
        p.setMobility(req.mobility());
    }

    public List<Patient> listPatients() {
        Roles.requireLogin();
        return patientRepo.findAll();
    }

    public Patient getPatient(Long id) {
        return patientRepo.findById(id).orElseThrow(() -> new ApiException("患者不存在"));
    }

    // ============ 家属提交申请 ============

    @Transactional
    public BedApplication submit(SubmitApplicationReq req) {
        AppUser family = Roles.require(Role.FAMILY);
        Patient p = patientRepo.findById(req.patientId())
                .orElseThrow(() -> new ApiException("患者不存在"));
        BedApplication a = new BedApplication();
        a.setCode("FA" + System.currentTimeMillis());
        a.setPatientId(p.getId());
        a.setFamilyUserId(family.getId());
        a.setCaregiverName(req.caregiverName());
        a.setCaregiverRelation(req.caregiverRelation());
        a.setCaregiverPhone(req.caregiverPhone());
        a.setReason(req.reason());
        a.setRequestedServices(joinCodes(req.requestedServices()));
        a.setExpectedFrequency(req.expectedFrequency());
        a.setStatus(ApplicationStatus.SUBMITTED);
        a = appRepo.save(a);
        audit.log(a.getId(), "SUBMIT",
                "家属 " + family.getName() + " 为患者 " + p.getName()
                        + " 提交家庭病床申请；期望频次：" + nz(req.expectedFrequency()));
        return a;
    }

    // ============ 医生评估 ============

    @Transactional
    public BedApplication assess(AssessReq req) {
        AppUser doctor = Roles.require(Role.DOCTOR);
        BedApplication a = appRepo.findById(req.applicationId())
                .orElseThrow(() -> new ApiException("申请不存在"));
        if (a.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new ApiException("仅待评估的申请可评估，当前状态：" + a.getStatus());
        }
        validateServiceCodes(req.approvedServices());
        a.setAssessDoctorId(doctor.getId());
        a.setAssessedAt(LocalDateTime.now());
        a.setDistanceKm(req.distanceKm());
        a.setInsuranceEligible(req.insuranceEligible());
        a.setRiskLevel(req.riskLevel());
        a.setApprovedServices(joinCodes(req.approvedServices()));
        a.setApprovedFrequency(req.approvedFrequency());
        a.setAssessOpinion(req.assessOpinion());

        if (req.eligible()) {
            if (Boolean.FALSE.equals(req.insuranceEligible())) {
                throw new ApiException("医保资格不通过时不得建议建床，请在评估意见中说明或驳回");
            }
            a.setStatus(ApplicationStatus.ASSESSED);
            audit.log(a.getId(), "ASSESS",
                    "医生 " + doctor.getName() + " 评估完成：风险=" + req.riskLevel()
                            + "，距离=" + req.distanceKm() + "km，建议频次=" + nz(req.approvedFrequency())
                            + "，意见=" + nz(req.assessOpinion()));
        } else {
            a.setStatus(ApplicationStatus.REJECTED);
            a.setRejectReason(req.rejectReason());
            audit.log(a.getId(), "REJECT",
                    "医生 " + doctor.getName() + " 评估驳回：" + nz(req.rejectReason()));
        }
        return appRepo.save(a);
    }

    // ============ 审批建床 / 撤床 ============

    @Transactional
    public BedApplication approve(Long applicationId) {
        AppUser approver = Roles.require(Role.ADMIN, Role.DOCTOR);
        BedApplication a = mustGet(applicationId);
        if (a.getStatus() != ApplicationStatus.ASSESSED) {
            throw new ApiException("仅评估通过的申请可审批建床，当前状态：" + a.getStatus());
        }
        a.setStatus(ApplicationStatus.ACTIVE);
        a.setApproverId(approver.getId());
        a.setApprovedAt(LocalDateTime.now());
        a.setBedNo("JB" + String.format("%04d", applicationId));
        a.setBedStartDate(LocalDate.now());
        a = appRepo.save(a);
        audit.log(a.getId(), "APPROVE",
                approver.getName() + " 审批通过并建床，家庭病床号：" + a.getBedNo());
        return a;
    }

    @Transactional
    public BedApplication discharge(Long applicationId, String note) {
        AppUser u = Roles.require(Role.DOCTOR, Role.ADMIN);
        BedApplication a = mustGet(applicationId);
        if (a.getStatus() != ApplicationStatus.ACTIVE && a.getStatus() != ApplicationStatus.PAUSED) {
            throw new ApiException("仅在建/暂停的病床可撤床，当前状态：" + a.getStatus());
        }
        a.setStatus(ApplicationStatus.DISCHARGED);
        a.setBedEndDate(LocalDate.now());
        a = appRepo.save(a);
        for (VisitPlan vp : planRepo.findByApplicationIdOrderByPlanDateAsc(applicationId)) {
            if (vp.getStatus() == PlanStatus.ACTIVE && !vp.getPlanDate().isBefore(LocalDate.now())) {
                vp.setStatus(PlanStatus.FINISHED);
                vp.setCancelReason("撤床结案：" + nz(note));
                planRepo.save(vp);
            }
        }
        audit.log(a.getId(), "DISCHARGE", u.getName() + " 办理撤床结案：" + nz(note));
        return a;
    }

    // ============ 暂停 / 恢复（临时住院、家属不在家等） ============

    @Transactional
    public BedApplication pause(Long applicationId, String reason) {
        AppUser u = Roles.require(Role.DOCTOR, Role.NURSE, Role.ADMIN);
        BedApplication a = mustGet(applicationId);
        if (a.getStatus() != ApplicationStatus.ACTIVE) {
            throw new ApiException("仅在建病床可暂停，当前状态：" + a.getStatus());
        }
        a.setStatus(ApplicationStatus.PAUSED);
        a.setPauseReason(reason);
        a = appRepo.save(a);
        for (VisitPlan vp : planRepo.findByApplicationIdOrderByPlanDateAsc(applicationId)) {
            if (vp.getStatus() == PlanStatus.ACTIVE && !vp.getPlanDate().isBefore(LocalDate.now())) {
                vp.setStatus(PlanStatus.SUSPENDED);
                vp.setCancelReason("病床暂停：" + nz(reason));
                planRepo.save(vp);
            }
        }
        audit.log(a.getId(), "PAUSE", u.getName() + " 暂停病床：" + nz(reason));
        return a;
    }

    @Transactional
    public BedApplication resume(Long applicationId, String note) {
        AppUser u = Roles.require(Role.DOCTOR, Role.ADMIN);
        BedApplication a = mustGet(applicationId);
        if (a.getStatus() != ApplicationStatus.PAUSED) {
            throw new ApiException("仅暂停的病床可恢复，当前状态：" + a.getStatus());
        }
        a.setStatus(ApplicationStatus.ACTIVE);
        a.setPauseReason(null);
        a = appRepo.save(a);
        audit.log(a.getId(), "RESUME", u.getName() + " 恢复巡诊：" + nz(note));
        return a;
    }

    // ============ 查询 ============

    public BedApplication mustGet(Long id) {
        return appRepo.findById(id).orElseThrow(() -> new ApiException("申请不存在"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        Roles.requireLogin();
        BedApplication a = mustGet(id);
        AppUser me = CurrentUser.get();
        if (me.getRole() == Role.FAMILY && !a.getFamilyUserId().equals(me.getId())) {
            throw new ApiException(403, "只能查看本人家庭的申请");
        }
        Patient p = patientRepo.findById(a.getPatientId()).orElse(null);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("application", a);
        m.put("patient", p);
        m.put("assessDoctor", a.getAssessDoctorId() == null ? null : userRepo.findById(a.getAssessDoctorId()).orElse(null));
        m.put("approver", a.getApproverId() == null ? null : userRepo.findById(a.getApproverId()).orElse(null));
        m.put("requestedServiceNames", namesOf(a.getRequestedServices()));
        m.put("approvedServiceNames", namesOf(a.getApprovedServices()));
        m.put("plans", planRepo.findByApplicationIdOrderByPlanDateAsc(id));
        m.put("visits", decorateVisits(visitRepo.findByApplicationIdOrderByCreatedAtDesc(id)));
        m.put("orders", orderRepo.findByApplicationIdOrderByCreatedAtDesc(id));
        m.put("bills", billRepo.findByApplicationIdOrderByPeriodStartDesc(id));
        m.put("riskEvents", riskRepo.findByApplicationIdOrderByCreatedAtDesc(id));
        m.put("timeline", timeline(me, id));
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listMine() {
        AppUser me = Roles.require(Role.FAMILY);
        return appRepo.findByFamilyUserIdOrderByCreatedAtDesc(me.getId()).stream()
                .map(this::listView).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAll(String status) {
        Roles.require(Role.DOCTOR, Role.ADMIN, Role.NURSE, Role.INSURANCE);
        List<BedApplication> list = status == null || status.isBlank()
                ? appRepo.findAllByOrderByCreatedAtDesc()
                : appRepo.findByStatusOrderByCreatedAtDesc(ApplicationStatus.valueOf(status));
        return list.stream().map(this::listView).toList();
    }

    private Map<String, Object> listView(BedApplication a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("code", a.getCode());
        m.put("status", a.getStatus());
        m.put("patientId", a.getPatientId());
        Patient p = patientRepo.findById(a.getPatientId()).orElse(null);
        m.put("patientName", p == null ? null : p.getName());
        m.put("address", p == null ? null : p.getAddress());
        m.put("distanceKm", a.getDistanceKm());
        m.put("diagnosis", p == null ? null : p.getDiagnosis());
        m.put("caregiverName", a.getCaregiverName());
        m.put("expectedFrequency", a.getExpectedFrequency());
        m.put("approvedFrequency", a.getApprovedFrequency());
        m.put("riskLevel", a.getRiskLevel());
        m.put("bedNo", a.getBedNo());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }

    /**
     * 争议还原：把申请、评估、排班、执行、照片、医嘱、签字、账单、事件串成一条时间线。
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> timeline(AppUser me, Long applicationId) {
        List<Map<String, Object>> line = new ArrayList<>();
        for (AuditLog l : auditLogRepo.findByApplicationIdOrderByCreatedAtAsc(applicationId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "AUDIT");
            m.put("time", l.getCreatedAt());
            m.put("title", l.getAction());
            m.put("detail", nz(l.getDetail()));
            m.put("operator", nz(l.getOperatorName()));
            line.add(m);
        }
        for (Visit v : visitRepo.findByApplicationIdOrderByCreatedAtDesc(applicationId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "VISIT");
            m.put("time", v.getCompletedAt() != null ? v.getCompletedAt() : v.getCreatedAt());
            m.put("title", "巡诊记录 #" + v.getId() + "（" + v.getStatus() + "）");
            m.put("signed", v.getFamilySignature() != null);
            m.put("signName", v.getFamilySignName());
            m.put("reviewStatus", v.getReviewStatus());
            m.put("abnormal", v.getAbnormalNote());
            m.put("photos", photoRepo.findByVisitId(v.getId()));
            m.put("items", visitItemRepo.findByVisitId(v.getId()));
            m.put("orders", orderRepo.findByVisitId(v.getId()));
            line.add(m);
        }
        line.sort((x, y) -> ((Comparable<Object>) y.get("time")).compareTo(x.get("time")));
        return line;
    }

    // ============ 工具 ============

    private List<Map<String, Object>> decorateVisits(List<Visit> visits) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Visit v : visits) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("visit", v);
            m.put("items", visitItemRepo.findByVisitId(v.getId()));
            m.put("photos", photoRepo.findByVisitId(v.getId()));
            m.put("orders", orderRepo.findByVisitId(v.getId()));
            out.add(m);
        }
        return out;
    }

    private void validateServiceCodes(List<String> codes) {
        if (codes == null) return;
        Set<String> enabled = catalogRepo.findByEnabledTrue().stream()
                .map(CatalogItem::getCode).collect(Collectors.toSet());
        for (String c : codes) {
            if (!enabled.contains(c)) {
                throw new ApiException("护理项目编码不在收费目录中：" + c);
            }
        }
    }

    private List<String> namesOf(String codes) {
        if (codes == null || codes.isBlank()) return List.of();
        Map<String, String> map = catalogRepo.findByEnabledTrue().stream()
                .collect(Collectors.toMap(CatalogItem::getCode, CatalogItem::getName, (a, b) -> a));
        return Arrays.stream(codes.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(c -> map.getOrDefault(c, c)).toList();
    }

    static String joinCodes(List<String> codes) {
        return codes == null ? null : String.join(",", codes);
    }

    static String nz(String s) { return s == null ? "" : s; }
}
