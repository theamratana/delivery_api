package com.delivery.deliveryapi.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery_photos",
    indexes = {
        @Index(name = "idx_delivery_photos_delivery_item_id", columnList = "delivery_item_id"),
        @Index(name = "idx_delivery_photos_uploaded_at", columnList = "uploaded_at"),
        @Index(name = "idx_delivery_photos_sequence_order", columnList = "sequence_order"),
        @Index(name = "idx_delivery_photos_created_at", columnList = "created_at")
    }
)
public class DeliveryPhoto extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_item_id", nullable = false)
    private DeliveryItem deliveryItem;

    @Column(name = "photo_url", nullable = false, columnDefinition = "TEXT")
    private String photoUrl;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder = 0;

    // Constructors
    public DeliveryPhoto() {}

    public DeliveryPhoto(DeliveryItem deliveryItem, String photoUrl, Integer sequenceOrder) {
        this.deliveryItem = deliveryItem;
        this.photoUrl = photoUrl;
        this.sequenceOrder = sequenceOrder != null ? sequenceOrder : 0;
        this.uploadedAt = OffsetDateTime.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DeliveryItem getDeliveryItem() { return deliveryItem; }
    public void setDeliveryItem(DeliveryItem deliveryItem) { this.deliveryItem = deliveryItem; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public OffsetDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public Integer getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }
}