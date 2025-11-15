package com.delivery.deliveryapi.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a batch delivery with multiple items
 * Groups all items from a single delivery batch (same receiver, address, fee)
 */
public class DeliveryBatchDTO {

    @JsonProperty("batchId")
    private String batchId; // First item ID as batch identifier

    @JsonProperty("receiverId")
    private UUID receiverId;

    @JsonProperty("receiverName")
    private String receiverName;

    @JsonProperty("receiverPhone")
    private String receiverPhone;

    @JsonProperty("deliveryAddress")
    private String deliveryAddress;

    @JsonProperty("deliveryProvince")
    private String deliveryProvince;

    @JsonProperty("deliveryDistrict")
    private String deliveryDistrict;

    @JsonProperty("deliveryFee")
    private BigDecimal deliveryFee;

    @JsonProperty("deliveryCompanyId")
    private UUID deliveryCompanyId;

    @JsonProperty("deliveryCompanyName")
    private String deliveryCompanyName;

    @JsonProperty("deliveryDriverId")
    private UUID deliveryDriverId;

    @JsonProperty("deliveryDriverName")
    private String deliveryDriverName;

    @JsonProperty("status")
    private String status;

    @JsonProperty("paymentMethod")
    private String paymentMethod;

    @JsonProperty("estimatedDeliveryTime")
    private OffsetDateTime estimatedDeliveryTime;

    @JsonProperty("itemCount")
    private Integer itemCount; // Number of items in this batch

    @JsonProperty("items")
    private List<DeliveryBatchItemDTO> items = new ArrayList<>();

    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    @JsonProperty("updatedAt")
    private OffsetDateTime updatedAt;

    // Getters and setters
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public UUID getReceiverId() { return receiverId; }
    public void setReceiverId(UUID receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getDeliveryProvince() { return deliveryProvince; }
    public void setDeliveryProvince(String deliveryProvince) { this.deliveryProvince = deliveryProvince; }

    public String getDeliveryDistrict() { return deliveryDistrict; }
    public void setDeliveryDistrict(String deliveryDistrict) { this.deliveryDistrict = deliveryDistrict; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public UUID getDeliveryCompanyId() { return deliveryCompanyId; }
    public void setDeliveryCompanyId(UUID deliveryCompanyId) { this.deliveryCompanyId = deliveryCompanyId; }

    public String getDeliveryCompanyName() { return deliveryCompanyName; }
    public void setDeliveryCompanyName(String deliveryCompanyName) { this.deliveryCompanyName = deliveryCompanyName; }

    public UUID getDeliveryDriverId() { return deliveryDriverId; }
    public void setDeliveryDriverId(UUID deliveryDriverId) { this.deliveryDriverId = deliveryDriverId; }

    public String getDeliveryDriverName() { return deliveryDriverName; }
    public void setDeliveryDriverName(String deliveryDriverName) { this.deliveryDriverName = deliveryDriverName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public OffsetDateTime getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
    public void setEstimatedDeliveryTime(OffsetDateTime estimatedDeliveryTime) { this.estimatedDeliveryTime = estimatedDeliveryTime; }

    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }

    public List<DeliveryBatchItemDTO> getItems() { return items; }
    public void setItems(List<DeliveryBatchItemDTO> items) { this.items = items; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Inner DTO for items within a batch
     */
    public static class DeliveryBatchItemDTO {
        @JsonProperty("itemId")
        private UUID itemId;

        @JsonProperty("itemDescription")
        private String itemDescription;

        @JsonProperty("quantity")
        private Integer quantity;

        @JsonProperty("estimatedValue")
        private BigDecimal itemValue;

        @JsonProperty("productId")
        private UUID productId;

        @JsonProperty("status")
        private String status;

        @JsonProperty("itemPhotos")
        private List<String> itemPhotos;

        public DeliveryBatchItemDTO() {}

        public DeliveryBatchItemDTO(UUID itemId, String itemDescription, Integer quantity, BigDecimal itemValue, UUID productId, String status) {
            this.itemId = itemId;
            this.itemDescription = itemDescription;
            this.quantity = quantity;
            this.itemValue = itemValue;
            this.productId = productId;
            this.status = status;
        }

        public DeliveryBatchItemDTO(UUID itemId, String itemDescription, Integer quantity, BigDecimal itemValue, UUID productId, String status, List<String> itemPhotos) {
            this.itemId = itemId;
            this.itemDescription = itemDescription;
            this.quantity = quantity;
            this.itemValue = itemValue;
            this.productId = productId;
            this.status = status;
            this.itemPhotos = itemPhotos;
        }

        // Getters and setters
        public UUID getItemId() { return itemId; }
        public void setItemId(UUID itemId) { this.itemId = itemId; }

        public String getItemDescription() { return itemDescription; }
        public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getItemValue() { return itemValue; }
        public void setItemValue(BigDecimal itemValue) { this.itemValue = itemValue; }

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public List<String> getItemPhotos() { return itemPhotos; }
        public void setItemPhotos(List<String> itemPhotos) { this.itemPhotos = itemPhotos; }
    }
}
