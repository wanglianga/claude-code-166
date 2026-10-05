package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.VisitItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitItemRepository extends JpaRepository<VisitItem, Long> {
    List<VisitItem> findByVisitId(Long visitId);
    List<VisitItem> findByVisitIdIn(List<Long> visitIds);
}
