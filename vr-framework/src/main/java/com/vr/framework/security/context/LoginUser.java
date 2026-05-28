package com.vr.framework.security.context;

import lombok.Data;

import java.util.List;

@Data
public class LoginUser {
    private Long userId;
    private String username;
    private List<String> roles;
    private Long tenantId;

    public LoginUser(Long userId, String username, List<String> roles, Long tenantId) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
        this.tenantId = tenantId;
    }
}
