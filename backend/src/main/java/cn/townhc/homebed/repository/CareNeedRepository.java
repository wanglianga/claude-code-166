package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.CareNeed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareNeedRepository extends JpaRepository<CareNeed, Long> {
    List<CareNeed> findByApplicationId(Long applicationId);
    void deleteByApplicationId(Long applicationId);
}
