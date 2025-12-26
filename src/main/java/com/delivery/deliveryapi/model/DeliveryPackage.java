package com.delivery.deliveryapi.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

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

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public Company getDeliveryCompany() { return deliveryCompany; }
    public void setDeliveryCompany(Company deliveryCompany) { this.deliveryCompany = deliveryCompany; }

    public User getDeliveryDriver() { return deliveryDriver; }
    public void setDeliveryDriver(User deliveryDriver) { this.deliveryDriver = deliveryDriver; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getPickupProvince() { return pickupProvince; }
    public void setPickupProvince(String pickupProvince) { this.pickupProvince = pickupProvince; }

    public String getPickupDistrict() { return pickupDistrict; }
    public void setPickupDistrict(String pickupDistrict) { this.pickupDistrict = pickupDistrict; }

    public BigDecimal getPickupLat() { return pickupLat; }
    public void setPickupLat(BigDecimal pickupLat) { this.pickupLat = pickupLat; }

    public BigDecimal getPickupLng() { return pickupLng; }
    public void setPickupLng(BigDecimal pickupLng) { this.pickupLng = pickupLng; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getDeliveryProvince() { return deliveryProvince; }
    public void setDeliveryProvince(String deliveryProvince) { this.deliveryProvince = deliveryProvince; }

    public String getDeliveryDistrict() { return deliveryDistrict; }
    public void setDeliveryDistrict(String deliveryDistrict) { this.deliveryDistrict = deliveryDistrict; }

    public BigDecimal getDeliveryLat() { return deliveryLat; }
    public void setDeliveryLat(BigDecimal deliveryLat) { this.deliveryLat = deliveryLat; }

    public BigDecimal getDeliveryLng() { return deliveryLng; }
    public void setDeliveryLng(BigDecimal deliveryLng) { this.deliveryLng = deliveryLng; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public BigDecimal getDeliveryDiscount() { return deliveryDiscount; }
    public void setDeliveryDiscount(BigDecimal deliveryDiscount) { this.deliveryDiscount = deliveryDiscount; }

    public BigDecimal getOrderDiscount() { return orderDiscount; }
    public void setOrderDiscount(BigDecimal orderDiscount) { this.orderDiscount = orderDiscount; }

    public BigDecimal getActualDeliveryCost() { return actualDeliveryCost; }
    public void setActualDeliveryCost(BigDecimal actualDeliveryCost) { this.actualDeliveryCost = actualDeliveryCost; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getExchangeRateUsed() { return exchangeRateUsed; }
    public void setExchangeRateUsed(BigDecimal exchangeRateUsed) { this.exchangeRateUsed = exchangeRateUsed; }

    public BigDecimal getKhrAmount() { return khrAmount; }
    public void setKhrAmount(BigDecimal khrAmount) { this.khrAmount = khrAmount; }

    public BigDecimal getSubTotal() { return subTotal; }
    public void setSubTotal(BigDecimal subTotal) { this.subTotal = subTotal; }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public OffsetDateTime getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
    public void setEstimatedDeliveryTime(OffsetDateTime estimatedDeliveryTime) { this.estimatedDeliveryTime = estimatedDeliveryTime; }

    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public String getLastStatusNote() { return lastStatusNote; }
    public void setLastStatusNote(String lastStatusNote) { this.lastStatusNote = lastStatusNote; }

    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getAutoCreatedCompany() { return autoCreatedCompany; }
    public void setAutoCreatedCompany(Boolean autoCreatedCompany) { this.autoCreatedCompany = autoCreatedCompany; }

    public Boolean getAutoCreatedDriver() { return autoCreatedDriver; }
    public void setAutoCreatedDriver(Boolean autoCreatedDriver) { this.autoCreatedDriver = autoCreatedDriver; }

    public Boolean getAutoCreatedReceiver() { return autoCreatedReceiver; }
    public void setAutoCreatedReceiver(Boolean autoCreatedReceiver) { this.autoCreatedReceiver = autoCreatedReceiver; }

    public Boolean getFeeAutoCalculated() { return feeAutoCalculated; }
    public void setFeeAutoCalculated(Boolean feeAutoCalculated) { this.feeAutoCalculated = feeAutoCalculated; }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }

    public List<DeliveryItem> getItems() { return items; }
    public void setItems(List<DeliveryItem> items) { this.items = items; }

    public List<DeliveryPackagePhoto> getPhotos() { return photos; }
    public void setPhotos(List<DeliveryPackagePhoto> photos) { this.photos = photos; }

    public List<DeliveryTracking> getTrackingHistory() { return trackingHistory; }
    public void setTrackingHistory(List<DeliveryTracking> trackingHistory) { this.trackingHistory = trackingHistory; }
}
