package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);
    List<AuditLog> findByVisitIdOrderByCreatedAtAsc(Long visitId);
}
