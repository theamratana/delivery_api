package com.delivery.deliveryapi.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "delivery_tracking")
public class DeliveryTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_package_id", nullable = false)
    private DeliveryPackage deliveryPackage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DeliveryStatus status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @Column(name = "location", columnDefinition = "TEXT")
    private String location;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "deleted")
    private Boolean deleted = false;

    // Constructors
    public DeliveryTracking() {}

    public DeliveryTracking(DeliveryPackage deliveryPackage, DeliveryStatus status, String description) {
        this.deliveryPackage = deliveryPackage;
        this.status = status;
        this.description = description;
        this.timestamp = OffsetDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DeliveryPackage getDeliveryPackage() { return deliveryPackage; }
    public void setDeliveryPackage(DeliveryPackage deliveryPackage) { this.deliveryPackage = deliveryPackage; }

    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public User getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
