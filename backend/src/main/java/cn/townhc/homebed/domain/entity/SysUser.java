package cn.townhc.homebed.domain.entity;

import cn.townhc.homebed.domain.enums.DomainEnums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseEntity {
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, length = 128)
    @JsonIgnore
    private String password;

    @Column(nullable = false, length = 32)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Column(length = 20)
    private String phone;

    /** 职称，如 主治医师 / 主管护师 */
    @Column(length = 32)
    private String title;

    /** 卫技人员编号 */
    @Column(length = 32)
    private String staffNo;

    /** 护士技能标签（逗号分隔）：压疮护理/导尿/换药/冷链注射 */
    @Column(length = 256)
    private String skills;

    private Boolean active = true;
}
