package com.townclinic.homebed.common;

/** 领域枚举集中定义。 */
public final class DomainEnums {
    private DomainEnums() {}

    /** 平台五类角色：家属 / 护士 / 家庭医生 / 卫生院管理者 / 医保经办 */
    public enum Role { FAMILY, NURSE, DOCTOR, ADMIN, INSURANCE }

    /** 家庭病床申请状态 */
    public enum ApplicationStatus {
        SUBMITTED,    // 家属已提交，待医生评估
        ASSESSED,     // 医生已完成评估，待审批
        APPROVED,     // 审批通过（建床前）
        ACTIVE,       // 已建床，巡诊执行中
        PAUSED,       // 暂停（临时住院/家属不在/病情变化）
        REJECTED,     // 评估不通过
        DISCHARGED    // 撤床结案
    }

    public enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

    public enum PlanStatus { ACTIVE, SUSPENDED, FINISHED }

    public enum VisitStatus { PLANNED, IN_PROGRESS, COMPLETED, CANCELED }

    /** 收费目录大类：护理服务 / 普通耗材 / 普通药品 / 冷链药品 */
    public enum ItemCategory { SERVICE, CONSUMABLE, DRUG, DRUG_COLD }

    public enum UsageKind { SERVICE, CONSUMABLE, DRUG }

    public enum BillStatus { DRAFT, CONFIRMED, QUESTIONED, SETTLED }

    public enum QuestionStatus { OPEN, REPLIED, RESOLVED, REJECTED }

    public enum OrderType { PRESCRIPTION_ADJUST, REVISIT, REFERRAL, CARE_PLAN }

    /** 风险事件类型：病情恶化 / 压疮 / 上门超时 / 冷链异常 / 收费投诉 / 其他 */
    public enum RiskEventType { DETERIORATION, PRESSURE_ULCER, OVERTIME, COLD_CHAIN, COMPLAINT, OTHER }

    public enum EventStatus { OPEN, HANDLED, CLOSED }

    public enum ReviewStatus { NONE, PENDING, REVIEWED, ESCALATED }

    /** 巡诊无法正常完成的原因分类 */
    public enum VisitAbortType { NONE, FAMILY_ABSENT, PATIENT_HOSPITALIZED, STOCK_OUT, PATIENT_REFUSED, OTHER }
}
