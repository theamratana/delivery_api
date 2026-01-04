package com.delivery.deliveryapi.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "pending_employees", indexes = {
        @Index(name = "idx_pending_phone_company", columnList = "phoneE164, companyId", unique = true),
        @Index(name = "idx_pending_company", columnList = "companyId"),
        @Index(name = "idx_pending_expires", columnList = "expiresAt")
})
public class PendingEmployee {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 32, name = "phonee164")
    private String phoneE164;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 200)
    private String displayName;

    @Column(length = 255)
    private String email;

    @Column(length = 500)
    private String address;

    @Column
    private UUID defaultProvinceId;

    @Column
    private UUID defaultDistrictId;

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