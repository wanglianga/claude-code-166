package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.Visit;
import com.townclinic.homebed.common.DomainEnums.ReviewStatus;
import com.townclinic.homebed.common.DomainEnums.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    Optional<Visit> findByPlanId(Long planId);
    List<Visit> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
    List<Visit> findByNurseIdOrderByCreatedAtDesc(Long nurseId);
    List<Visit> findByReviewStatus(ReviewStatus reviewStatus);
    List<Visit> findByStatus(VisitStatus status);
    long countByStatus(VisitStatus status);
}
