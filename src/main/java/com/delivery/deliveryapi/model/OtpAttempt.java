package com.delivery.deliveryapi.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

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

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPhoneE164() { return phoneE164; }
    public void setPhoneE164(String phoneE164) { this.phoneE164 = phoneE164; }

    public OtpStatus getStatus() { return status; }
    public void setStatus(OtpStatus status) { this.status = status; }

    public String getLinkCode() { return linkCode; }
    public void setLinkCode(String linkCode) { this.linkCode = linkCode; }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

    public int getTriesCount() { return triesCount; }
    public void setTriesCount(int triesCount) { this.triesCount = triesCount; }

    public int getMaxTries() { return maxTries; }
    public void setMaxTries(int maxTries) { this.maxTries = maxTries; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
