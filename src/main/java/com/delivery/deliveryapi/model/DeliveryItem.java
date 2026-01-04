package com.delivery.deliveryapi.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "delivery_items")
public class DeliveryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private DeliveryPackage deliveryPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "item_description", columnDefinition = "TEXT")
    private String itemDescription;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "item_value", precision = 10, scale = 2)
    private BigDecimal itemValue;

    @Column(name = "item_discount", precision = 10, scale = 2)
    private BigDecimal itemDiscount = BigDecimal.ZERO;

    @Column(name = "sequence_order")
    private Integer sequenceOrder = 0;

    // Audit fields
    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // Relationships
    @OneToMany(mappedBy = "deliveryItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryItemPhoto> photos;

    // Constructors
    public DeliveryItem() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
