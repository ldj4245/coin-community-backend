package com.coincommunity.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증을 위한 유틸리티 클래스
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long accessTokenValidityInMilliseconds;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenValidityInMilliseconds;
    
    private final UserDetailsService userDetailsService;
    private SecretKey key;
    
    public JwtTokenProvider(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    
    @PostConstruct
    protected void init() {
        key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 사용자 ID를 기반으로 액세스 토큰 생성
     */
    public String createAccessToken(Long userId) {
        Claims claims = Jwts.claims().setSubject(userId.toString());
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * 사용자 ID를 기반으로 리프레시 토큰 생성
     */
    public String createRefreshToken(Long userId) {
        Claims claims = Jwts.claims().setSubject(userId.toString());
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * 토큰에서 인증 정보 추출
     */
    public Authentication getAuthentication(String token) {
        String userIdStr = getUserIdFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userIdStr);
        
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
    
    /**
     * 토큰에서 사용자 ID 추출
     */
    public String getUserIdFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    
    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("유효하지 않은 JWT 토큰입니다: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 토큰이 만료되었는지 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return claims.getBody().getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.error("토큰 만료 확인 중 오류가 발생했습니다: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * 토큰의 만료 시간 추출
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return claims.getBody().getExpiration();
        } catch (Exception e) {
            log.error("토큰에서 만료 시간을 추출하는 중 오류가 발생했습니다: {}", e.getMessage());
            return null;
        }
    }
}
