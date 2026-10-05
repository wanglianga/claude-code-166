package cn.townhc.homebed.domain.entity;

import cn.townhc.homebed.domain.enums.DomainEnums.CatalogCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 耗材/药品库存（含冷链药品） */
@Getter
@Setter
@Entity
@Table(name = "material")
public class Material extends BaseEntity {
    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 64)
    private String spec;

    @Column(length = 16)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CatalogCategory category;

    private Boolean coldChain = false;

    /** 当前库存数量 */
    private Integer stockQty = 0;

    /** 安全库存（低于则缺货预警） */
    private Integer safetyStock = 0;

    /** 采购单价 */
    @Column(precision = 10, scale = 2)
    private BigDecimal unitCost;

    /** 关联收费目录（领用时带出收费标准） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id")
    private ChargeCatalog catalog;

    private LocalDate nearestExpiry;

    private Boolean active = true;
}
