package com.townclinic.homebed.entity;

import com.townclinic.homebed.common.DomainEnums.OrderType;
import jakarta.persistence.*;

/** 医生依据巡诊记录下达的医嘱：调处方 / 安排复诊 / 建议转诊 / 调整护理方案 */
@Entity
@Table(name = "doctor_order")
public class DoctorOrder extends BaseEntity {
    @Column(nullable = false)
    private Long visitId;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long doctorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OrderType orderType;

    @Column(nullable = false, length = 1024)
    private String content;

    /** 复诊/转诊日期 */
    private java.time.LocalDate targetDate;

    @Column(length = 128)
    private String targetOrg;

    /** 执行状态：待执行 / 已执行 / 已取消 */
    @Column(length = 16)
    private String execStatus = "PENDING";

    @Column(length = 512)
    private String execNote;

    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public java.time.LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(java.time.LocalDate targetDate) { this.targetDate = targetDate; }
    public String getTargetOrg() { return targetOrg; }
    public void setTargetOrg(String targetOrg) { this.targetOrg = targetOrg; }
    public String getExecStatus() { return execStatus; }
    public void setExecStatus(String execStatus) { this.execStatus = execStatus; }
    public String getExecNote() { return execNote; }
    public void setExecNote(String execNote) { this.execNote = execNote; }
}
