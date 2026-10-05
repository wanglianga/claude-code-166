package com.townclinic.homebed.dto;

import com.townclinic.homebed.common.DomainEnums.ItemCategory;

import java.math.BigDecimal;

public record CatalogItemReq(
        String code,
        String name,
        String spec,
        ItemCategory category,
        BigDecimal price,
        String unit,
        boolean insuranceCovered,
        Integer reimburseRatio,
        boolean coldChain,
        Integer stockQty,
        Integer safetyStock,
        Integer durationMin,
        String frequencyLimit
) {}
