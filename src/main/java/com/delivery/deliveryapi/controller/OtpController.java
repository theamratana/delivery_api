package com.delivery.deliveryapi.controller;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.security.JwtService;
import com.delivery.deliveryapi.service.OtpService;
import com.delivery.deliveryapi.service.TelegramBotClient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/auth/otp")
@Validated
public class OtpController {
    private final OtpService otpService;
    private final TelegramBotClient tg;
    private final JwtService jwtService;

    public OtpController(OtpService otpService, TelegramBotClient tg, JwtService jwtService) {
        this.otpService = otpService;
        this.tg = tg;
        this.jwtService = jwtService;
    }

    public record RequestBodyDto(@NotBlank String phone_e164) {}
    public record RequestResponse(UUID attemptId, String deepLink, Instant expiresAt, boolean sentDirectly) {}

    @PostMapping("/request")
    public ResponseEntity<RequestResponse> request(@Valid @RequestBody RequestBodyDto body) {
        var res = otpService.createOtpRequest(body.phone_e164(), tg.getBotUsername());
        return ResponseEntity.ok(new RequestResponse(res.attemptId(), res.deepLink(), res.expiresAt(), res.sentDirectly()));
    }

    public record VerifyBodyDto(@NotBlank String attemptId, @NotBlank String code) {}

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@Valid @RequestBody VerifyBodyDto body) {
        UUID id = UUID.fromString(body.attemptId());
        Optional<User> user = otpService.verifyCode(id, body.code());
        if (user.isEmpty()) return ResponseEntity.status(400).body(Map.of("error", "invalid_or_expired_code"));
        User u = user.get();
        String token = jwtService.generateToken(u.getId(), u.getUsername(), Map.of("provider", "TELEGRAM-OTP"));
        return ResponseEntity.ok(Map.of("token", token, "userId", u.getId()));
    }
}
