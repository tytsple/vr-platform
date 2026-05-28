package com.vr.framework.security;

import com.vr.framework.security.context.LoginUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class TokenService {

    @Value("${jwt.secret:dev-secret-change-in-production}")
    private String secret;

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(secret.getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(LoginUser loginUser) {
        return Jwts.builder()
            .claim("user_id", loginUser.getUserId())
            .claim("username", loginUser.getUsername())
            .claim("roles", loginUser.getRoles())
            .claim("tenant_id", loginUser.getTenantId())
            .setSubject(loginUser.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 86400000L))
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
