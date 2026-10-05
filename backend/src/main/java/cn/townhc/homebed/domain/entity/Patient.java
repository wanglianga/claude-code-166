package cn.townhc.homebed.domain.entity;

import cn.townhc.homebed.domain.enums.DomainEnums.DiseaseCategory;
import cn.townhc.homebed.domain.enums.DomainEnums.InsuranceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "patient")
public class Patient extends BaseEntity {
    @Column(nullable = false, length = 32)
    private String name;

    /** 男/女 */
    @Column(length = 4)
    private String gender;

    private Integer age;

    @Column(length = 20)
    private String phone;

    @Column(length = 18)
    private String idCard;

    @Column(length = 256)
    private String address;

    /** 经纬度（GCJ-02 近似坐标），用于上门路线与半径计算 */
    private Double longitude;
    private Double latitude;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private InsuranceType insuranceType;

    @Column(length = 32)
    private String insuranceNo;

    /** 行动能力：卧床 / 轮椅 / 可扶行 / 自理 */
    @Column(length = 16)
    private String mobilityLevel;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private DiseaseCategory diseaseCategory;

    /** 主要照护人姓名 */
    @Column(length = 32)
    private String primaryCaregiver;

    @Column(length = 20)
    private String primaryContactPhone;

    @Column(length = 32)
    private String emergencyContact;

    @Column(length = 20)
    private String emergencyPhone;

    /** 绑定的家属账号（一个家属账号可绑定多名老人） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_user_id")
    private SysUser familyUser;
}
