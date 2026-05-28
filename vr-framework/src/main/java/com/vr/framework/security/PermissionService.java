package com.vr.framework.security;

import com.vr.framework.security.context.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("ss")
public class PermissionService {

    public boolean hasRole(String role) {
        LoginUser user = getLoginUser();
        return user != null && user.getRoles() != null && user.getRoles().contains(role);
    }

    public LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser) {
            return (LoginUser) auth.getPrincipal();
        }
        return null;
    }

    public Long getUserId() {
        LoginUser user = getLoginUser();
        return user != null ? user.getUserId() : null;
    }

    public Long getTenantId() {
        LoginUser user = getLoginUser();
        return user != null ? user.getTenantId() : null;
    }
}
