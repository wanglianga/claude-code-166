package com.townclinic.homebed.controller;

import com.townclinic.homebed.common.R;
import com.townclinic.homebed.common.DomainEnums.Role;
import com.townclinic.homebed.entity.AppUser;
import com.townclinic.homebed.repo.AppUserRepository;
import com.townclinic.homebed.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "用户（人员）")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository repo;

    public UserController(AppUserRepository repo) { this.repo = repo; }

    @GetMapping
    @Operation(summary = "人员列表（可按角色过滤，不含密码）")
    public R<?> list(@RequestParam(required = false) String role) {
        Roles.requireLogin();
        List<AppUser> users = role == null || role.isBlank()
                ? repo.findAll() : repo.findByRole(Role.valueOf(role));
        return R.ok(users.stream().map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "name", u.getName(),
                "username", u.getUsername(),
                "role", u.getRole(),
                "title", u.getTitle() == null ? "" : u.getTitle(),
                "phone", u.getPhone() == null ? "" : u.getPhone(),
                "dailyCapacityMin", u.getDailyCapacityMin(),
                "enabled", u.isEnabled()
        )).toList());
    }
}
