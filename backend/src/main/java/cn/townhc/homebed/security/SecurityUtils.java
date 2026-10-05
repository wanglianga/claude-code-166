package cn.townhc.homebed.security;

import cn.townhc.homebed.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser u)) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return u;
    }
}
