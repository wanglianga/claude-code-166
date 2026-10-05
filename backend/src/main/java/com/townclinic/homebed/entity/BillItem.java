package com.townclinic.homebed.entity;

import com.townclinic.homebed.common.DomainEnums.UsageKind;
import jakarta.persistence.*;

import java.math.BigDecimal;

/** 账单明细（由巡诊领用记录 VisitItem 归集生成，含快照） */
@Entity
@Table(name = "bill_item")
public class BillItem extends BaseEntity {
    @Column(nullable = false)
    private Long billId;

    /** 来源巡诊 */
    @Column(nullable = false)
    private Long visitId;

    /** 来源领用记录 */
    @Column(nullable = false)
    private Long visitItemId;

    @Column(length = 32)
    private String itemCode;

    @Column(length = 128)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private UsageKind kind;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(length = 16)
    private String unit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    private boolean insuranceCovered;
    private Integer reimburseRatio = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal insuranceAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal selfPayAmount = BigDecimal.ZERO;

    private boolean coldChain;
    private boolean familyAck;

    /** 合规标记，如 "超频次"、"重复收费"、"目录外"、null 正常 */
    @Column(length = 256)
    private String complianceFlag;

    private java.time.LocalDateTime serviceTime;

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public Long getVisitItemId() { return visitItemId; }
    public void setVisitItemId(Long visitItemId) { this.visitItemId = visitItemId; }
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
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public boolean isInsuranceCovered() { return insuranceCovered; }
    public void setInsuranceCovered(boolean insuranceCovered) { this.insuranceCovered = insuranceCovered; }
    public Integer getReimburseRatio() { return reimburseRatio; }
    public void setReimburseRatio(Integer reimburseRatio) { this.reimburseRatio = reimburseRatio; }
    public BigDecimal getInsuranceAmount() { return insuranceAmount; }
    public void setInsuranceAmount(BigDecimal amount) { this.insuranceAmount = amount; }
    public BigDecimal getSelfPayAmount() { return selfPayAmount; }
    public void setSelfPayAmount(BigDecimal selfPayAmount) { this.selfPayAmount = selfPayAmount; }
    public boolean isColdChain() { return coldChain; }
    public void setColdChain(boolean coldChain) { this.coldChain = coldChain; }
    public boolean isFamilyAck() { return familyAck; }
    public void setFamilyAck(boolean familyAck) { this.familyAck = familyAck; }
    public String getComplianceFlag() { return complianceFlag; }
    public void setComplianceFlag(String complianceFlag) { this.complianceFlag = complianceFlag; }
    public java.time.LocalDateTime getServiceTime() { return serviceTime; }
    public void setServiceTime(java.time.LocalDateTime serviceTime) { this.serviceTime = serviceTime; }
}
