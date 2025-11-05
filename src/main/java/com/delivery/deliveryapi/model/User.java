package com.delivery.deliveryapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = {"username"}),
        @UniqueConstraint(name = "uk_users_email", columnNames = {"email"})
    },
    indexes = {
        @Index(name = "idx_users_created_at", columnList = "created_at"),
        @Index(name = "idx_users_updated_at", columnList = "updated_at")
    }
)
public class User {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "username", length = 50)
    private String username; // lowercase, unique

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    // Legacy single phone field; not unique anymore since multi-phone support exists
    @Column(name = "phone_e164")
    private String phoneE164;

    @Column(name = "phone_verified_at")
    private OffsetDateTime phoneVerifiedAt;

    @Column(name = "email")
    private String email;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @Column(name = "is_placeholder", nullable = false)
    private boolean placeholder = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getPhoneE164() { return phoneE164; }
    public void setPhoneE164(String phoneE164) { this.phoneE164 = phoneE164; }
    public OffsetDateTime getPhoneVerifiedAt() { return phoneVerifiedAt; }
    public void setPhoneVerifiedAt(OffsetDateTime phoneVerifiedAt) { this.phoneVerifiedAt = phoneVerifiedAt; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public OffsetDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(OffsetDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }
    public boolean isPlaceholder() { return placeholder; }
    public void setPlaceholder(boolean placeholder) { this.placeholder = placeholder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(OffsetDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Transient
    public String getFullName() {
        String fn = firstName != null ? firstName.trim() : "";
        String ln = lastName != null ? lastName.trim() : "";
        String full = (fn + " " + ln).trim();
        return full.isEmpty() ? null : full;
    }

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (username != null) username = username.trim().toLowerCase();
        if (email != null) email = email.trim().toLowerCase();
        if (displayName != null) displayName = displayName.trim();
        if (firstName != null) firstName = firstName.trim();
        if (lastName != null) lastName = lastName.trim();
        if (phoneE164 != null) phoneE164 = phoneE164.trim();
        if (avatarUrl != null) avatarUrl = avatarUrl.trim();
    }
}
