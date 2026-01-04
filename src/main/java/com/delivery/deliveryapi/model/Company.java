package com.delivery.deliveryapi.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "companies",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_companies_name_created_by", columnNames = {"name", "created_by_company_id"})
    },
    indexes = {
        @Index(name = "idx_companies_created_at", columnList = "created_at"),
        @Index(name = "idx_companies_updated_at", columnList = "updated_at")
    }
)
public class Company {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    // Address fields for pickup auto-detection
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "district_id")
    private UUID districtId;

    @Column(name = "province_id")
    private UUID provinceId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_company_id")
    private Company createdByCompany;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Company() {}

    public Company(String name) {
        this.name = name;
    }

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (name != null) name = name.trim();
        if (address != null) address = address.trim();
    }
}