package com.townclinic.homebed.entity;

import com.townclinic.homebed.common.DomainEnums.ReviewStatus;
import com.townclinic.homebed.common.DomainEnums.VisitAbortType;
import com.townclinic.homebed.common.DomainEnums.VisitStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 一次上门巡诊执行记录（生命体征 + 护理情况 + 签字 + 异常） */
@Entity
@Table(name = "visit")
public class Visit extends BaseEntity {
    @Column(nullable = false)
    private Long planId;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private Long patientId;

    private Long nurseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VisitStatus status = VisitStatus.PLANNED;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    /** 计划上门时间（用于超时判定） */
    private LocalDateTime scheduledAt;

    // ===== 生命体征 =====
    /** 体温 ℃ */
    private Double temperature;
    /** 收缩压/舒张压 mmHg */
    private Integer systolic;
    private Integer diastolic;
    /** 心率 次/分 */
    private Integer heartRate;
    /** 血糖 mmol/L */
    private Double bloodGlucose;
    /** 血氧 % */
    private Integer spo2;

    // ===== 护理评估 =====
    /** 压疮情况：部位/分期/面积描述 */
    @Column(length = 512)
    private String pressureUlcer;

    /** 换药部位及情况 */
    @Column(length = 512)
    private String dressingChange;

    /** 导尿护理记录 */
    @Column(length = 512)
    private String catheterCare;

    /** 用药提醒/服药情况 */
    @Column(length = 512)
    private String medicationReminder;

    /** 护理过程与病情观察 */
    @Column(length = 2048)
    private String nursingNote;

    /** 护士发现的异常（病情恶化等） */
    @Column(length = 1024)
    private String abnormalNote;

    /** 未正常完成原因 */
    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private VisitAbortType abortType = VisitAbortType.NONE;

    @Column(length = 512)
    private String abortNote;

    // ===== 签字与复核 =====
    /** 家属电子签字（Base64 PNG dataURL） */
    @Column(columnDefinition = "text")
    private String familySignature;

    @Column(length = 64)
    private String familySignName;

    private LocalDateTime signedAt;

    /** 护士签字姓名（来自当前用户） */
    @Column(length = 64)
    private String nurseSignName;

    /** 医生远程复核状态 */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ReviewStatus reviewStatus = ReviewStatus.NONE;

    private Long reviewDoctorId;
    private LocalDateTime reviewedAt;

    /** 复核意见 */
    @Column(length = 1024)
    private String reviewNote;

    /** 实际耗时分钟（完成-到达），超时判定依据 */
    private Integer actualDurationMin;

    /** 超时分钟数（超出计划时段） */
    private Integer overtimeMin;

    /** 冷链药品交接温度记录（℃），无则空 */
    private Double coldChainTemp;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getNurseId() { return nurseId; }
    public void setNurseId(Long nurseId) { this.nurseId = nurseId; }
    public VisitStatus getStatus() { return status; }
    public void setStatus(VisitStatus status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getSystolic() { return systolic; }
    public void setSystolic(Integer systolic) { this.systolic = systolic; }
    public Integer getDiastolic() { return diastolic; }
    public void setDiastolic(Integer diastolic) { this.diastolic = diastolic; }
    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }
    public Double getBloodGlucose() { return bloodGlucose; }
    public void setBloodGlucose(Double bloodGlucose) { this.bloodGlucose = bloodGlucose; }
    public Integer getSpo2() { return spo2; }
    public void setSpo2(Integer spo2) { this.spo2 = spo2; }
    public String getPressureUlcer() { return pressureUlcer; }
    public void setPressureUlcer(String pressureUlcer) { this.pressureUlcer = pressureUlcer; }
    public String getDressingChange() { return dressingChange; }
    public void setDressingChange(String dressingChange) { this.dressingChange = dressingChange; }
    public String getCatheterCare() { return catheterCare; }
    public void setCatheterCare(String catheterCare) { this.catheterCare = catheterCare; }
    public String getMedicationReminder() { return medicationReminder; }
    public void setMedicationReminder(String medicationReminder) { this.medicationReminder = medicationReminder; }
    public String getNursingNote() { return nursingNote; }
    public void setNursingNote(String nursingNote) { this.nursingNote = nursingNote; }
    public String getAbnormalNote() { return abnormalNote; }
    public void setAbnormalNote(String abnormalNote) { this.abnormalNote = abnormalNote; }
    public VisitAbortType getAbortType() { return abortType; }
    public void setAbortType(VisitAbortType abortType) { this.abortType = abortType; }
    public String getAbortNote() { return abortNote; }
    public void setAbortNote(String abortNote) { this.abortNote = abortNote; }
    public String getFamilySignature() { return familySignature; }
    public void setFamilySignature(String familySignature) { this.familySignature = familySignature; }
    public String getFamilySignName() { return familySignName; }
    public void setFamilySignName(String familySignName) { this.familySignName = familySignName; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }
    public String getNurseSignName() { return nurseSignName; }
    public void setNurseSignName(String nurseSignName) { this.nurseSignName = nurseSignName; }
    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(ReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
    public Long getReviewDoctorId() { return reviewDoctorId; }
    public void setReviewDoctorId(Long reviewDoctorId) { this.reviewDoctorId = reviewDoctorId; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public Integer getActualDurationMin() { return actualDurationMin; }
    public void setActualDurationMin(Integer actualDurationMin) { this.actualDurationMin = actualDurationMin; }
    public Integer getOvertimeMin() { return overtimeMin; }
    public void setOvertimeMin(Integer overtimeMin) { this.overtimeMin = overtimeMin; }
    public Double getColdChainTemp() { return coldChainTemp; }
    public void setColdChainTemp(Double coldChainTemp) { this.coldChainTemp = coldChainTemp; }
}
