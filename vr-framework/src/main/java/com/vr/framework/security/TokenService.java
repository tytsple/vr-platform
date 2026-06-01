package com.vr.framework.security;

import com.vr.framework.security.context.LoginUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class TokenService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private SecretKey key;

    @PostConstruct
    void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET 未设置或长度不足（至少32字符）");
        }
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getKey() {
        return key;
    }

    public String createToken(LoginUser loginUser, int tokenVersion) {
        return Jwts.builder()
            .claim("user_id", loginUser.getUserId())
            .claim("username", loginUser.getUsername())
            .claim("roles", loginUser.getRoles())
            .claim("tenant_id", loginUser.getTenantId())
            .claim("token_version", tokenVersion)
            .setSubject(loginUser.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getKey())
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
