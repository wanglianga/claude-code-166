package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.ComplianceRecord;
import cn.townhc.homebed.domain.enums.DomainEnums.ComplianceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplianceRecordRepository extends JpaRepository<ComplianceRecord, Long> {
    Optional<ComplianceRecord> findByBillId(Long billId);
    List<ComplianceRecord> findByStatusOrderByCreatedAtDesc(ComplianceStatus status);
}
