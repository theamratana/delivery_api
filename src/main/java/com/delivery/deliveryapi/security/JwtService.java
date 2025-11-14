package com.delivery.deliveryapi.security;

import com.delivery.deliveryapi.model.RefreshToken;
import com.delivery.deliveryapi.repo.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private final Key signingKey;
    private final String issuer;
    private final long accessExpMinutes;
    private final long refreshExpMinutes;
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtService(
            @Value("${jwt.secret:dev-secret-change}") String secret,
            @Value("${jwt.issuer:delivery-api}") String issuer,
            @Value("${jwt.access-exp-minutes:14400}") long accessExpMinutes,
            @Value("${jwt.refresh-exp-minutes:10080}") long refreshExpMinutes,
            RefreshTokenRepository refreshTokenRepository
    ) {
        // Accept plain text or base64; ensure an HS256-safe 256-bit key is derived
        byte[] keyBytes = null;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (DecodingException | IllegalArgumentException ex) {
            // Not valid base64; derive a 256-bit key from the provided secret using SHA-256
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                keyBytes = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to initialize JWT signing key", e);
            }
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.accessExpMinutes = accessExpMinutes;
        this.refreshExpMinutes = refreshExpMinutes;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String generateAccessToken(UUID userId, String username, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessExpMinutes * 60);
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(extraClaims != null ? extraClaims : Map.of())
                .claim("username", username)
                .claim("type", "access")
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UUID userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshExpMinutes * 60);
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("username", username)
                .claim("type", "refresh")
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Keep the old method for backward compatibility with dev tokens
    public String generateToken(UUID userId, String username, Map<String, Object> extraClaims) {
        return generateAccessToken(userId, username, extraClaims);
    }

    public Claims validateAndParse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Claims validateAccessToken(String token) {
        Claims claims = validateAndParse(token);
        String type = claims.get("type", String.class);
        if (!"access".equals(type)) {
            throw new io.jsonwebtoken.security.SecurityException("Invalid token type");
        }
        return claims;
    }

    public Claims validateRefreshToken(String token) {
        Claims claims = validateAndParse(token);
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new io.jsonwebtoken.security.SecurityException("Invalid token type");
        }

        // Check if refresh token exists in database
        String tokenHash = hashToken(token);
        if (!refreshTokenRepository.existsByTokenHash(tokenHash)) {
            throw new io.jsonwebtoken.security.SecurityException("Refresh token has been revoked");
        }

        return claims;
    }

    public void storeRefreshToken(String refreshToken, UUID userId, String deviceInfo, String ipAddress) {
        String tokenHash = hashToken(refreshToken);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(refreshExpMinutes);

        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setTokenHash(tokenHash);
        tokenEntity.setUserId(userId);
        tokenEntity.setExpiresAt(expiresAt);
        tokenEntity.setDeviceInfo(deviceInfo);
        tokenEntity.setIpAddress(ipAddress);

        refreshTokenRepository.save(tokenEntity);
    }

    public void revokeRefreshToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(refreshTokenRepository::delete);
    }

    public void revokeAllUserRefreshTokens(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private String hashToken(String token) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
