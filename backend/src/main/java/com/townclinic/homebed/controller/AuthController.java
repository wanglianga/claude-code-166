package com.townclinic.homebed.controller;

import com.townclinic.homebed.common.R;
import com.townclinic.homebed.dto.LoginReq;
import com.townclinic.homebed.security.CurrentUser;
import com.townclinic.homebed.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @Operation(summary = "账号密码登录，返回 token")
    @PostMapping("/login")
    public R<?> login(@Valid @RequestBody LoginReq req) {
        return R.ok(auth.login(req));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<?> logout(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) auth.logout(h.substring(7));
        return R.ok();
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public R<?> me() {
        return R.ok(auth.userView(CurrentUser.get()));
    }
}
