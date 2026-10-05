package com.townclinic.homebed.entity;

import com.townclinic.homebed.common.DomainEnums.Role;
import jakarta.persistence.*;

@Entity
@Table(name = "sys_user")
public class AppUser extends BaseEntity {
    @Column(unique = true, nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    @Column(length = 32)
    private String title;

    @Column(length = 32)
    private String phone;

    /** 护士：是否可承担上门；医生：是否参与远程复核 */
    private boolean enabled = true;

    /** 今日计划可承担的上门工时（分钟），用于排班负荷约束 */
    private Integer dailyCapacityMin = 480;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Integer getDailyCapacityMin() { return dailyCapacityMin; }
    public void setDailyCapacityMin(Integer dailyCapacityMin) { this.dailyCapacityMin = dailyCapacityMin; }
}
