package cn.townhc.homebed.controller;

import cn.townhc.homebed.common.ApiResponse;
import cn.townhc.homebed.domain.entity.SysUser;
import cn.townhc.homebed.repository.SysUserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "工作人员")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserRepository userRepository;

    @GetMapping
    public ApiResponse<List<SysUser>> list(@RequestParam(required = false) String role) {
        if (role != null && !role.isBlank()) {
            return ApiResponse.ok(userRepository.findByRoleAndActiveTrue(
                    cn.townhc.homebed.domain.enums.DomainEnums.Role.valueOf(role)));
        }
        return ApiResponse.ok(userRepository.findByActiveTrue());
    }
}
