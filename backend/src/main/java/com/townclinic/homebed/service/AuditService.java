package com.townclinic.homebed.service;

import com.townclinic.homebed.entity.AppUser;
import com.townclinic.homebed.entity.AuditLog;
import com.townclinic.homebed.repo.AuditLogRepository;
import com.townclinic.homebed.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(Long applicationId, Long visitId, Long billId, String action, String detail) {
        AppUser u = CurrentUser.get();
        AuditLog l = new AuditLog(
                applicationId, visitId, billId, action, detail,
                u == null ? null : u.getId(),
                u == null ? "系统" : u.getName(),
                u == null ? "SYSTEM" : u.getRole().name());
        repo.save(l);
    }

    public void log(Long applicationId, String action, String detail) {
        log(applicationId, null, null, action, detail);
    }
}
