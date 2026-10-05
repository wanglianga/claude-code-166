package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.MaterialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialTransactionRepository extends JpaRepository<MaterialTransaction, Long> {
    List<MaterialTransaction> findByVisitId(Long visitId);
    List<MaterialTransaction> findByMaterialIdOrderByOccurredAtDesc(Long materialId);
}
