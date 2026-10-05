package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.DoctorOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorOrderRepository extends JpaRepository<DoctorOrder, Long> {
    List<DoctorOrder> findByVisitId(Long visitId);
    List<DoctorOrder> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
