package com.delivery.deliveryapi.service;

import com.delivery.deliveryapi.config.TelegramBotProperties;
import org.springframework.stereotype.Service;

// Using fully-qualified class names in methods; no need to import
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TelegramAuthService {
    private final TelegramBotProperties props;

    public TelegramAuthService(TelegramBotProperties props) {
        this.props = props;
    }

    public boolean verifyLoginPayload(Map<String, String> payload) {
        String botToken = props.getBotToken();
        if (botToken == null || botToken.isBlank()) return false;

        String providedHash = payload.get("hash");
        if (providedHash == null) return false;

        String dataCheckString = payload.entrySet().stream()
                .filter(e -> !e.getKey().equals("hash"))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secretKey = sha256(botToken.getBytes(StandardCharsets.UTF_8));
        byte[] hmac = hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8));
        String computedHex = bytesToHex(hmac);
        return computedHex.equalsIgnoreCase(providedHash);
    }

    private static byte[] sha256(byte[] input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return md.digest(input);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (java.security.NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
