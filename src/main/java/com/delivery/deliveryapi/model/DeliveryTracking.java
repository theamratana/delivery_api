package com.delivery.deliveryapi.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery_tracking",
    indexes = {
        @Index(name = "idx_delivery_tracking_delivery_item_id", columnList = "delivery_item_id"),
        @Index(name = "idx_delivery_tracking_status", columnList = "status"),
        @Index(name = "idx_delivery_tracking_timestamp", columnList = "timestamp"),
        @Index(name = "idx_delivery_tracking_updated_by", columnList = "updated_by"),
        @Index(name = "idx_delivery_tracking_created_at", columnList = "created_at")
    }
)
public class DeliveryTracking extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_item_id", nullable = false)
    private DeliveryItem deliveryItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliveryStatus status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_updated_by")
    private User statusUpdatedBy;

    @Column(name = "location", columnDefinition = "TEXT")
    private String location;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    // Constructors
    public DeliveryTracking() {}

    public DeliveryTracking(DeliveryItem deliveryItem, DeliveryStatus status, String description, User statusUpdatedBy) {
        this.deliveryItem = deliveryItem;
        this.status = status;
        this.description = description;
        this.statusUpdatedBy = statusUpdatedBy;
        this.timestamp = OffsetDateTime.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DeliveryItem getDeliveryItem() { return deliveryItem; }
    public void setDeliveryItem(DeliveryItem deliveryItem) { this.deliveryItem = deliveryItem; }

    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public User getStatusUpdatedBy() { return statusUpdatedBy; }
    public void setStatusUpdatedBy(User statusUpdatedBy) { this.statusUpdatedBy = statusUpdatedBy; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
}