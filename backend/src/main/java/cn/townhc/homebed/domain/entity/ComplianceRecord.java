package cn.townhc.homebed.domain.entity;

import cn.townhc.homebed.domain.enums.DomainEnums.ComplianceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 医保合规审查记录。系统对每张账单自动初审，输出命中规则与建议，
 * 医保经办人员复核并给出结论。
 */
@Getter
@Setter
@Entity
@Table(name = "compliance_record")
public class ComplianceRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ComplianceStatus status = ComplianceStatus.PENDING;

    /** 命中的合规问题（JSON 数组文本：重复收费/超频次/项目冲突/无资质等） */
    @Column(length = 4096)
    private String issuesJson;

    /** 自动初审得分（0-100，越高越合规） */
    private Integer autoScore;

    /** 系统可解释的规则命中明细以 JSON 存储，由服务层序列化 */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_id")
    private SysUser auditor;

    @Column(length = 1024)
    private String auditorComment;

    private LocalDateTime auditedAt;

    /** 经办机构 */
    @Column(length = 64)
    private String agencyName;

    /** 复核结论明细（问题是否成立、扣款金额） */
    @Column(length = 2048)
    private String auditConclusion;
}
