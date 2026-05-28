package com.vr.framework.security;

import java.util.List;

/**
 * Thread-local holder for the current request's tenant and role context.
 * Populated by JwtAuthenticationFilter on every authenticated request.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> tenantIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> rolesHolder = new ThreadLocal<>();
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(Long userId, List<String> roles, Long tenantId) {
        userIdHolder.set(userId);
        rolesHolder.set(roles);
        tenantIdHolder.set(tenantId);
    }

    public static Long getCurrentTenantId() {
        return tenantIdHolder.get();
    }

    public static List<String> getCurrentRoles() {
        return rolesHolder.get();
    }

    public static Long getCurrentUserId() {
        return userIdHolder.get();
    }

    public static void clear() {
        userIdHolder.remove();
        rolesHolder.remove();
        tenantIdHolder.remove();
    }
}
