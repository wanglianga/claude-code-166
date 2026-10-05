package com.townclinic.homebed.entity;

import com.townclinic.homebed.common.DomainEnums.BillStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 统一账单：一个建床周期/一个月一张账单，
 * 把耗材领用、冷链药品、护理收费、医保报销与自费项目、家属确认汇总在一起。
 */
@Entity
@Table(name = "bill")
public class Bill extends BaseEntity {
    @Column(unique = true, nullable = false, length = 32)
    private String billNo;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long familyUserId;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BillStatus status = BillStatus.DRAFT;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** 医保可报销金额 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal insuranceAmount = BigDecimal.ZERO;

    /** 家属自付金额 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal selfPayAmount = BigDecimal.ZERO;

    /** 其中冷链药品费用（管理统计用） */
    @Column(precision = 12, scale = 2)
    private BigDecimal coldChainAmount = BigDecimal.ZERO;

    /** 合规检查命中的问题（JSON 文本/换行文本） */
    @Column(length = 2048)
    private String complianceIssues;

    private LocalDateTime confirmedAt;

    private LocalDateTime settledAt;

    /** 家属整体确认意见 */
    @Column(length = 512)
    private String familyConfirmNote;

    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getFamilyUserId() { return familyUserId; }
    public void setFamilyUserId(Long familyUserId) { this.familyUserId = familyUserId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public BillStatus getStatus() { return status; }
    public void setStatus(BillStatus status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getInsuranceAmount() { return insuranceAmount; }
    public void setInsuranceAmount(BigDecimal insuranceAmount) { this.insuranceAmount = insuranceAmount; }
    public BigDecimal getSelfPayAmount() { return selfPayAmount; }
    public void setSelfPayAmount(BigDecimal selfPayAmount) { this.selfPayAmount = selfPayAmount; }
    public BigDecimal getColdChainAmount() { return coldChainAmount; }
    public void setColdChainAmount(BigDecimal coldChainAmount) { this.coldChainAmount = coldChainAmount; }
    public String getComplianceIssues() { return complianceIssues; }
    public void setComplianceIssues(String complianceIssues) { this.complianceIssues = complianceIssues; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public String getFamilyConfirmNote() { return familyConfirmNote; }
    public void setFamilyConfirmNote(String familyConfirmNote) { this.familyConfirmNote = familyConfirmNote; }
}
