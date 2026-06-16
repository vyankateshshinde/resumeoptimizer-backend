package com.vyankatesh.resumeoptimizer.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    private static final String SECRET_KEY =
            "my-secret-key-my-secret-key-my-secret-key-123456";

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // =========================
    // GENERATE TOKEN
    // =========================
    public String generateToken(String email) {

        long expirationTime = 1000L * 60 * 60 * 10; // ✅ 10 HOURS

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================
    // EXTRACT EMAIL
    // =========================
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // =========================
    // VALIDATE TOKEN
    // =========================
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    // =========================
    // PARSE TOKEN
    // =========================
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}