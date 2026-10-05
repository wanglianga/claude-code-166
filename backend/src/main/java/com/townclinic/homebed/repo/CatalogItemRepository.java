package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.CatalogItem;
import com.townclinic.homebed.common.DomainEnums.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {
    List<CatalogItem> findByEnabledTrue();
    List<CatalogItem> findByCategoryAndEnabledTrue(ItemCategory category);
    boolean existsByCode(String code);
}
