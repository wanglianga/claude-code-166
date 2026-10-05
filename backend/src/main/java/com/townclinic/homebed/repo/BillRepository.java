package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.Bill;
import com.townclinic.homebed.common.DomainEnums.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByApplicationIdOrderByPeriodStartDesc(Long applicationId);
    List<Bill> findByFamilyUserIdOrderByCreatedAtDesc(Long familyUserId);
    Optional<Bill> findByApplicationIdAndStatus(Long applicationId, BillStatus status);
    List<Bill> findByStatus(BillStatus status);
}
