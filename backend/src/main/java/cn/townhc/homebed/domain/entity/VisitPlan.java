package cn.townhc.homebed.domain.entity;

import cn.townhc.homebed.domain.enums.DomainEnums.PlanStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "visit_plan")
public class VisitPlan extends BaseEntity {
    @Column(nullable = false, unique = true, length = 32)
    private String planNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private BedApplication application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    /** 负责家庭医生 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private SysUser doctor;

    /** 责任护士 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_id")
    private SysUser nurse;

    private LocalDate startDate;
    private LocalDate expectedEndDate;

    /** 计划巡诊频次（次/周） */
    private Integer timesPerWeek;

    @Column(length = 1024)
    private String planContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlanStatus status = PlanStatus.ACTIVE;

    /** 排班时给出的可解释安排说明（护士路线/能力/负荷冲突权衡） */
    @Column(length = 2048)
    private String scheduleExplanation;
}
