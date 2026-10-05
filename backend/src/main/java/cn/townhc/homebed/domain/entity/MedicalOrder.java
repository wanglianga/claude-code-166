package cn.townhc.homebed.domain.entity;

import cn.townhc.homebed.domain.enums.DomainEnums.OrderStatus;
import cn.townhc.homebed.domain.enums.DomainEnums.OrderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** 医生根据巡诊记录下达的医嘱：调处方/复诊/转诊/护理方案调整 */
@Getter
@Setter
@Entity
@Table(name = "medical_order")
public class MedicalOrder extends BaseEntity {
    @Column(nullable = false, unique = true, length = 32)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    /** 依据的巡诊记录（可空，医生也可主动下达） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private VisitPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private SysUser doctor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderType orderType;

    /** 医嘱内容：药品用法用量 / 复诊时间机构 / 转诊机构建议 */
    @Column(nullable = false, length = 2048)
    private String content;

    @Column(length = 512)
    private String diagnosisBasis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status = OrderStatus.PROPOSED;

    /** 远程复核：上级医生（家庭医生以外）远程复核意见 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private SysUser reviewer;

    @Column(length = 1024)
    private String reviewComment;

    private LocalDateTime reviewedAt;

    /** 执行/作废说明 */
    @Column(length = 1024)
    private String executionNote;
    private LocalDateTime executedAt;

    /** 是否需要冷链 */
    private Boolean coldChainRequired = false;

    /** 冷链要求说明（2-8℃等） */
    @Column(length = 256)
    private String coldChainNote;
}
