package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.VisitPlan;
import com.townclinic.homebed.common.DomainEnums.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VisitPlanRepository extends JpaRepository<VisitPlan, Long> {
    List<VisitPlan> findByApplicationIdOrderByPlanDateAsc(Long applicationId);
    List<VisitPlan> findByPlanDateAndStatusOrderByNurseIdAscRouteOrderAsc(LocalDate date, PlanStatus status);
    List<VisitPlan> findByNurseIdAndPlanDateAndStatusOrderByRouteOrderAsc(Long nurseId, LocalDate date, PlanStatus status);
    List<VisitPlan> findByPlanDateBetweenAndStatus(LocalDate from, LocalDate to, PlanStatus status);
    long countByPlanDateAndNurseIdAndStatus(LocalDate date, Long nurseId, PlanStatus status);
}
