package cn.townhc.homebed.domain.entity;

import cn.townhc.homebed.domain.enums.DomainEnums.CareItemType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 建床评估时确认的护理项目与频次 */
@Getter
@Setter
@Entity
@Table(name = "care_need")
public class CareNeed extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private BedApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CareItemType itemType;

    /** 建议频次（次/周） */
    private Integer timesPerWeek;

    @Column(length = 256)
    private String note;
}
