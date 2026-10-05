package com.townclinic.homebed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** 登录令牌（简化方案：表头 Authorization: Bearer <token>） */
@Entity
@Table(name = "auth_token")
public class AuthToken extends BaseEntity {
    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(nullable = false)
    private Long userId;

    public AuthToken() {}

    public AuthToken(String token, Long userId) {
        this.token = token;
        this.userId = userId;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
