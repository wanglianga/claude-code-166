package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByPatientIdOrderByOccurredAtDesc(Long patientId);
    List<AuditLog> findByBizNoOrderByOccurredAtAsc(String bizNo);
    List<AuditLog> findTop200ByOrderByOccurredAtDesc();
    List<AuditLog> findByModuleOrderByOccurredAtDesc(String module);
}
