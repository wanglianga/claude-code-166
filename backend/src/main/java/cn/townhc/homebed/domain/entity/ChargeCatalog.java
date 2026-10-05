package cn.townhc.homebed.domain.entity;

import cn.townhc.homebed.domain.enums.DomainEnums.CatalogCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 医疗服务收费/医保结算目录。
 * 通过 effectiveFrom/effectiveTo + active 体现医保目录变化：
 * 旧版本目录条目录失效，新版本生效；账单项保存当时快照，事后可还原。
 */
@Getter
@Setter
@Entity
@Table(name = "charge_catalog")
public class ChargeCatalog extends BaseEntity {
    /** 同一编码可存在多个版本（医保目录变更），同一时间仅一个 active */
    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CatalogCategory category;

    @Column(length = 16)
    private String unit;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** 是否医保可报销项目 */
    private Boolean reimbursable = false;

    /** 医保目录类别：甲类/乙类/丙类（自费） */
    @Column(length = 8)
    private String insuranceCategory;

    /** 报销比例（0~1），如居民医保乙类 0.7 */
    @Column(precision = 4, scale = 2)
    private BigDecimal reimbursementRate;

    /** 是否冷链药品 */
    private Boolean coldChain = false;

    /** 巡诊费类项目每日限报次数等限制说明 */
    @Column(length = 256)
    private String limitRule;

    /** 每日次数上限（可空），用于重复收费/超频次合规判断 */
    private Integer dailyLimit;

    private Boolean active = true;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    @Column(length = 256)
    private String versionNote;
}
