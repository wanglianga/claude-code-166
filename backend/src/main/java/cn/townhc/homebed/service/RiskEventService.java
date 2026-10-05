package cn.townhc.homebed.service;

import cn.townhc.homebed.common.BusinessException;
import cn.townhc.homebed.domain.entity.Patient;
import cn.townhc.homebed.domain.entity.RiskEvent;
import cn.townhc.homebed.domain.entity.SysUser;
import cn.townhc.homebed.domain.entity.Visit;
import cn.townhc.homebed.domain.enums.DomainEnums.RiskEventStatus;
import cn.townhc.homebed.domain.enums.DomainEnums.RiskSeverity;
import cn.townhc.homebed.repository.PatientRepository;
import cn.townhc.homebed.repository.RiskEventRepository;
import cn.townhc.homebed.repository.SysUserRepository;
import cn.townhc.homebed.repository.VisitRepository;
import cn.townhc.homebed.security.LoginUser;
import cn.townhc.homebed.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskEventService {

    private final RiskEventRepository riskRepository;
    private final SysUserRepository userRepository;
    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<RiskEvent> list(String status, Long patientId) {
        if (patientId != null) return riskRepository.findByPatientIdOrderByReportedAtDesc(patientId);
        if (status != null && !status.isBlank()) {
            return riskRepository.findByStatusOrderByReportedAtDesc(RiskEventStatus.valueOf(status));
        }
        return riskRepository.findAllByOrderByReportedAtDesc();
    }

    @Transactional
    public RiskEvent confirm(Long id, String handlePlan) {
        LoginUser me = SecurityUtils.current();
        RiskEvent e = riskRepository.findById(id).orElseThrow(() -> new BusinessException(404, "风险事件不存在"));
        e.setStatus(RiskEventStatus.CONFIRMED);
        e.setConfirmer(userRepository.findById(me.id()).orElse(null));
        e.setHandlePlan(handlePlan);
        e = riskRepository.save(e);
        auditService.log(me, "风险事件", "确认并制定处置", e.getId(), e.getRiskNo(),
                e.getPatient().getId(), handlePlan);
        return e;
    }

    @Transactional
    public RiskEvent close(Long id, String closeNote) {
        LoginUser me = SecurityUtils.current();
        RiskEvent e = riskRepository.findById(id).orElseThrow(() -> new BusinessException(404, "风险事件不存在"));
        e.setStatus(RiskEventStatus.CLOSED);
        e.setCloseNote(closeNote);
        e.setClosedAt(LocalDateTime.now());
        e = riskRepository.save(e);
        auditService.log(me, "风险事件", "闭环", e.getId(), e.getRiskNo(),
                e.getPatient().getId(), closeNote);
        return e;
    }

    @Transactional
    public RiskEvent manualReport(Long patientId, Long visitId, String riskType,
                                  RiskSeverity severity,
                                  String description, String evidencePath) {
        LoginUser me = SecurityUtils.current();
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new BusinessException(404, "患者不存在"));
        RiskEvent e = new RiskEvent();
        e.setRiskNo(ApplicationService.generateNo("FX"));
        e.setPatient(patient);
        if (visitId != null) {
            Visit v = visitRepository.findById(visitId).orElse(null);
            e.setVisit(v);
        }
        e.setRiskType(riskType);
        e.setSeverity(severity == null ? RiskSeverity.MEDIUM : severity);
        e.setDescription(description);
        e.setEvidencePath(evidencePath);
        e.setStatus(RiskEventStatus.OPEN);
        e.setReporter(userRepository.findById(me.id()).orElse(null));
        e.setReportedAt(LocalDateTime.now());
        e = riskRepository.save(e);
        auditService.log(me, "风险事件", "手工上报", e.getId(), e.getRiskNo(),
                patient.getId(), riskType + "：" + description);
        return e;
    }
}
