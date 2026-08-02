package com.prompttraining.security;

import com.prompttraining.common.Constant;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * JWT Token 生成与解析工具
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    @Value("${jwt.access-token-expiration:7200}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800}")
    private long refreshTokenExpiration;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Access Token（V3：默认角色 USER）
     */
    public String generateAccessToken(Long userId, String username) {
        return generateAccessToken(userId, username, Constant.ROLE_USER, accessTokenExpiration);
    }

    /**
     * 生成带角色的 Access Token（V3）
     *
     * @param userId            用户 ID（管理员使用哨兵 ID 0）
     * @param username          用户名
     * @param role              角色：USER / ADMIN
     * @param expirationSeconds 有效期（秒）
     */
    public String generateAccessToken(Long userId, String username, String role, long expirationSeconds) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationSeconds * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成管理员 Token（V3）
     */
    public String generateAdminToken(String username, long expirationSeconds) {
        return generateAccessToken(Constant.ADMIN_USER_ID, username, Constant.ROLE_ADMIN, expirationSeconds);
    }

    /**
     * 生成 Refresh Token
     */
    public String generateRefreshToken(Long userId, Long tokenId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("tokenId", tokenId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从 Token 中解析用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中解析用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("username", String.class);
    }

    /**
     * 从 Refresh Token 中解析 tokenId
     */
    public Long getTokenIdFromRefreshToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("tokenId", Long.class);
    }

    /**
     * 从 Token 中解析角色（V3 新增；旧 Token 无 role 载荷时默认 USER）
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get("role", String.class);
        return role != null ? role : Constant.ROLE_USER;
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析 Token 中的 Claims
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
