package cn.townhc.homebed.service;

import cn.townhc.homebed.common.BusinessException;
import cn.townhc.homebed.domain.entity.SysUser;
import cn.townhc.homebed.repository.SysUserRepository;
import cn.townhc.homebed.security.JwtUtil;
import cn.townhc.homebed.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    public Map<String, Object> login(String username, String password) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException(403, "账号已停用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getName(), user.getRole());
        auditService.log(new LoginUser(user.getId(), user.getUsername(), user.getName(), user.getRole()),
                "认证", "登录", user.getId(), user.getUsername(), null, "用户登录系统");

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("name", user.getName());
        info.put("role", user.getRole().name());
        info.put("roleLabel", user.getRole().label);
        info.put("phone", user.getPhone());
        info.put("title", user.getTitle());
        info.put("skills", user.getSkills());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", info);
        return result;
    }
}
