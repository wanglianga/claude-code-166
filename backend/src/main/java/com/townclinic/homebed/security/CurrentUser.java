package com.townclinic.homebed.security;

import com.townclinic.homebed.entity.AppUser;

/** 当前登录用户的线程级上下文 */
public class CurrentUser {
    private static final ThreadLocal<AppUser> HOLDER = new ThreadLocal<>();

    public static void set(AppUser user) { HOLDER.set(user); }
    public static AppUser get() { return HOLDER.get(); }
    public static Long id() {
        AppUser u = HOLDER.get();
        return u == null ? null : u.getId();
    }
    public static void clear() { HOLDER.remove(); }
}
