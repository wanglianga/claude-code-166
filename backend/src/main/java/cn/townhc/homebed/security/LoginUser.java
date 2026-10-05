package cn.townhc.homebed.security;

import cn.townhc.homebed.domain.enums.DomainEnums.Role;

/**
 * 解析 JWT 后放入 SecurityContext 的登录主体。
 */
public record LoginUser(Long id, String username, String name, Role role) {
}
