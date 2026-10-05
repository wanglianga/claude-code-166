package cn.townhc.homebed.domain.entity;

import cn.townhc.homebed.domain.enums.DomainEnums.CareResultStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/** 单次巡诊中逐项目执行的护理服务（与收费目录一一对应，防漏记/重复记） */
@Getter
@Setter
@Entity
@Table(name = "visit_care_result")
public class VisitCareResult extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id")
    private ChargeCatalog catalog;

    @Column(nullable = false, length = 64)
    private String itemName;

    @Column(nullable = false, length = 32)
    private String itemCode;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    private Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CareResultStatus resultStatus = CareResultStatus.DONE;

    @Column(length = 512)
    private String note;
}
