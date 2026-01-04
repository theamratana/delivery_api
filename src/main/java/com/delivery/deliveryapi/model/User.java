package com.delivery.deliveryapi.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = {"username"}),
        @UniqueConstraint(name = "uk_users_email", columnNames = {"email"}),
        @UniqueConstraint(name = "uk_users_phone_e164", columnNames = {"phone_e164"})
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

    @Column(name = "phone_e164")
    private String phoneE164;

    @Column(name = "phone_verified_at")
    private OffsetDateTime phoneVerifiedAt;

    @Column(name = "email")
    private String email;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "default_address", columnDefinition = "TEXT")
    private String defaultAddress;

    @Column(name = "default_province_id")
    private UUID defaultProvinceId;

    @Column(name = "default_district_id")
    private UUID defaultDistrictId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role")
    private UserRole userRole;

    @Column(name = "is_incomplete", nullable = false, columnDefinition = "boolean default false")
    private boolean incomplete = false;

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
