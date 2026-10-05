package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.BedApplication;
import com.townclinic.homebed.common.DomainEnums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BedApplicationRepository extends JpaRepository<BedApplication, Long> {
    List<BedApplication> findByFamilyUserIdOrderByCreatedAtDesc(Long familyUserId);
    List<BedApplication> findByStatusOrderByCreatedAtDesc(ApplicationStatus status);
    List<BedApplication> findAllByOrderByCreatedAtDesc();
    long countByStatus(ApplicationStatus status);
}
