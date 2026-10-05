package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.Bill;
import cn.townhc.homebed.domain.enums.DomainEnums.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByPatientIdOrderByPeriodEndDesc(Long patientId);
    List<Bill> findByApplicationId(Long applicationId);
    List<Bill> findByStatusOrderByCreatedAtDesc(BillStatus status);
    List<Bill> findAllByOrderByCreatedAtDesc();
}
