package com.townclinic.homebed.entity;

import com.townclinic.homebed.common.DomainEnums.UsageKind;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 一次巡诊中实际发生的服务/耗材/药品领用记录。
 * 这是账单明细的唯一数据来源：护士记了服务与耗材，财务即可据此结算。
 */
@Entity
@Table(name = "visit_item")
public class VisitItem extends BaseEntity {
    @Column(nullable = false)
    private Long visitId;

    @Column(nullable = false)
    private Long itemId;

    /** 冗余目录快照（医保目录变化后历史账单仍可还原） */
    @Column(length = 32)
    private String itemCode;

    @Column(length = 128)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UsageKind kind;

    @Column(nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(length = 16)
    private String unit;

    /** 执行时单价快照 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /** 执行时是否医保覆盖快照 */
    private boolean insuranceCovered;

    /** 执行时报销比例快照 */
    private Integer reimburseRatio = 0;

    private boolean coldChain = false;

    /** 家属是否已逐项确认该收费项（账单生成时默认未确认） */
    private boolean familyAck = false;

    /** 耗材是否已出库扣减库存 */
    private boolean stockDeducted = false;

    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public UsageKind getKind() { return kind; }
    public void setKind(UsageKind kind) { this.kind = kind; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public boolean isInsuranceCovered() { return insuranceCovered; }
    public void setInsuranceCovered(boolean insuranceCovered) { this.insuranceCovered = insuranceCovered; }
    public Integer getReimburseRatio() { return reimburseRatio; }
    public void setReimburseRatio(Integer reimburseRatio) { this.reimburseRatio = reimburseRatio; }
    public boolean isColdChain() { return coldChain; }
    public void setColdChain(boolean coldChain) { this.coldChain = coldChain; }
    public boolean isFamilyAck() { return familyAck; }
    public void setFamilyAck(boolean familyAck) { this.familyAck = familyAck; }
    public boolean isStockDeducted() { return stockDeducted; }
    public void setStockDeducted(boolean stockDeducted) { this.stockDeducted = stockDeducted; }
}
