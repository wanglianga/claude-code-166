package cn.townhc.homebed.domain.entity;

import cn.townhc.homebed.domain.enums.DomainEnums.MaterialTxType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 耗材出入库流水：上门领用、入库、盘点 */
@Getter
@Setter
@Entity
@Table(name = "material_transaction")
public class MaterialTransaction extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id")
    private Material material;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MaterialTxType type;

    /** 数量（入库为正，出库为正数量+type=OUT） */
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private SysUser operator;

    @Column(length = 32)
    private String batchNo;

    private LocalDate expiryDate;

    /** 冷链药品交接温度（℃） */
    private Double coldChainTemp;

    @Column(length = 512)
    private String note;

    private LocalDateTime occurredAt;
}
