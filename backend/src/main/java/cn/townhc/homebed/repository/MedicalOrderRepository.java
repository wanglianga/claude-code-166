package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.MedicalOrder;
import cn.townhc.homebed.domain.enums.DomainEnums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, Long> {
    List<MedicalOrder> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<MedicalOrder> findByVisitId(Long visitId);
    List<MedicalOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    List<MedicalOrder> findByPlanIdOrderByCreatedAtDesc(Long planId);
}
