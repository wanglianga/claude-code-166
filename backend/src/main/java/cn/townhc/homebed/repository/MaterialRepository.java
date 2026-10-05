package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByActiveTrueOrderByCodeAsc();
    List<Material> findByStockQtyLessThanEqualOrderByStockQtyAsc(Integer threshold);
}
