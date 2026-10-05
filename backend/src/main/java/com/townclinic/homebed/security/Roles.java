package com.townclinic.homebed.security;

import com.townclinic.homebed.common.ApiException;
import com.townclinic.homebed.common.DomainEnums.Role;
import com.townclinic.homebed.entity.AppUser;

/** 角色断言工具 */
public final class Roles {
    private Roles() {}

    public static AppUser requireLogin() {
        AppUser u = CurrentUser.get();
        if (u == null) throw new ApiException(401, "未登录或登录已失效");
        return u;
    }

    public static AppUser require(Role... roles) {
        AppUser u = requireLogin();
        for (Role r : roles) {
            if (u.getRole() == r) return u;
        }
        throw new ApiException(403, "当前角色无权执行该操作");
    }

    public static boolean has(Role role) {
        AppUser u = CurrentUser.get();
        return u != null && u.getRole() == role;
    }
}
