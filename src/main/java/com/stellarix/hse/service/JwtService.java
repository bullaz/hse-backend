package com.stellarix.hse.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET;

    // --- Public token generation ---

    public Map<String, String> generateToken(String email, String nom, String prenom, UUID jti) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("nom", nom);
        claims.put("prenom", prenom);
        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", createAccessToken(claims, email));
        tokens.put("refresh_token", createRefreshToken(claims, email, jti));
        return tokens;
    }

    public String generateAccessToken(String email, String nom, String prenom) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("nom", nom);
        claims.put("prenom", prenom);
        return createAccessToken(claims, email);
    }

    public String generateRefreshToken(String email, String nom, String prenom, UUID jti) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("nom", nom);
        claims.put("prenom", prenom);
        return createRefreshToken(claims, email, jti);
    }

    // --- Public claim extraction ---

    public String extractUsername(String token) throws Exception {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractJti(String token) throws Exception {
        return UUID.fromString(extractAllClaims(token).getId());
    }

    // --- Validation ---

    public boolean validateToken(String token) {
        try {
            return !extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    // --- Private token builders ---

    private String createAccessToken(Map<String, Object> claims, String email) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000L))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String createRefreshToken(Map<String, Object> claims, String email, UUID jti) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setId(jti.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // --- Private helpers ---

    private <T> T extractClaim(String token, Function<Claims, T> resolver) throws RuntimeException {
        try {
            return resolver.apply(extractAllClaims(token));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Claims extractAllClaims(String token) throws Exception {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }
}
