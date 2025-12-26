package com.delivery.deliveryapi.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

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

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DeliveryPackage getDeliveryPackage() { return deliveryPackage; }
    public void setDeliveryPackage(DeliveryPackage deliveryPackage) { this.deliveryPackage = deliveryPackage; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getItemValue() { return itemValue; }
    public void setItemValue(BigDecimal itemValue) { this.itemValue = itemValue; }

    public BigDecimal getItemDiscount() { return itemDiscount; }
    public void setItemDiscount(BigDecimal itemDiscount) { this.itemDiscount = itemDiscount; }

    public Integer getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }

    public List<DeliveryItemPhoto> getPhotos() { return photos; }
    public void setPhotos(List<DeliveryItemPhoto> photos) { this.photos = photos; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
