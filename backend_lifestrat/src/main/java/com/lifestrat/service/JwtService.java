package com.lifestrat.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration-time}")
    private long expirationTime;

    // Сгенерировать JWT токен для пользователя
    public String generateToken(String username) {
        log.debug("Generating JWT token for username: {}", username);

        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    // Создать токен с claims и subject
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTime);

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();

        log.info("JWT token generated successfully for subject: {}", subject);
        return token;
    }

    // Получить ключ для подписи
    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Извлечь имя пользователя из токена
    public String extractUsername(String token) {
        log.debug("Extracting username from JWT token");
        return extractClaim(token, Claims::getSubject);
    }

    // Извлечь дату истечения токена
    public Date extractExpiration(String token) {
        log.debug("Extracting expiration date from JWT token");
        return extractClaim(token, Claims::getExpiration);
    }

    // Извлечь конкретный claim из токена
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Извлечь все claims из токена
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Проверить валидность токена
    public boolean validateToken(String token, String username) {
        log.debug("Validating JWT token for username: {}", username);

        try {
            final String extractedUsername = extractUsername(token);
            boolean isValid = (extractedUsername.equals(username) && !isTokenExpired(token));

            if (isValid) {
                log.debug("JWT token validation successful for username: {}", username);
            } else {
                log.warn("JWT token validation failed for username: {}", username);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error validating JWT token for username: {}. Error: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Проверить истек ли срок действия токена
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Проверить валидность токена (без проверки username)
     */
    public boolean validateToken(String token) {
        log.debug("Validating JWT token structure and expiration");

        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("JWT token validation failed. Error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Получить оставшееся время жизни токена в миллисекундах
     */
    public long getRemainingTime(String token) {
        try {
            Date expiration = extractExpiration(token);
            Date now = new Date();
            return expiration.getTime() - now.getTime();
        } catch (Exception e) {
            log.error("Error getting remaining time for token. Error: {}", e.getMessage());
            return 0;
        }
    }
}