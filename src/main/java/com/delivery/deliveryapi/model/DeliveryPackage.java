package com.delivery.deliveryapi.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "delivery_packages")
public class DeliveryPackage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // Sender information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_phone", length = 20)
    private String senderPhone;

    // Receiver information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    // Delivery handler
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_company_id")
    private Company deliveryCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_driver_id")
    private User deliveryDriver;

    // Pickup location
    @Column(name = "pickup_address", columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(name = "pickup_province", length = 100)
    private String pickupProvince;

    @Column(name = "pickup_district", length = 100)
    private String pickupDistrict;

    @Column(name = "pickup_lat", precision = 10, scale = 8)
    private BigDecimal pickupLat;

    @Column(name = "pickup_lng", precision = 11, scale = 8)
    private BigDecimal pickupLng;

    // Delivery location
    @Column(name = "delivery_address", nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "delivery_province", length = 100)
    private String deliveryProvince;

    @Column(name = "delivery_district", length = 100)
    private String deliveryDistrict;

    @Column(name = "delivery_lat", precision = 10, scale = 8)
    private BigDecimal deliveryLat;

    @Column(name = "delivery_lng", precision = 11, scale = 8)
    private BigDecimal deliveryLng;

    // Pricing
    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "delivery_discount", precision = 10, scale = 2)
    private BigDecimal deliveryDiscount = BigDecimal.ZERO;

    @Column(name = "order_discount", precision = 10, scale = 2)
    private BigDecimal orderDiscount = BigDecimal.ZERO;

    @Column(name = "actual_delivery_cost", precision = 10, scale = 2)
    private BigDecimal actualDeliveryCost;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "exchange_rate_used", precision = 10, scale = 4)
    private BigDecimal exchangeRateUsed;

    @Column(name = "khr_amount", precision = 15, scale = 2)
    private BigDecimal khrAmount;

    @Column(name = "sub_total", precision = 10, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "grand_total", precision = 10, scale = 2)
    private BigDecimal grandTotal;

    // Delivery information
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Column(name = "estimated_delivery_time")
    private OffsetDateTime estimatedDeliveryTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DeliveryStatus status = DeliveryStatus.CREATED;

    @Column(name = "last_status_note", columnDefinition = "TEXT")
    private String lastStatusNote;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Auto-creation flags
    @Column(name = "auto_created_company")
    private Boolean autoCreatedCompany = false;

    @Column(name = "auto_created_driver")
    private Boolean autoCreatedDriver = false;

    @Column(name = "auto_created_receiver")
    private Boolean autoCreatedReceiver = false;

    @Column(name = "fee_auto_calculated")
    private Boolean feeAutoCalculated = false;

    // Soft delete
    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // Relationships
    @OneToMany(mappedBy = "deliveryPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryItem> items;

    @OneToMany(mappedBy = "deliveryPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryPackagePhoto> photos;

    @OneToMany(mappedBy = "deliveryPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryTracking> trackingHistory;

    // Constructors
    public DeliveryPackage() {}
}
