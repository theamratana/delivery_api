package com.delivery.deliveryapi.controller;

import com.delivery.deliveryapi.model.AuthIdentity;
import com.delivery.deliveryapi.model.AuthProvider;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.AuthIdentityRepository;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.service.TelegramAuthService;
import com.delivery.deliveryapi.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final TelegramAuthService telegramAuthService;
    private final JwtService jwtService;
    private final boolean devTokenEnabled;
    private final UserRepository userRepository;
    private final AuthIdentityRepository identityRepository;

    public AuthController(TelegramAuthService telegramAuthService,
                          UserRepository userRepository,
                          AuthIdentityRepository identityRepository,
                          JwtService jwtService,
                          @Value("${jwt.dev-enabled:false}") boolean devTokenEnabled) {
        this.telegramAuthService = telegramAuthService;
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.jwtService = jwtService;
        this.devTokenEnabled = devTokenEnabled;
    }

    public static class TelegramVerifyRequest {
        public String id;
        @JsonProperty("first_name") public String firstName;
        @JsonProperty("last_name") public String lastName;
        public String username;
        @JsonProperty("photo_url") public String photoUrl;
        @JsonProperty("auth_date") public String authDate;
        public String hash;

        public Map<String, String> toMap() {
            Map<String, String> m = new HashMap<>();
            if (id != null) m.put("id", id);
            if (firstName != null) m.put("first_name", firstName);
            if (lastName != null) m.put("last_name", lastName);
            if (username != null) m.put("username", username);
            if (photoUrl != null) m.put("photo_url", photoUrl);
            if (authDate != null) m.put("auth_date", authDate);
            if (hash != null) m.put("hash", hash);
            return m;
        }
    }

    public record AuthResponse(String token, UUID userId, String displayName, String username, String provider) {}

    @PostMapping("/telegram/verify")
    @Transactional
    public ResponseEntity<?> verifyTelegram(@RequestBody TelegramVerifyRequest req) {
        Map<String, String> payload = req.toMap();
        if (!telegramAuthService.verifyLoginPayload(payload)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_signature"));
        }

        String providerUserId = req.id;
        Optional<AuthIdentity> existing = identityRepository
                .findByProviderAndProviderUserId(AuthProvider.TELEGRAM, providerUserId);

        User user;
        AuthIdentity identity;
        if (existing.isPresent()) {
            identity = existing.get();
            identity.setLastLoginAt(OffsetDateTime.now());
            identity.setDisplayName(displayNameFrom(req));
            identity.setUsername(req.username);
            user = identity.getUser();
            // update user profile details from Telegram
            user.setFirstName(req.firstName);
            user.setLastName(req.lastName);
            user.setUsername(req.username);
            user.setAvatarUrl(req.photoUrl);
            user.setLastLoginAt(OffsetDateTime.now());
        } else {
            user = new User();
            user.setDisplayName(displayNameFrom(req));
            user.setFirstName(req.firstName);
            user.setLastName(req.lastName);
            user.setUsername(req.username);
            user.setAvatarUrl(req.photoUrl);
            user.setLastLoginAt(OffsetDateTime.now());
            user = userRepository.save(user);

            identity = new AuthIdentity();
            identity.setUser(user);
            identity.setProvider(AuthProvider.TELEGRAM);
            identity.setProviderUserId(providerUserId);
            identity.setUsername(req.username);
            identity.setDisplayName(displayNameFrom(req));
            identity.setLastLoginAt(OffsetDateTime.now());
        }
        identityRepository.save(identity);

    String display = user.getFullName() != null ? user.getFullName() :
        (user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());

    String token = jwtService.generateToken(
        user.getId(),
        user.getUsername(),
        Map.of("provider", "TELEGRAM")
    );

    return ResponseEntity.ok(new AuthResponse(token, user.getId(), display, user.getUsername(), "TELEGRAM"));
    }

    private static String displayNameFrom(TelegramVerifyRequest req) {
        String first = Optional.ofNullable(req.firstName).orElse("");
        String last = Optional.ofNullable(req.lastName).orElse("");
        String full = (first + " " + last).trim();
        if (!full.isEmpty()) return full;
        if (req.username != null) return req.username;
        return "TG-" + req.id;
    }

    @GetMapping("/dev/token/{userId}")
    public ResponseEntity<?> devToken(@PathVariable UUID userId) {
        if (!devTokenEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "dev_token_disabled"));
        }
        return userRepository.findById(userId)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of(
                        "token", jwtService.generateToken(u.getId(), u.getUsername(), Map.of("provider", "DEV")),
                        "userId", u.getId()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/dev/token/new")
    @Transactional
    public ResponseEntity<?> devNewToken() {
        if (!devTokenEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "dev_token_disabled"));
        }

        // Create a minimal placeholder user for development/testing
        User u = new User();
        String uname = ("dev_" + UUID.randomUUID().toString().substring(0, 8)).toLowerCase();
        u.setUsername(uname);
        u.setDisplayName("Dev User");
        u.setPlaceholder(true);
        u.setActive(true);
        u = userRepository.save(u);

        String token = jwtService.generateToken(u.getId(), u.getUsername(), Map.of("provider", "DEV"));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "token", token,
                "userId", u.getId(),
                "username", u.getUsername()
        ));
    }
}
