package com.delivery.deliveryapi.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "otp_attempts", indexes = {
        @Index(name = "idx_otp_link_code", columnList = "linkCode", unique = true),
        @Index(name = "idx_otp_phone", columnList = "phoneE164")
})
public class OtpAttempt {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 32, name = "phonee164")
    private String phoneE164;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OtpStatus status;

    // Random short token used in deep link: https://t.me/<bot>?start=link_<linkCode>
    @Column(nullable = false, unique = true, length = 16)
    private String linkCode;

    // Telegram chat id (set after linking)
    private Long chatId;

    // Hash of the OTP code (sha256(code + ":" + linkCode))
    @Column(length = 128)
    private String codeHash;

    private int triesCount;

    private int maxTries;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
