package com.townclinic.homebed.entity;

import jakarta.persistence.*;

/**
 * 全过程留痕审计日志。
 * 任意争议（临时住院/家属不在/缺货/超时/病情恶化/目录变化/收费质疑）
 * 都可按申请串联出申请-评估-计划-执行-照片-医嘱-签字-账单时间线。
 */
@Entity
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {
    @Column(nullable = false)
    private Long applicationId;

    private Long visitId;
    private Long billId;

    /** 动作类型：SUBMIT/ASSESS/APPROVE/REJECT/SCHEDULE/VISIT_START/SIGN/REVIEW/ORDER/BILL/... */
    @Column(nullable = false, length = 48)
    private String action;

    @Column(length = 1024)
    private String detail;

    private Long operatorId;

    @Column(length = 64)
    private String operatorName;

    @Column(length = 16)
    private String operatorRole;

    public AuditLog() {}

    public AuditLog(Long applicationId, Long visitId, Long billId, String action,
                    String detail, Long operatorId, String operatorName, String operatorRole) {
        this.applicationId = applicationId;
        this.visitId = visitId;
        this.billId = billId;
        this.action = action;
        this.detail = detail;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
        this.operatorRole = operatorRole;
    }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getOperatorRole() { return operatorRole; }
    public void setOperatorRole(String operatorRole) { this.operatorRole = operatorRole; }
}
