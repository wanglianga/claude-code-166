package cn.townhc.homebed.domain.entity;

import cn.townhc.homebed.domain.enums.DomainEnums.VisitStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 单次上门巡诊任务。一次巡诊的服务、耗材、异常、医嘱、签字都挂在该任务下，
 * 是账单与审计还原的核心节点。
 */
@Getter
@Setter
@Entity
@Table(name = "visit")
public class Visit extends BaseEntity {
    @Column(nullable = false, unique = true, length = 32)
    private String visitNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private VisitPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_id")
    private SysUser nurse;

    /** 计划上门时间 */
    private LocalDateTime scheduledAt;

    /** 实际开始（护士签到） */
    private LocalDateTime startedAt;

    /** 实际结束（提交记录） */
    private LocalDateTime completedAt;

    /** 预估单程分钟 */
    private Integer estimatedTravelMinutes;

    /** 实际路程分钟（含路途） */
    private Integer actualTravelMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VisitStatus status = VisitStatus.SCHEDULED;

    /** 排班/改派说明，解释该次任务为什么这样安排 */
    @Column(length = 1024)
    private String arrangementNote;

    // ===== 巡诊记录：生命体征 =====
    private Integer systolicPressure;
    private Integer diastolicPressure;
    private Integer heartRate;
    private Double temperature;
    private Integer bloodGlucose;
    private Integer oxygenSaturation;
    private Integer breathRate;

    /** 压疮情况：部位/分期/面积，结构化文本 */
    @Column(length = 512)
    private String pressureUlcerNote;

    /** 换药部位与情况 */
    @Column(length = 512)
    private String dressingNote;

    /** 导尿护理记录 */
    @Column(length = 512)
    private String catheterNote;

    /** 用药提醒/核对记录 */
    @Column(length = 512)
    private String medicationNote;

    /** 护士主观情况描述 */
    @Column(length = 1024)
    private String overallNote;

    // ===== 家属签字 =====
    @Column(length = 32)
    private String familySignName;

    /** 家属签字图片相对路径 */
    @Column(length = 256)
    private String familySignPath;

    private LocalDateTime familySignedAt;

    @Column(length = 256)
    private String nurseLocationSnapshot;
}
