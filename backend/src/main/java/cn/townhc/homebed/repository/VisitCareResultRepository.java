package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.VisitCareResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitCareResultRepository extends JpaRepository<VisitCareResult, Long> {
    List<VisitCareResult> findByVisitId(Long visitId);
}
