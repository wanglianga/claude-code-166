package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.Visit;
import cn.townhc.homebed.domain.enums.DomainEnums.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByPlanIdOrderByScheduledAtAsc(Long planId);
    List<Visit> findByPatientIdOrderByScheduledAtDesc(Long patientId);
    List<Visit> findByNurseIdAndStatus(Long nurseId, VisitStatus status);
    List<Visit> findByStatusOrderByScheduledAtAsc(VisitStatus status);
    List<Visit> findByNurseIdAndScheduledAtBetween(Long nurseId, LocalDateTime from, LocalDateTime to);
    long countByStatus(VisitStatus status);
}
