package com.townclinic.homebed.entity;

import com.townclinic.homebed.common.DomainEnums.ItemCategory;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 收费/耗材/药品统一目录。
 * 既是医保结算目录（报销比例、医保编码），也是库存台账（冷链药品单独标记）。
 */
@Entity
@Table(name = "catalog_item")
public class CatalogItem extends BaseEntity {
    /** 医保/收费项目编码 */
    @Column(unique = true, nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 64)
    private String spec;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ItemCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(length = 16)
    private String unit;

    /** 是否医保目录内项目 */
    private boolean insuranceCovered = true;

    /** 医保报销比例 0~100（百分比），自费项目为 0 */
    private Integer reimburseRatio = 0;

    /** 需冷链保存的药品 */
    private boolean coldChain = false;

    /** 当前库存数量（耗材/药品） */
    private Integer stockQty = 0;

    /** 安全库存，低于则预警/可能导致耗材缺货 */
    private Integer safetyStock = 0;

    /** 服务项目标准耗时（分钟），用于排班 */
    private Integer durationMin = 30;

    /** 护理项目适用频次限制，如 "1/日"、"2/周"，空为不限；用于重复收费/超频次判定 */
    @Column(length = 32)
    private String frequencyLimit;

    private boolean enabled = true;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public boolean isInsuranceCovered() { return insuranceCovered; }
    public void setInsuranceCovered(boolean insuranceCovered) { this.insuranceCovered = insuranceCovered; }
    public Integer getReimburseRatio() { return reimburseRatio; }
    public void setReimburseRatio(Integer reimburseRatio) { this.reimburseRatio = reimburseRatio; }
    public boolean isColdChain() { return coldChain; }
    public void setColdChain(boolean coldChain) { this.coldChain = coldChain; }
    public Integer getStockQty() { return stockQty; }
    public void setStockQty(Integer stockQty) { this.stockQty = stockQty; }
    public Integer getSafetyStock() { return safetyStock; }
    public void setSafetyStock(Integer safetyStock) { this.safetyStock = safetyStock; }
    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }
    public String getFrequencyLimit() { return frequencyLimit; }
    public void setFrequencyLimit(String frequencyLimit) { this.frequencyLimit = frequencyLimit; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
