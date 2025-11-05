package com.delivery.deliveryapi.security;

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
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private final Key signingKey;
    private final String issuer;
    private final long expMinutes;

    public JwtService(
            @Value("${jwt.secret:dev-secret-change}") String secret,
            @Value("${jwt.issuer:delivery-api}") String issuer,
            @Value("${jwt.exp-minutes:10080}") long expMinutes
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
        this.expMinutes = expMinutes;
    }

    public String generateToken(UUID userId, String username, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expMinutes * 60);
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(extraClaims != null ? extraClaims : Map.of())
                .claim("username", username)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateAndParse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
