package com.townclinic.homebed.service;

import com.townclinic.homebed.common.ApiException;
import com.townclinic.homebed.common.DomainEnums.EventStatus;
import com.townclinic.homebed.common.DomainEnums.Role;
import com.townclinic.homebed.dto.HandleRiskReq;
import com.townclinic.homebed.dto.RiskEventReq;
import com.townclinic.homebed.entity.AppUser;
import com.townclinic.homebed.entity.RiskEvent;
import com.townclinic.homebed.repo.BedApplicationRepository;
import com.townclinic.homebed.repo.RiskEventRepository;
import com.townclinic.homebed.security.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RiskEventService {

    private final RiskEventRepository repo;
    private final BedApplicationRepository appRepo;
    private final AuditService audit;

    public RiskEventService(RiskEventRepository repo, BedApplicationRepository appRepo, AuditService audit) {
        this.repo = repo;
        this.appRepo = appRepo;
        this.audit = audit;
    }

    @Transactional
    public RiskEvent report(RiskEventReq req) {
        AppUser u = Roles.require(Role.NURSE, Role.DOCTOR, Role.ADMIN, Role.FAMILY);
        var a = appRepo.findById(req.applicationId()).orElseThrow(() -> new ApiException("申请不存在"));
        if (u.getRole() == Role.FAMILY && !a.getFamilyUserId().equals(u.getId())) {
            throw new ApiException(403, "只能上报本家庭病床的风险");
        }
        RiskEvent e = new RiskEvent();
        e.setApplicationId(req.applicationId());
        e.setVisitId(req.visitId());
        e.setPatientId(a.getPatientId());
        e.setEventType(req.eventType());
        e.setDescription(req.description());
        e.setReporterId(u.getId());
        e.setStatus(EventStatus.OPEN);
        e = repo.save(e);
        audit.log(req.applicationId(), req.visitId(), null, "RISK_REPORT",
                u.getName() + " 上报风险事件[" + req.eventType() + "]：" + req.description());
        return e;
    }

    @Transactional
    public RiskEvent handle(HandleRiskReq req) {
        AppUser u = Roles.require(Role.ADMIN, Role.DOCTOR);
        RiskEvent e = repo.findById(req.eventId()).orElseThrow(() -> new ApiException("风险事件不存在"));
        e.setHandlerId(u.getId());
        e.setHandleNote(req.handleNote());
        e.setHandledAt(LocalDateTime.now());
        e.setStatus(req.close() ? EventStatus.CLOSED : EventStatus.HANDLED);
        repo.save(e);
        audit.log(e.getApplicationId(), e.getVisitId(), null, "RISK_HANDLE",
                u.getName() + " 处置风险事件 #" + e.getId() + "：" + req.handleNote()
                        + (req.close() ? "（已关闭）" : "（跟进中）"));
        return e;
    }

    @Transactional(readOnly = true)
    public List<RiskEvent> list(String status) {
        Roles.requireLogin();
        if (status == null || status.isBlank()) return repo.findAllByOrderByCreatedAtDesc();
        return repo.findByStatusOrderByCreatedAtDesc(EventStatus.valueOf(status));
    }
}
