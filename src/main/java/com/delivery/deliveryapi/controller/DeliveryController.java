package com.delivery.deliveryapi.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.model.DeliveryItem;
import com.delivery.deliveryapi.model.DeliveryStatus;
import com.delivery.deliveryapi.model.DeliveryTracking;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.DeliveryItemRepository;
import com.delivery.deliveryapi.repo.DeliveryPhotoRepository;
import com.delivery.deliveryapi.repo.DeliveryTrackingRepository;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.service.DeliveryService;
import com.fasterxml.jackson.annotation.JsonProperty;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final DeliveryItemRepository deliveryItemRepository;
    private final DeliveryTrackingRepository deliveryTrackingRepository;
    private final DeliveryPhotoRepository deliveryPhotoRepository;
    private final UserRepository userRepository;

    public DeliveryController(DeliveryService deliveryService,
                            DeliveryItemRepository deliveryItemRepository,
                            DeliveryTrackingRepository deliveryTrackingRepository,
                            DeliveryPhotoRepository deliveryPhotoRepository,
                            UserRepository userRepository) {
        this.deliveryService = deliveryService;
        this.deliveryItemRepository = deliveryItemRepository;
        this.deliveryTrackingRepository = deliveryTrackingRepository;
        this.deliveryPhotoRepository = deliveryPhotoRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<DeliveryResponse> createDelivery(@RequestBody CreateDeliveryRequest request) {
        try {
            // Get current user from security context
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            UUID userId;
            try {
                userId = UUID.fromString(userIdStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            var optUser = userRepository.findById(userId);
            if (optUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            User currentUser = optUser.get();

            // Create the delivery
            DeliveryItem delivery = deliveryService.createDelivery(currentUser, request);

            // Create initial tracking entry
            DeliveryTracking initialTracking = new DeliveryTracking(
                delivery,
                DeliveryStatus.CREATED,
                "Delivery created successfully",
                currentUser
            );
            deliveryTrackingRepository.save(initialTracking);

            // Return response
            DeliveryResponse response = new DeliveryResponse(delivery);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new DeliveryResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<DeliveryItem>> getUserDeliveries() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User currentUser = optUser.get();
        List<DeliveryItem> deliveries = deliveryItemRepository.findByUserInvolved(currentUser);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryItem> getDelivery(@PathVariable UUID id) {
        return deliveryItemRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Request/Response DTOs
    public static class CreateDeliveryRequest {
        @JsonProperty("receiverPhone")
        private String receiverPhone;

        @JsonProperty("receiverName")
        private String receiverName;

        @JsonProperty("deliveryType")
        private String deliveryType; // "COMPANY" or "DRIVER"

        @JsonProperty("companyName")
        private String companyName;

        @JsonProperty("companyPhone")
        private String companyPhone;

        @JsonProperty("driverPhone")
        private String driverPhone;

        @JsonProperty("itemDescription")
        private String itemDescription;

        @JsonProperty("itemPhotos")
        private List<String> itemPhotos;

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

        @JsonProperty("deliveryFee")
        private BigDecimal deliveryFee;

        @JsonProperty("deliveryFeeModel")
        private String deliveryFeeModel;

        @JsonProperty("estimatedValue")
        private BigDecimal estimatedValue;

        @JsonProperty("specialInstructions")
        private String specialInstructions;

        // Getters and setters
        public String getReceiverPhone() { return receiverPhone; }
        public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

        public String getReceiverName() { return receiverName; }
        public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

        public String getDeliveryType() { return deliveryType; }
        public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }

        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }

        public String getCompanyPhone() { return companyPhone; }
        public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }

        public String getDriverPhone() { return driverPhone; }
        public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }

        public String getItemDescription() { return itemDescription; }
        public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

        public List<String> getItemPhotos() { return itemPhotos; }
        public void setItemPhotos(List<String> itemPhotos) { this.itemPhotos = itemPhotos; }

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

        public BigDecimal getDeliveryFee() { return deliveryFee; }
        public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

        public String getDeliveryFeeModel() { return deliveryFeeModel; }
        public void setDeliveryFeeModel(String deliveryFeeModel) { this.deliveryFeeModel = deliveryFeeModel; }

        public BigDecimal getEstimatedValue() { return estimatedValue; }
        public void setEstimatedValue(BigDecimal estimatedValue) { this.estimatedValue = estimatedValue; }

        public String getSpecialInstructions() { return specialInstructions; }
        public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
    }

    public static class DeliveryResponse {
        @JsonProperty("deliveryId")
        private UUID deliveryId;

        @JsonProperty("status")
        private String status;

        @JsonProperty("trackingCode")
        private String trackingCode;

        @JsonProperty("estimatedDelivery")
        private String estimatedDelivery;

        @JsonProperty("error")
        private String error;

        public DeliveryResponse(DeliveryItem delivery) {
            this.deliveryId = delivery.getId();
            this.status = delivery.getStatus().toString();
            this.trackingCode = "DEL" + delivery.getId().toString().substring(0, 8).toUpperCase();
            this.estimatedDelivery = delivery.getEstimatedDeliveryTime() != null ?
                    delivery.getEstimatedDeliveryTime().toString() : null;
        }

        public DeliveryResponse(String error) {
            this.error = error;
        }

        // Getters and setters
        public UUID getDeliveryId() { return deliveryId; }
        public void setDeliveryId(UUID deliveryId) { this.deliveryId = deliveryId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getTrackingCode() { return trackingCode; }
        public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }

        public String getEstimatedDelivery() { return estimatedDelivery; }
        public void setEstimatedDelivery(String estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}