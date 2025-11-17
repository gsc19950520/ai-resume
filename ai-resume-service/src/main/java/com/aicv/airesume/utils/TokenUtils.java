package com.aicv.airesume.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Token工具类
 */
@Component
public class TokenUtils {

    @Value("${jwt.secret:ai_resume_optimizer_secret_key_256bit_length_for_hs512_algorithm}")
    private String secret;

    @Value("${jwt.expire:7200000}")
    private long expire;

    /**
     * 生成Token
     */
    public String generateToken(Long userId) {
        Date nowDate = new Date();
        Date expireDate = new Date(nowDate.getTime() + expire);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(nowDate)
                .setExpiration(expireDate)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 解析Token
     */
    public Map<String, Object> parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Map<String, Object> claims = parseToken(token);
        if (claims != null) {
            return Long.valueOf(claims.get("userId").toString());
        }
        return null;
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    /**
     * 刷新Token（延长有效期）
     * @param token 原token
     * @return 新的token，如果原token无效则返回null
     */
    public String refreshToken(String token) {
        Map<String, Object> claims = parseToken(token);
        if (claims != null && claims.get("userId") != null) {
            Long userId = Long.valueOf(claims.get("userId").toString());
            return generateToken(userId);
        }
        return null;
    }

    /**
     * 获取Token的剩余有效时间（毫秒）
     * @param token token字符串
     * @return 剩余有效时间，如果token无效返回-1
     */
    public long getRemainingTime(String token) {
        try {
            Map<String, Object> claims = parseToken(token);
            if (claims != null && claims.get("exp") != null) {
                Date expiration = new Date(Long.valueOf(claims.get("exp").toString()) * 1000);
                Date now = new Date();
                return expiration.getTime() - now.getTime();
            }
        } catch (Exception e) {
            // 忽略异常，返回-1
        }
        return -1;
    }
}