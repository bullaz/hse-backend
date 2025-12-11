package com.stellarix.hse.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtService {

	@Value("${jwt.secret}")
    private String SECRET;

    public Map<String,String> generateToken(String email) { // Use email as username
        Map<String, Object> claims = new HashMap<>();
        Map<String, String> tokens = new HashMap<>();
      //return createToken(claims, email);
        tokens.put("access_token", createToken(claims, email));
        tokens.put("refresh_token", createRefreshToken(claims, email));
        return tokens;
    }
    
    public String generateAccessToken(String email) { // Use email as username
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email);
    }

    private String createToken(Map<String, Object> claims, String email) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 5 * 1000))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    private String createRefreshToken(Map<String, Object> claims, String email) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                //.setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
                .setExpiration(new Date(System.currentTimeMillis() + 60 * 1000))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) throws Exception{
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) throws Exception{
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) throws Exception {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) throws Exception{
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) throws Exception{
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token/*, UserDetails userDetails*/){
        //final String username = extractUsername(token);
        
        //return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    	//log.info(String.format("validate token %b",!isTokenExpired(token)));
    	boolean etat = false;
    	try {
    		etat = !isTokenExpired(token);
    	}catch(Exception e) {
    		etat = false;
    	}
    	return etat;
    }
}