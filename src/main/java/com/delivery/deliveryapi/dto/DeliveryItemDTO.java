package com.delivery.deliveryapi.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.delivery.deliveryapi.model.DeliveryItem;
import com.delivery.deliveryapi.model.DeliveryStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeliveryItemDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("itemDescription")
    private String itemDescription;

    @JsonProperty("status")
    private DeliveryStatus status;

    @JsonProperty("paymentMethod")
    private String paymentMethod;

    @JsonProperty("estimatedDeliveryTime")
    private OffsetDateTime estimatedDeliveryTime;

    @JsonProperty("pickupAddress")
    private String pickupAddress;

    @JsonProperty("pickupProvince")
    private String pickupProvince;

    @JsonProperty("pickupDistrict")
    private String pickupDistrict;

    @JsonProperty("deliveryAddress")
    private String deliveryAddress;

    @JsonProperty("deliveryProvince")
    private String deliveryProvince;

    @JsonProperty("deliveryDistrict")
    private String deliveryDistrict;

    @JsonProperty("pickupLat")
    private BigDecimal pickupLat;

    @JsonProperty("pickupLng")
    private BigDecimal pickupLng;

    @JsonProperty("deliveryLat")
    private BigDecimal deliveryLat;

    @JsonProperty("deliveryLng")
    private BigDecimal deliveryLng;

    @JsonProperty("deliveryFee")
    private BigDecimal deliveryFee;

    @JsonProperty("itemValue")
    private BigDecimal itemValue;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("senderId")
    private UUID senderId;

    @JsonProperty("senderName")
    private String senderName;

    @JsonProperty("receiverId")
    private UUID receiverId;

    @JsonProperty("receiverName")
    private String receiverName;

    @JsonProperty("deliveryCompanyId")
    private UUID deliveryCompanyId;

    @JsonProperty("deliveryCompanyName")
    private String deliveryCompanyName;

    @JsonProperty("deliveryDriverId")
    private UUID deliveryDriverId;

    @JsonProperty("deliveryDriverName")
    private String deliveryDriverName;

    @JsonProperty("productId")
    private UUID productId;

    @JsonProperty("photoUrls")
    private String photoUrls;

    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    @JsonProperty("updatedAt")
    private OffsetDateTime updatedAt;

    // Constructor (used by Jackson deserialization)
    public DeliveryItemDTO() {
        // Default constructor for Jackson
    }

    // Factory method to convert DeliveryItem to DTO
    public static DeliveryItemDTO fromDeliveryItem(DeliveryItem item) {
        DeliveryItemDTO dto = new DeliveryItemDTO();
        dto.id = item.getId();
        dto.itemDescription = item.getItemDescription();
        dto.status = item.getStatus();
        dto.paymentMethod = item.getPaymentMethod() != null ? item.getPaymentMethod().getCode() : "COD";
        dto.estimatedDeliveryTime = item.getEstimatedDeliveryTime();
        dto.pickupAddress = item.getPickupAddress();
        dto.pickupProvince = item.getPickupProvince();
        dto.pickupDistrict = item.getPickupDistrict();
        dto.deliveryAddress = item.getDeliveryAddress();
        dto.deliveryProvince = item.getDeliveryProvince();
        dto.deliveryDistrict = item.getDeliveryDistrict();
        dto.pickupLat = item.getPickupLat();
        dto.pickupLng = item.getPickupLng();
        dto.deliveryLat = item.getDeliveryLat();
        dto.deliveryLng = item.getDeliveryLng();
        dto.deliveryFee = item.getDeliveryFee();
        dto.itemValue = item.getItemValue();
        dto.quantity = item.getQuantity();
        dto.currency = item.getCurrency();
        
        // Map sender info
        if (item.getSender() != null) {
            dto.senderId = item.getSender().getId();
            dto.senderName = item.getSender().getDisplayName();
        }
        
        // Map receiver info
        if (item.getReceiver() != null) {
            dto.receiverId = item.getReceiver().getId();
            dto.receiverName = item.getReceiver().getDisplayName();
        }
        
        // Map delivery company info
        if (item.getDeliveryCompany() != null) {
            dto.deliveryCompanyId = item.getDeliveryCompany().getId();
            dto.deliveryCompanyName = item.getDeliveryCompany().getName();
        }
        
        // Map delivery driver info
        if (item.getDeliveryDriver() != null) {
            dto.deliveryDriverId = item.getDeliveryDriver().getId();
            dto.deliveryDriverName = item.getDeliveryDriver().getDisplayName();
        }
        
        // Map product info
        if (item.getProduct() != null) {
            dto.productId = item.getProduct().getId();
        }
        
        dto.photoUrls = item.getPhotoUrls();
        dto.createdAt = item.getCreatedAt();
        dto.updatedAt = item.getUpdatedAt();
        
        return dto;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

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

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public UUID getSenderId() { return senderId; }
    public void setSenderId(UUID senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public UUID getReceiverId() { return receiverId; }
    public void setReceiverId(UUID receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public UUID getDeliveryCompanyId() { return deliveryCompanyId; }
    public void setDeliveryCompanyId(UUID deliveryCompanyId) { this.deliveryCompanyId = deliveryCompanyId; }

    public String getDeliveryCompanyName() { return deliveryCompanyName; }
    public void setDeliveryCompanyName(String deliveryCompanyName) { this.deliveryCompanyName = deliveryCompanyName; }

    public UUID getDeliveryDriverId() { return deliveryDriverId; }
    public void setDeliveryDriverId(UUID deliveryDriverId) { this.deliveryDriverId = deliveryDriverId; }

    public String getDeliveryDriverName() { return deliveryDriverName; }
    public void setDeliveryDriverName(String deliveryDriverName) { this.deliveryDriverName = deliveryDriverName; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public String getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(String photoUrls) { this.photoUrls = photoUrls; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
