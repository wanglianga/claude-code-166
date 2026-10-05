package com.townclinic.homebed.entity;

import com.townclinic.homebed.common.DomainEnums.ApplicationStatus;
import com.townclinic.homebed.common.DomainEnums.RiskLevel;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 家庭病床建床申请与医生评估结论 */
@Entity
@Table(name = "bed_application")
public class BedApplication extends BaseEntity {
    /** 申请编号（可读） */
    @Column(length = 32)
    private String code;

    @Column(nullable = false)
    private Long patientId;

    /** 提交申请的家属用户 */
    @Column(nullable = false)
    private Long familyUserId;

    /** 主要照护人姓名及关系 */
    @Column(length = 64)
    private String caregiverName;

    @Column(length = 32)
    private String caregiverRelation;

    @Column(length = 32)
    private String caregiverPhone;

    @Column(length = 1024)
    private String reason;

    /** 申请的护理项目，逗号分隔的目录编码 */
    @Column(length = 512)
    private String requestedServices;

    /** 期望上门频次，如 "每周3次" */
    @Column(length = 64)
    private String expectedFrequency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    // ===== 医生评估部分 =====
    private Long assessDoctorId;
    private LocalDateTime assessedAt;

    /** 居住地到卫生院距离（公里，演示可手动填） */
    private Double distanceKm;

    /** 医保资格核验结果 */
    private Boolean insuranceEligible;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private RiskLevel riskLevel;

    /** 评估确认的护理项目（目录编码逗号分隔） */
    @Column(length = 512)
    private String approvedServices;

    /** 建议上门频次 */
    @Column(length = 64)
    private String approvedFrequency;

    /** 评估意见与说明 */
    @Column(length = 2048)
    private String assessOpinion;

    /** 驳回原因 */
    @Column(length = 512)
    private String rejectReason;

    /** 审批人（管理者） */
    private Long approverId;
    private LocalDateTime approvedAt;

    /** 建床号（审批通过后生成） */
    @Column(length = 32)
    private String bedNo;

    private LocalDate bedStartDate;
    private LocalDate bedEndDate;

    /** 最近一次暂停原因（临时住院/家属不在家等） */
    @Column(length = 512)
    private String pauseReason;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getFamilyUserId() { return familyUserId; }
    public void setFamilyUserId(Long familyUserId) { this.familyUserId = familyUserId; }
    public String getCaregiverName() { return caregiverName; }
    public void setCaregiverName(String caregiverName) { this.caregiverName = caregiverName; }
    public String getCaregiverRelation() { return caregiverRelation; }
    public void setCaregiverRelation(String caregiverRelation) { this.caregiverRelation = caregiverRelation; }
    public String getCaregiverPhone() { return caregiverPhone; }
    public void setCaregiverPhone(String caregiverPhone) { this.caregiverPhone = caregiverPhone; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRequestedServices() { return requestedServices; }
    public void setRequestedServices(String requestedServices) { this.requestedServices = requestedServices; }
    public String getExpectedFrequency() { return expectedFrequency; }
    public void setExpectedFrequency(String expectedFrequency) { this.expectedFrequency = expectedFrequency; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public Long getAssessDoctorId() { return assessDoctorId; }
    public void setAssessDoctorId(Long assessDoctorId) { this.assessDoctorId = assessDoctorId; }
    public LocalDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDateTime assessedAt) { this.assessedAt = assessedAt; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public Boolean getInsuranceEligible() { return insuranceEligible; }
    public void setInsuranceEligible(Boolean insuranceEligible) { this.insuranceEligible = insuranceEligible; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public String getApprovedServices() { return approvedServices; }
    public void setApprovedServices(String approvedServices) { this.approvedServices = approvedServices; }
    public String getApprovedFrequency() { return approvedFrequency; }
    public void setApprovedFrequency(String approvedFrequency) { this.approvedFrequency = approvedFrequency; }
    public String getAssessOpinion() { return assessOpinion; }
    public void setAssessOpinion(String assessOpinion) { this.assessOpinion = assessOpinion; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getBedNo() { return bedNo; }
    public void setBedNo(String bedNo) { this.bedNo = bedNo; }
    public LocalDate getBedStartDate() { return bedStartDate; }
    public void setBedStartDate(LocalDate bedStartDate) { this.bedStartDate = bedStartDate; }
    public LocalDate getBedEndDate() { return bedEndDate; }
    public void setBedEndDate(LocalDate bedEndDate) { this.bedEndDate = bedEndDate; }
    public String getPauseReason() { return pauseReason; }
    public void setPauseReason(String pauseReason) { this.pauseReason = pauseReason; }
}
