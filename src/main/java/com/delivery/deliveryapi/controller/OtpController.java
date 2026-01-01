package com.delivery.deliveryapi.controller;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OtpController.class);
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
    public record OtpVerifyResponse(String token, UUID userId, String displayName, String username, String provider) {}

    @PostMapping("/verify")
    public ResponseEntity<Object> verify(@Valid @RequestBody VerifyBodyDto body) {
        try {
            UUID id = UUID.fromString(body.attemptId());
            Optional<User> user = otpService.verifyCode(id, body.code());
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid or expired verification code"));
            }
            User u = user.get();
            String token = jwtService.generateToken(u.getId(), u.getUsername(), Map.of("provider", "TELEGRAM-OTP"));
            String displayName = u.getFullName();
            if (displayName == null) {
                displayName = u.getDisplayName() != null ? u.getDisplayName() : u.getUsername();
            }
            return ResponseEntity.ok(new OtpVerifyResponse(token, u.getId(), displayName, u.getUsername(), "TELEGRAM-OTP"));
        } catch (DataIntegrityViolationException e) {
            log.error("DataIntegrityViolationException during OTP verification", e);
            String message = e.getMessage() != null ? e.getMessage() : "Unknown constraint violation";
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Phone number already registered or constraint violation", "details", message));
        } catch (Exception e) {
            log.error("Exception during OTP verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Verification failed", "details", e.getMessage()));
        }
    }
}
