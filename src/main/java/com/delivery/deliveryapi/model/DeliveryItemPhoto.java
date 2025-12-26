package com.delivery.deliveryapi.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "delivery_item_photos")
public class DeliveryItemPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_item_id", nullable = false)
    private DeliveryItem deliveryItem;

    @Column(name = "photo_url", nullable = false, columnDefinition = "TEXT")
    private String photoUrl;

    @Column(name = "sequence_order")
    private Integer sequenceOrder = 0;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Constructors
    public DeliveryItemPhoto() {}

    public DeliveryItemPhoto(DeliveryItem deliveryItem, String photoUrl, Integer sequenceOrder) {
        this.deliveryItem = deliveryItem;
        this.photoUrl = photoUrl;
        this.sequenceOrder = sequenceOrder;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DeliveryItem getDeliveryItem() { return deliveryItem; }
    public void setDeliveryItem(DeliveryItem deliveryItem) { this.deliveryItem = deliveryItem; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Integer getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
