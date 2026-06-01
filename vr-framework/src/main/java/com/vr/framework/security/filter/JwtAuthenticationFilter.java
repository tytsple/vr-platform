package com.vr.framework.security.filter;

import com.vr.framework.security.TenantContext;
import com.vr.framework.security.TokenService;
import com.vr.framework.security.context.LoginUser;
import com.vr.system.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;
    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    @SuppressWarnings("unchecked")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = tokenService.parseToken(header.substring(7));
                List<String> roles = (List<String>) claims.get("roles");
                Long tenantId = claims.get("tenant_id") != null ?
                    ((Number) claims.get("tenant_id")).longValue() : null;
                Long userId = ((Number) claims.get("user_id")).longValue();
                // 校验 token_version: 新登录使旧 token 失效
                Integer dbVersion = sysUserMapper.selectTokenVersion(userId);
                Number tokenVersion = (Number) claims.get("token_version");
                if (dbVersion != null && tokenVersion != null && dbVersion.intValue() != tokenVersion.intValue()) {
                    response.setContentType("application/json");
                    response.setStatus(401);
                    response.getWriter().write("{\"error\":\"已在其他设备登录\"}");
                    return;
                }
                LoginUser user = new LoginUser(userId, (String) claims.get("username"), roles, tenantId);
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (roles != null) {
                    for (String role : roles) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }
                }
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                TenantContext.set(userId, roles, tenantId);
            } catch (Exception e) {
                response.setContentType("application/json");
                response.setStatus(401);
                response.getWriter().write("{\"error\":\"token invalid or expired\"}");
                return;
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/health") || path.equals("/ws") || path.equals("/api/auth/login");
    }
}
