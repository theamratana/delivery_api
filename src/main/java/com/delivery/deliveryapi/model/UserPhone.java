package com.delivery.deliveryapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_phones",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_phones_phone", columnNames = {"phone_e164"}),
                @UniqueConstraint(name = "uk_user_phones_user_phone", columnNames = {"user_id", "phone_e164"})
        },
        indexes = {
                @Index(name = "idx_user_phones_user", columnList = "user_id"),
                @Index(name = "idx_user_phones_created_at", columnList = "created_at")
        }
)
public class UserPhone {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
        @JsonIgnore
        private User user;

    @Column(name = "phone_e164", nullable = false, length = 32)
    private String phoneE164;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getPhoneE164() { return phoneE164; }
    public void setPhoneE164(String phoneE164) { this.phoneE164 = phoneE164; }
    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
    public OffsetDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(OffsetDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (phoneE164 != null) phoneE164 = phoneE164.trim(); // Expect E.164 format
    }
}
