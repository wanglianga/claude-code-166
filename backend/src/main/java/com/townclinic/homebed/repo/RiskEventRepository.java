package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.RiskEvent;
import com.townclinic.homebed.common.DomainEnums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {
    List<RiskEvent> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
    List<RiskEvent> findByStatusOrderByCreatedAtDesc(EventStatus status);
    List<RiskEvent> findAllByOrderByCreatedAtDesc();
}
