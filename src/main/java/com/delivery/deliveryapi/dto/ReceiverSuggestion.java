package com.delivery.deliveryapi.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for receiver suggestion in delivery auto-recognition
 * Shows previous delivery recipients that can be quickly selected
 */
public class ReceiverSuggestion {

    @JsonProperty("receiverId")
    private UUID receiverId;

    @JsonProperty("receiverName")
    private String receiverName;

    @JsonProperty("receiverPhone")
    private String receiverPhone;

    @JsonProperty("lastDeliveryAddress")
    private String lastDeliveryAddress;

    @JsonProperty("lastDeliveryProvince")
    private String lastDeliveryProvince;

    @JsonProperty("lastDeliveryDistrict")
    private String lastDeliveryDistrict;

    @JsonProperty("lastUsedAt")
    private OffsetDateTime lastUsedAt;

    // Constructor
    public ReceiverSuggestion(UUID receiverId, String receiverName, String receiverPhone,
                             String lastDeliveryAddress, String lastDeliveryProvince, 
                             String lastDeliveryDistrict, OffsetDateTime lastUsedAt) {
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.lastDeliveryAddress = lastDeliveryAddress;
        this.lastDeliveryProvince = lastDeliveryProvince;
        this.lastDeliveryDistrict = lastDeliveryDistrict;
        this.lastUsedAt = lastUsedAt;
    }

    // Getters and setters
    public UUID getReceiverId() { return receiverId; }
    public void setReceiverId(UUID receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public String getLastDeliveryAddress() { return lastDeliveryAddress; }
    public void setLastDeliveryAddress(String lastDeliveryAddress) { this.lastDeliveryAddress = lastDeliveryAddress; }

    public String getLastDeliveryProvince() { return lastDeliveryProvince; }
    public void setLastDeliveryProvince(String lastDeliveryProvince) { this.lastDeliveryProvince = lastDeliveryProvince; }

    public String getLastDeliveryDistrict() { return lastDeliveryDistrict; }
    public void setLastDeliveryDistrict(String lastDeliveryDistrict) { this.lastDeliveryDistrict = lastDeliveryDistrict; }

    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(OffsetDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
