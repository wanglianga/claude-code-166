package cn.townhc.homebed.domain.entity;

import cn.townhc.homebed.domain.enums.DomainEnums.BedApplyStatus;
import cn.townhc.homebed.domain.enums.DomainEnums.RiskLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "bed_application")
public class BedApplication extends BaseEntity {
    /** 申请单号，如 FB20261004001 */
    @Column(nullable = false, unique = true, length = 32)
    private String applyNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    /** 提交申请的家属 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_user_id")
    private SysUser applicantUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BedApplyStatus status = BedApplyStatus.DRAFT;

    /** 申请理由（行动不便/术后/慢病情况） */
    @Column(length = 1024)
    private String applyReason;

    /** 家属期望的护理服务 */
    @Column(length = 1024)
    private String expectedItems;

    @Column(length = 256)
    private String attachmentsJson;

    // ===== 家庭医生评估部分 =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private SysUser doctor;

    private LocalDateTime assessedAt;

    /** 距卫生院直线距离（米） */
    private Integer distanceMeters;

    /** 预计单程路程（分钟） */
    private Integer travelMinutes;

    /** 照护人情况（是否长期在家、照护能力） */
    @Column(length = 512)
    private String caregiverCondition;

    /** 居住条件（是否具备护理条件） */
    @Column(length = 512)
    private String livingCondition;

    /** 医保资格：参保类型/目录适用/报销资格核实结果 */
    private Boolean insuranceEligible;

    @Column(length = 512)
    private String insuranceNote;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private RiskLevel riskLevel;

    @Column(length = 1024)
    private String riskNotes;

    /** 评估结论（医生意见） */
    @Column(length = 1024)
    private String assessmentConclusion;

    @Column(length = 512)
    private String rejectReason;

    private LocalDateTime approvedAt;

    /** 家庭病床编号 */
    @Column(length = 32)
    private String bedNo;
}
