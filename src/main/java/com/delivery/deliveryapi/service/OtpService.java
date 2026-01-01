package com.delivery.deliveryapi.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.deliveryapi.model.AuthIdentity;
import com.delivery.deliveryapi.model.AuthProvider;
import com.delivery.deliveryapi.model.OtpAttempt;
import com.delivery.deliveryapi.model.OtpStatus;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.AuthIdentityRepository;
import com.delivery.deliveryapi.repo.OtpAttemptRepository;
import com.delivery.deliveryapi.repo.UserRepository;

@Service
public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private final OtpAttemptRepository attempts;
    private final UserRepository users;
    private final AuthIdentityRepository identities;
    private final CompanyAssignmentService companyAssignmentService;
    private final TelegramBotClient tg;

    private static final SecureRandom RNG = new SecureRandom();
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final int MAX_TRIES = 5;

    public OtpService(OtpAttemptRepository attempts,
                      UserRepository users,
                      AuthIdentityRepository identities,
                      CompanyAssignmentService companyAssignmentService,
                      TelegramBotClient tg) {
        this.attempts = attempts;
        this.users = users;
        this.identities = identities;
        this.companyAssignmentService = companyAssignmentService;
        this.tg = tg;
    }

    public record OtpRequestResult(UUID attemptId, String deepLink, Instant expiresAt, boolean sentDirectly) {}

    public OtpRequestResult createOtpRequest(String phoneE164, String botUsername) {
        phoneE164 = normalizePhone(phoneE164);
        Optional<User> existingUser = users.findByPhoneE164(phoneE164);
        Optional<AuthIdentity> existingIdentity = Optional.empty();
        if (existingUser.isPresent()) {
            // Find Telegram identity for this user
            existingIdentity = identities.findAll().stream()
                    .filter(id -> id.getProvider() == AuthProvider.TELEGRAM && id.getUser().getId().equals(existingUser.get().getId()))
                    .findFirst();
        }

        OtpAttempt a = new OtpAttempt();
        a.setPhoneE164(phoneE164);
        a.setStatus(OtpStatus.PENDING);
        a.setMaxTries(MAX_TRIES);
        a.setTriesCount(0);
        a.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10))); // time to link
        a.setLinkCode(generateLinkCode());
        attempts.save(a);

        boolean sentDirectly = false;
        if (existingIdentity.isPresent()) {
            // Send OTP directly to the known chat_id
            Long chatId = Long.parseLong(existingIdentity.get().getProviderUserId());
            int code = 100000 + RNG.nextInt(900000);
            String codeStr = Integer.toString(code);
            a.setCodeHash(hashCode(codeStr, a.getLinkCode()));
            a.setChatId(chatId);
            a.setStatus(OtpStatus.SENT);
            a.setTriesCount(0);
            a.setExpiresAt(Instant.now().plus(CODE_TTL));
            attempts.save(a);

            String name = ""; // Could fetch from identity or user
            String msg = (name.isBlank() ? "" : (name + ", ")) +
                    "your verification code is: " + codeStr + "\n" +
                    "This code expires in " + CODE_TTL.toMinutes() + " minutes.";
            sendOtpMessageAsync(chatId, msg);
            sentDirectly = true;
        }

        String deepLink = sentDirectly ? null : "https://t.me/" + botUsername + "?start=link_" + a.getLinkCode();
        return new OtpRequestResult(a.getId(), deepLink, a.getExpiresAt(), sentDirectly);
    }

    @Async
    public void sendOtpMessageAsync(Long chatId, String message) {
        try {
            tg.sendMessage(chatId, message);
        } catch (Exception e) {
            log.error("Failed to send OTP message to chatId: {}", chatId, e);
        }
    }

    @Transactional
    public Optional<UUID> linkAndSendOtp(String linkCode, long chatId, Map<String, Object> tgUser) {
        Optional<OtpAttempt> opt = attempts.findByLinkCode(linkCode);
        if (opt.isEmpty()) return Optional.empty();
        OtpAttempt a = opt.get();
        if (a.getStatus() != OtpStatus.PENDING || Instant.now().isAfter(a.getExpiresAt())) return Optional.empty();

        log.info("Linking attempt for linkCode: {} chatId: {}", linkCode, chatId);

        // Check if this phone is already linked to a Telegram identity
        Optional<User> existingUser = users.findByPhoneE164(a.getPhoneE164());
        boolean hasTelegramIdentity = false;
        if (existingUser.isPresent()) {
            hasTelegramIdentity = identities.findAll().stream()
                    .anyMatch(id -> id.getProvider() == AuthProvider.TELEGRAM && id.getUser().getId().equals(existingUser.get().getId()));
        }

        if (hasTelegramIdentity) {
            // Send OTP directly to the known chat_id
            int code = 100000 + RNG.nextInt(900000);
            String codeStr = Integer.toString(code);
            a.setCodeHash(hashCode(codeStr, a.getLinkCode()));
            a.setChatId(chatId);
            a.setStatus(OtpStatus.SENT);
            a.setTriesCount(0);
            a.setExpiresAt(Instant.now().plus(CODE_TTL));
            attempts.save(a);

            String name = tgUser != null ? (String) tgUser.getOrDefault("first_name", "") : "";
            String msg = (name.isBlank() ? "" : (name + ", ")) +
                    "your verification code is: " + codeStr + "\n" +
                    "This code expires in " + CODE_TTL.toMinutes() + " minutes.";
            sendOtpMessageAsync(chatId, msg);
            return Optional.of(a.getId());
        } else {
            // Request contact to verify phone
            log.info("Setting attempt to WAITING_FOR_CONTACT for linkCode: {} chatId: {}", linkCode, chatId);
            a.setChatId(chatId);
            a.setStatus(OtpStatus.WAITING_FOR_CONTACT);
            attempts.save(a);
            log.info("Saved attempt id: {} with status WAITING_FOR_CONTACT", a.getId());
            tg.sendContactRequest(chatId, "Please share your phone number to verify your identity.");
            return Optional.of(a.getId());
        }
    }

    @Transactional
    public Optional<User> verifyCode(UUID attemptId, String code) {
    Optional<OtpAttempt> opt = attempts.findById(java.util.Objects.requireNonNull(attemptId));
        if (opt.isEmpty()) return Optional.empty();
        OtpAttempt a = opt.get();
        if (a.getStatus() != OtpStatus.SENT) return Optional.empty();
        if (Instant.now().isAfter(a.getExpiresAt())) {
            a.setStatus(OtpStatus.EXPIRED);
            attempts.save(a);
            return Optional.empty();
        }
        if (a.getTriesCount() >= a.getMaxTries()) {
            a.setStatus(OtpStatus.BLOCKED);
            attempts.save(a);
            return Optional.empty();
        }
        a.setTriesCount(a.getTriesCount() + 1);
        boolean ok = hashCode(code, a.getLinkCode()).equals(a.getCodeHash());
        if (!ok) {
            attempts.save(a);
            return Optional.empty();
        }

        // Success: mark verified and upsert user
        a.setStatus(OtpStatus.VERIFIED);
        attempts.save(a);

        User u = users.findByPhoneE164(a.getPhoneE164()).orElseGet(() -> {
            User nu = new User();
            nu.setPhoneE164(a.getPhoneE164());
            nu.setActive(true);
            nu.setIncomplete(true);
            nu.setUsername("u_" + a.getPhoneE164());
            return users.save(nu);
        });

        // Handle automatic company assignment for pending employee invitations
        companyAssignmentService.handlePhoneVerificationAssignment(u, a.getPhoneE164());

        // Link Telegram identity if chatId is known
        if (a.getChatId() != null) {
            String providerUserId = Long.toString(a.getChatId());
            Optional<AuthIdentity> existing = identities.findByProviderAndProviderUserId(AuthProvider.TELEGRAM, providerUserId);
            if (existing.isEmpty()) {
                AuthIdentity id = new AuthIdentity();
                id.setProvider(AuthProvider.TELEGRAM);
                id.setProviderUserId(providerUserId);
                id.setUser(u);
                identities.save(id);
            }
        }

        return Optional.of(u);
    }

    @Transactional
    public void processContact(long chatId, String phoneNumber) {
        String normalizedPhone = normalizePhone(phoneNumber);
        log.info("Processing contact for chatId: {} phone: {}", chatId, normalizedPhone);
        // Find waiting attempt for this chat
        log.info("Querying for attempt with chatId: {} status: WAITING_FOR_CONTACT", chatId);
        Optional<OtpAttempt> opt = attempts.findTopByChatIdAndStatusOrderByCreatedAtDesc(chatId, OtpStatus.WAITING_FOR_CONTACT);
        if (opt.isEmpty()) {
            log.warn("No attempt found for chatId: {}", chatId);
            sendOtpMessageAsync(chatId, "No pending verification request found.");
            return;
        }
        OtpAttempt a = opt.get();
        log.info("Found attempt id: {} phone: {}", a.getId(), a.getPhoneE164());

        if (!normalizedPhone.equals(a.getPhoneE164())) {
            a.setStatus(OtpStatus.BLOCKED);
            attempts.save(a);
            sendOtpMessageAsync(chatId, "The shared phone number does not match the one you entered. Verification failed.");
            return;
        }

        // Phone matches, send OTP
        int code = 100000 + RNG.nextInt(900000);
        String codeStr = Integer.toString(code);
        a.setCodeHash(hashCode(codeStr, a.getLinkCode()));
        a.setStatus(OtpStatus.SENT);
        a.setTriesCount(0);
        a.setExpiresAt(Instant.now().plus(CODE_TTL));
        attempts.save(a);

        String msg = "Your verification code is: " + codeStr + "\n" +
                "This code expires in " + CODE_TTL.toMinutes() + " minutes.";
        sendOtpMessageAsync(chatId, msg);
    }

    public static String normalizePhone(String phone) {
        if (phone == null) return null;
        phone = phone.replaceAll("\\s+", "").trim();
        if (!phone.startsWith("+")) phone = "+" + phone;
        return phone;
    }

    public static String generateLinkCode() {
        byte[] buf = new byte[12];
        RNG.nextBytes(buf);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }    public static String hashCode(String code, String salt) {
        byte[] h = sha256((code + ":" + salt).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(h);
    }

    private static byte[] sha256(byte[] input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return md.digest(input);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
