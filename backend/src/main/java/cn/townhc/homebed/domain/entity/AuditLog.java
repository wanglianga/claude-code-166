package cn.townhc.homebed.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 全流程操作审计：申请、评估、排班、执行、签字、医嘱、收费、确认、质疑等
 * 关键动作均写审计，支持按患者/申请单还原完整服务链。
 */
@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String module;

    @Column(nullable = false, length = 64)
    private String action;

    /** 关联业务主键 */
    private Long bizId;

    @Column(length = 32)
    private String bizNo;

    @Column(length = 32)
    private Long patientId;

    @Column(length = 64)
    private String operatorName;

    @Column(length = 32)
    private String operatorRole;

    private Long operatorId;

    @Column(length = 2048)
    private String detail;

    @Column(length = 64)
    private String ip;

    @Column(nullable = false)
    private LocalDateTime occurredAt;
}
