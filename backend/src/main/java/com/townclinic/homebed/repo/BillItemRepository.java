package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillItemRepository extends JpaRepository<BillItem, Long> {
    List<BillItem> findByBillId(Long billId);
    boolean existsByVisitItemId(Long visitItemId);
    void deleteByBillId(Long billId);
}
