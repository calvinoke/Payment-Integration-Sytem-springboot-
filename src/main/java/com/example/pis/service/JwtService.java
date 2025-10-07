package com.example.pis.service;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.pis.entity.Role;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Production-ready JWT service.
 * - Base64 256-bit secret
 * - Configurable expiration, issuer, and audience
 * - Validation and extraction helpers
 * - Supports optional claims (e.g., role as enum)
 */
@Service
public class JwtService {

    private final byte[] secretKeyBytes;
    private final long expirationMs;
    private final String issuer;
    private final String audience;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience) {
        // Decode Base64 256-bit secret for HS256
        this.secretKeyBytes = java.util.Base64.getDecoder().decode(secret);
        this.expirationMs = expirationMs;
        this.issuer = issuer;
        this.audience = audience;
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKeyBytes);
    }

    /** Generate JWT token with username as subject. */
    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMs))
                .setIssuer(issuer)
                .setAudience(audience)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Generate JWT token with username and role claim (enum-safe). */
    public String generateTokenWithClaims(String username, Role role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMs))
                .setIssuer(issuer)
                .setAudience(audience)
                .addClaims(Map.of("role", role.name()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Validate token signature, expiration, issuer, and audience. */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .setAllowedClockSkewSeconds(60)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Extract username (subject) from token. */
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /** Extract role claim and convert back to Role enum. */
    public Role extractRole(String token) {
        String roleString = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);

        return Role.valueOf(roleString);
    }

    /** Extract any custom claim as String. */
    public String extractClaim(String token, String claimKey) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get(claimKey, String.class);
    }
}
