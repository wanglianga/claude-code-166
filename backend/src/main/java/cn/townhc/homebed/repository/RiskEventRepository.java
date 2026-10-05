package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.RiskEvent;
import cn.townhc.homebed.domain.enums.DomainEnums.RiskEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {
    List<RiskEvent> findByPatientIdOrderByReportedAtDesc(Long patientId);
    List<RiskEvent> findByStatusOrderByReportedAtDesc(RiskEventStatus status);
    List<RiskEvent> findAllByOrderByReportedAtDesc();
    long countByStatus(RiskEventStatus status);
}
