package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.BedApplication;
import cn.townhc.homebed.domain.enums.DomainEnums.BedApplyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BedApplicationRepository extends JpaRepository<BedApplication, Long> {
    List<BedApplication> findByStatusOrderByCreatedAtDesc(BedApplyStatus status);
    List<BedApplication> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<BedApplication> findByApplicantUserIdOrderByCreatedAtDesc(Long applicantUserId);
    long countByStatus(BedApplyStatus status);
}
