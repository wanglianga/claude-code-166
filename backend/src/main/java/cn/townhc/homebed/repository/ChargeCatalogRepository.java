package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.ChargeCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChargeCatalogRepository extends JpaRepository<ChargeCatalog, Long> {
    List<ChargeCatalog> findByActiveTrueOrderByCategoryAscCodeAsc();
    List<ChargeCatalog> findAllByOrderByCategoryAscCodeAsc();
}
