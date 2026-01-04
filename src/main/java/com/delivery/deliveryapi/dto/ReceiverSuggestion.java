package com.delivery.deliveryapi.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receiver suggestion in delivery auto-recognition
 * Shows previous delivery recipients that can be quickly selected
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
