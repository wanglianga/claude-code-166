package com.townclinic.homebed.security;

import com.townclinic.homebed.entity.AppUser;
import com.townclinic.homebed.repo.AppUserRepository;
import com.townclinic.homebed.repo.AuthTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthTokenRepository tokenRepo;
    private final AppUserRepository userRepo;

    public AuthInterceptor(AuthTokenRepository tokenRepo, AppUserRepository userRepo) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            var t = tokenRepo.findByToken(token);
            if (t != null) {
                AppUser u = userRepo.findById(t.getUserId()).orElse(null);
                if (u != null && u.isEnabled()) {
                    CurrentUser.set(u);
                    return true;
                }
            }
        }
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录或登录已失效\"}");
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUser.clear();
    }
}
