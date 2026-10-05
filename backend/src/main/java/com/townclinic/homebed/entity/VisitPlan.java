package com.townclinic.homebed.entity;

import com.townclinic.homebed.common.DomainEnums.PlanStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 巡诊计划（排班）条目：某患者某日由某护士上门 */
@Entity
@Table(name = "visit_plan")
public class VisitPlan extends BaseEntity {
    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private LocalDate planDate;

    /** 建议开始时间（排班计算） */
    private LocalTime plannedTime;

    /** 预计耗时（分钟，含路程） */
    private Integer durationMin;

    private Long nurseId;

    /** 计划执行的护理项目（目录编码逗号分隔） */
    @Column(length = 512)
    private String services;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlanStatus status = PlanStatus.ACTIVE;

    /** 排班/调整的解释说明（距离、负荷、风险、家属期待之间的权衡） */
    @Column(length = 1024)
    private String scheduleNote;

    /** 路线顺序（同一天同一护士，从卫生院出发的顺序） */
    private Integer routeOrder;

    /** 距卫生院公里数（排班时快照） */
    private Double distanceKm;

    private LocalDateTime canceledAt;

    @Column(length = 512)
    private String cancelReason;

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }
    public LocalTime getPlannedTime() { return plannedTime; }
    public void setPlannedTime(LocalTime plannedTime) { this.plannedTime = plannedTime; }
    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }
    public Long getNurseId() { return nurseId; }
    public void setNurseId(Long nurseId) { this.nurseId = nurseId; }
    public String getServices() { return services; }
    public void setServices(String services) { this.services = services; }
    public PlanStatus getStatus() { return status; }
    public void setStatus(PlanStatus status) { this.status = status; }
    public String getScheduleNote() { return scheduleNote; }
    public void setScheduleNote(String scheduleNote) { this.scheduleNote = scheduleNote; }
    public Integer getRouteOrder() { return routeOrder; }
    public void setRouteOrder(Integer routeOrder) { this.routeOrder = routeOrder; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public LocalDateTime getCanceledAt() { return canceledAt; }
    public void setCanceledAt(LocalDateTime canceledAt) { this.canceledAt = canceledAt; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}
