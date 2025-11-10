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
@Table(name = "delivery_items",
    indexes = {
        @Index(name = "idx_delivery_items_sender_id", columnList = "sender_id"),
        @Index(name = "idx_delivery_items_receiver_id", columnList = "receiver_id"),
        @Index(name = "idx_delivery_items_delivery_company_id", columnList = "delivery_company_id"),
        @Index(name = "idx_delivery_items_delivery_driver_id", columnList = "delivery_driver_id"),
        @Index(name = "idx_delivery_items_status", columnList = "status"),
        @Index(name = "idx_delivery_items_created_at", columnList = "created_at"),
        @Index(name = "idx_delivery_items_updated_at", columnList = "updated_at"),
        @Index(name = "idx_delivery_items_deleted_at", columnList = "deleted_at")
    }
)
public class DeliveryItem extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_company_id")
    private Company deliveryCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_driver_id")
    private User deliveryDriver;

    @Column(name = "item_description", columnDefinition = "TEXT")
    private String itemDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliveryStatus status = DeliveryStatus.CREATED;

    @Column(name = "estimated_delivery_time")
    private OffsetDateTime estimatedDeliveryTime;

    @Column(name = "pickup_address", columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(name = "pickup_province")
    private String pickupProvince;

    @Column(name = "pickup_district")
    private String pickupDistrict;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "delivery_province")
    private String deliveryProvince;

    @Column(name = "delivery_district")
    private String deliveryDistrict;

    @Column(name = "pickup_lat", precision = 10, scale = 8)
    private BigDecimal pickupLat;

    @Column(name = "pickup_lng", precision = 11, scale = 8)
    private BigDecimal pickupLng;

    @Column(name = "delivery_lat", precision = 10, scale = 8)
    private BigDecimal deliveryLat;

    @Column(name = "delivery_lng", precision = 11, scale = 8)
    private BigDecimal deliveryLng;

    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "item_value", precision = 10, scale = 2)
    private BigDecimal itemValue;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    // Constructors
    public DeliveryItem() {}

    public DeliveryItem(User sender, String itemDescription, String pickupAddress, String deliveryAddress) {
        this.sender = sender;
        this.itemDescription = itemDescription;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public Company getDeliveryCompany() { return deliveryCompany; }
    public void setDeliveryCompany(Company deliveryCompany) { this.deliveryCompany = deliveryCompany; }

    public User getDeliveryDriver() { return deliveryDriver; }
    public void setDeliveryDriver(User deliveryDriver) { this.deliveryDriver = deliveryDriver; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public OffsetDateTime getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
    public void setEstimatedDeliveryTime(OffsetDateTime estimatedDeliveryTime) { this.estimatedDeliveryTime = estimatedDeliveryTime; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getPickupProvince() { return pickupProvince; }
    public void setPickupProvince(String pickupProvince) { this.pickupProvince = pickupProvince; }

    public String getPickupDistrict() { return pickupDistrict; }
    public void setPickupDistrict(String pickupDistrict) { this.pickupDistrict = pickupDistrict; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getDeliveryProvince() { return deliveryProvince; }
    public void setDeliveryProvince(String deliveryProvince) { this.deliveryProvince = deliveryProvince; }

    public String getDeliveryDistrict() { return deliveryDistrict; }
    public void setDeliveryDistrict(String deliveryDistrict) { this.deliveryDistrict = deliveryDistrict; }

    public BigDecimal getPickupLat() { return pickupLat; }
    public void setPickupLat(BigDecimal pickupLat) { this.pickupLat = pickupLat; }

    public BigDecimal getPickupLng() { return pickupLng; }
    public void setPickupLng(BigDecimal pickupLng) { this.pickupLng = pickupLng; }

    public BigDecimal getDeliveryLat() { return deliveryLat; }
    public void setDeliveryLat(BigDecimal deliveryLat) { this.deliveryLat = deliveryLat; }

    public BigDecimal getDeliveryLng() { return deliveryLng; }
    public void setDeliveryLng(BigDecimal deliveryLng) { this.deliveryLng = deliveryLng; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public BigDecimal getItemValue() { return itemValue; }
    public void setItemValue(BigDecimal itemValue) { this.itemValue = itemValue; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}