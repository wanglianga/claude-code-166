package com.townclinic.homebed.service;

import com.townclinic.homebed.common.ApiException;
import com.townclinic.homebed.dto.LoginReq;
import com.townclinic.homebed.entity.AppUser;
import com.townclinic.homebed.entity.AuthToken;
import com.townclinic.homebed.repo.AppUserRepository;
import com.townclinic.homebed.repo.AuthTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final AppUserRepository userRepo;
    private final AuthTokenRepository tokenRepo;

    public AuthService(AppUserRepository userRepo, AuthTokenRepository tokenRepo) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
    }

    @Transactional
    public java.util.Map<String, Object> login(LoginReq req) {
        AppUser u = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new ApiException(401, "用户名或密码错误"));
        if (!u.getPassword().equals(req.password())) {
            throw new ApiException(401, "用户名或密码错误");
        }
        if (!u.isEnabled()) throw new ApiException(403, "账号已停用");
        AuthToken t = new AuthToken(UUID.randomUUID().toString().replace("-", ""), u.getId());
        tokenRepo.save(t);
        return java.util.Map.of("token", t.getToken(), "user", userView(u));
    }

    @Transactional
    public void logout(String token) {
        if (token != null) tokenRepo.deleteByToken(token);
    }

    public java.util.Map<String, Object> userView(AppUser u) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("name", u.getName());
        m.put("role", u.getRole());
        m.put("title", u.getTitle());
        m.put("phone", u.getPhone());
        m.put("dailyCapacityMin", u.getDailyCapacityMin());
        return m;
    }
}
