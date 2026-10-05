package com.townclinic.homebed.entity;

import com.townclinic.homebed.common.DomainEnums.EventStatus;
import com.townclinic.homebed.common.DomainEnums.RiskEventType;
import jakarta.persistence.*;

/** 风险事件：病情恶化、压疮、上门超时、冷链异常、收费投诉等 */
@Entity
@Table(name = "risk_event")
public class RiskEvent extends BaseEntity {
    @Column(nullable = false)
    private Long applicationId;

    private Long visitId;

    @Column(nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private RiskEventType eventType;

    @Column(nullable = false, length = 1024)
    private String description;

    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventStatus status = EventStatus.OPEN;

    private Long handlerId;

    @Column(length = 1024)
    private String handleNote;

    private java.time.LocalDateTime handledAt;

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public RiskEventType getEventType() { return eventType; }
    public void setEventType(RiskEventType eventType) { this.eventType = eventType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public Long getHandlerId() { return handlerId; }
    public void setHandlerId(Long handlerId) { this.handlerId = handlerId; }
    public String getHandleNote() { return handleNote; }
    public void setHandleNote(String handleNote) { this.handleNote = handleNote; }
    public java.time.LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(java.time.LocalDateTime handledAt) { this.handledAt = handledAt; }
}
