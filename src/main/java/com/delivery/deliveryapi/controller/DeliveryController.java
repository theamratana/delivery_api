package com.delivery.deliveryapi.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

import com.delivery.deliveryapi.dto.DeliveryBatchDTO;
import com.delivery.deliveryapi.dto.DeliveryItemDTO;
import com.delivery.deliveryapi.dto.ReceiverSuggestion;
import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.DeliveryItem;
import com.delivery.deliveryapi.model.DeliveryPhoto;
import com.delivery.deliveryapi.model.DeliveryStatus;
import com.delivery.deliveryapi.model.DeliveryTracking;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.DeliveryItemRepository;
import com.delivery.deliveryapi.repo.DeliveryPhotoRepository;
import com.delivery.deliveryapi.repo.DeliveryTrackingRepository;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.service.DeliveryService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final DeliveryItemRepository deliveryItemRepository;
    private final DeliveryTrackingRepository deliveryTrackingRepository;
    private final DeliveryPhotoRepository deliveryPhotoRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

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
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping
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

            // Create the delivery (service method is @Transactional)
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

        } catch (IllegalArgumentException e) {
            // Validation errors - return 400 with clear message
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new DeliveryResponse("Validation error: " + e.getMessage()));
        } catch (Exception e) {
            // Other errors - return 500 with message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DeliveryResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<DeliveryItemDTO>> getUserDeliveries() {
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
        List<DeliveryItemDTO> dtos = deliveries.stream()
            .map(DeliveryItemDTO::fromDeliveryItem)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryBatchDTO> getDelivery(@PathVariable UUID id) {
        // Find a delivery item with this ID to get batch context
        var optional = deliveryItemRepository.findById(id)
                .filter(d -> !d.isDeleted());
        
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        DeliveryItem item = optional.get();
        List<DeliveryItem> batchItems;

        // Prefer grouping by explicit batch id (created at POST time). Fall back to older
        // receiver + address grouping when batch id is not present (backwards compatible).
        if (item.getBatchId() != null) {
            java.util.UUID batchUuid = item.getBatchId();
            batchItems = deliveryItemRepository.findAll().stream()
                .filter(d -> !d.isDeleted())
                .filter(d -> batchUuid.equals(d.getBatchId()))
                .toList();
        } else {
            // legacy behavior: group by receiver + address + province + district
            batchItems = deliveryItemRepository.findAll().stream()
                .filter(d -> !d.isDeleted())
                .filter(d -> {
                    User dReceiver = d.getReceiver();
                    User itemReceiver = item.getReceiver();
                    return (dReceiver != null && itemReceiver != null && dReceiver.getId().equals(itemReceiver.getId())) 
                        && d.getDeliveryAddress().equals(item.getDeliveryAddress())
                        && d.getDeliveryProvince().equals(item.getDeliveryProvince())
                        && d.getDeliveryDistrict().equals(item.getDeliveryDistrict());
                })
                .toList();
        }
        
        if (batchItems.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Create batch from first item (all share same context)
        DeliveryItem first = batchItems.get(0);
        DeliveryBatchDTO batch = new DeliveryBatchDTO();
        batch.setBatchId(first.getBatchId() != null ? first.getBatchId().toString() : first.getId().toString());
        
        User receiver = first.getReceiver();
        if (receiver != null) {
            batch.setReceiverId(receiver.getId());
            batch.setReceiverName(receiver.getDisplayName());
            batch.setReceiverPhone(receiver.getPhoneE164());
        }
        
        batch.setDeliveryAddress(first.getDeliveryAddress());
        batch.setDeliveryProvince(first.getDeliveryProvince());
        batch.setDeliveryDistrict(first.getDeliveryDistrict());
        batch.setDeliveryFee(first.getDeliveryFee());
        
        Company company = first.getDeliveryCompany();
        if (company != null) {
            batch.setDeliveryCompanyId(company.getId());
            batch.setDeliveryCompanyName(company.getName());
        }
        
        User driver = first.getDeliveryDriver();
        if (driver != null) {
            batch.setDeliveryDriverId(driver.getId());
            batch.setDeliveryDriverName(driver.getDisplayName());
        }
        
        batch.setStatus(first.getStatus().toString());
                batch.setPaymentMethod(first.getPaymentMethod().toString().toLowerCase());
        batch.setEstimatedDeliveryTime(first.getEstimatedDeliveryTime());
        batch.setCreatedAt(first.getCreatedAt());
        batch.setUpdatedAt(first.getUpdatedAt());
        
        // Add all items to batch
        List<DeliveryBatchDTO.DeliveryBatchItemDTO> items = batchItems.stream()
            .map(d -> {
                // Fetch photos from delivery_photos table instead of parsing JSON field
                List<String> itemPhotos = deliveryPhotoRepository
                    .findByDeliveryItemIdAndDeletedFalseOrderBySequenceOrderAsc(d.getId())
                    .stream()
                    .map(DeliveryPhoto::getPhotoUrl)
                    .toList();
                
                return new DeliveryBatchDTO.DeliveryBatchItemDTO(
                    d.getId(),
                    d.getItemDescription(),
                    d.getQuantity() != null ? d.getQuantity() : 1,
                    d.getItemValue(),
                    d.getProduct() != null ? d.getProduct().getId() : null,
                    d.getStatus().toString(),
                    itemPhotos
                );
            })
            .toList();
        batch.setItems(items);
        batch.setItemCount(items.size());
        
        return ResponseEntity.ok(batch);
    }

    @GetMapping("/receiver-suggestions/{phone}")
    @Transactional
    public ResponseEntity<List<ReceiverSuggestion>> getReceiverSuggestions(@PathVariable String phone) {
        String normalizedPhone = phone.replaceAll("\\s+", "").trim();
        
        // Find all unique receivers with this phone number from delivery history
        List<DeliveryItem> deliveries = deliveryItemRepository.findByReceiverPhone(normalizedPhone);
        
        if (deliveries.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        
        // Create a map to track unique receivers and their most recent delivery data
        java.util.Map<UUID, ReceiverSuggestion> suggestions = new java.util.LinkedHashMap<>();
        
        for (DeliveryItem delivery : deliveries) {
            if (delivery.getReceiver() != null) {
                UUID receiverId = delivery.getReceiver().getId();
                if (!suggestions.containsKey(receiverId)) {
                    ReceiverSuggestion suggestion = new ReceiverSuggestion(
                        delivery.getReceiver().getId(),
                        delivery.getReceiver().getDisplayName(),
                        delivery.getReceiver().getPhoneE164(),
                        delivery.getDeliveryAddress(),
                        delivery.getDeliveryProvince(),
                        delivery.getDeliveryDistrict(),
                        delivery.getUpdatedAt()
                    );
                    suggestions.put(receiverId, suggestion);
                }
            }
        }
        
        return ResponseEntity.ok(new java.util.ArrayList<>(suggestions.values()));
    }

    @GetMapping("/batched")
    public ResponseEntity<List<DeliveryBatchDTO>> getUserDeliveriesBatched() {
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
        
        // Get all deliveries for the user
        List<DeliveryItem> deliveries = deliveryItemRepository.findByUserInvolved(currentUser);
        
        // Group by (receiver, deliveryAddress) to identify batches
        java.util.Map<String, DeliveryBatchDTO> batches = new java.util.LinkedHashMap<>();
        
        for (DeliveryItem item : deliveries) {
            // Prefer batch id grouping when present. Otherwise fall back to receiver/address grouping.
            String batchKey;
            if (item.getBatchId() != null) {
                batchKey = "batch:" + item.getBatchId().toString();
            } else {
                batchKey = String.format("legacy:%s|%s|%s|%s",
                    item.getReceiver() != null ? item.getReceiver().getId() : "unknown",
                    item.getDeliveryAddress() != null ? item.getDeliveryAddress() : "",
                    item.getDeliveryProvince() != null ? item.getDeliveryProvince() : "",
                    item.getDeliveryDistrict() != null ? item.getDeliveryDistrict() : ""
                );
            }

            if (!batches.containsKey(batchKey)) {
                // Create new batch
                DeliveryBatchDTO batch = new DeliveryBatchDTO();
                batch.setBatchId(item.getBatchId() != null ? item.getBatchId().toString() : item.getId().toString());

                if (item.getReceiver() != null) {
                    batch.setReceiverId(item.getReceiver().getId());
                    batch.setReceiverName(item.getReceiver().getDisplayName());
                    batch.setReceiverPhone(item.getReceiver().getPhoneE164());
                }

                batch.setDeliveryAddress(item.getDeliveryAddress());
                batch.setDeliveryProvince(item.getDeliveryProvince());
                batch.setDeliveryDistrict(item.getDeliveryDistrict());
                batch.setDeliveryFee(item.getDeliveryFee());
                batch.setStatus(item.getStatus().toString());
                batch.setPaymentMethod(item.getPaymentMethod() != null ? item.getPaymentMethod().getCode() : "COD");
                batch.setEstimatedDeliveryTime(item.getEstimatedDeliveryTime());
                batch.setCreatedAt(item.getCreatedAt());
                batch.setUpdatedAt(item.getUpdatedAt());

                if (item.getDeliveryCompany() != null) {
                    batch.setDeliveryCompanyId(item.getDeliveryCompany().getId());
                    batch.setDeliveryCompanyName(item.getDeliveryCompany().getName());
                }

                if (item.getDeliveryDriver() != null) {
                    batch.setDeliveryDriverId(item.getDeliveryDriver().getId());
                    batch.setDeliveryDriverName(item.getDeliveryDriver().getDisplayName());
                }

                batches.put(batchKey, batch);
            }

            // Add item to batch
            DeliveryBatchDTO batch = batches.get(batchKey);
            // Fetch photos from delivery_photos table instead of parsing JSON field
            List<String> itemPhotos = deliveryPhotoRepository
                .findByDeliveryItemIdAndDeletedFalseOrderBySequenceOrderAsc(item.getId())
                .stream()
                .map(DeliveryPhoto::getPhotoUrl)
                .toList();
            batch.getItems().add(new DeliveryBatchDTO.DeliveryBatchItemDTO(
                item.getId(),
                item.getItemDescription(),
                item.getQuantity(),
                item.getItemValue(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getStatus().toString(),
                itemPhotos
            ));
        }
        
        // Set item count for each batch
        batches.values().forEach(batch -> batch.setItemCount(batch.getItems().size()));
        
        return ResponseEntity.ok(new java.util.ArrayList<>(batches.values()));
    }

    @PostMapping("/{batchId}/items")
    @Transactional
    public ResponseEntity<?> appendItemsToBatch(@PathVariable String batchId, @RequestBody Object rawPayload) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            java.util.UUID userId;
            try {
                userId = java.util.UUID.fromString(userIdStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            var optUser = userRepository.findById(userId);
            if (optUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            User currentUser = optUser.get();

            // Normalize incoming payload: accept either a JSON array or an object with { items: [...] }
            List<DeliveryItemPayload> items;
            if (rawPayload instanceof List) {
                items = ((List<?>) rawPayload).stream()
                    .map(o -> objectMapper.convertValue(o, DeliveryItemPayload.class))
                    .toList();
            } else if (rawPayload instanceof java.util.Map) {
                Object inner = ((java.util.Map<?,?>) rawPayload).get("items");
                if (inner instanceof List) {
                    items = ((List<?>) inner).stream()
                        .map(o -> objectMapper.convertValue(o, DeliveryItemPayload.class))
                        .toList();
                } else {
                    return ResponseEntity.badRequest().body(new DeliveryResponse("Invalid payload: 'items' must be an array"));
                }
            } else {
                return ResponseEntity.badRequest().body(new DeliveryResponse("Invalid payload: expected array or object with 'items'"));
            }

            java.util.UUID bid = java.util.UUID.fromString(batchId);
            java.util.List<com.delivery.deliveryapi.model.DeliveryItem> created = deliveryService.appendItemsToBatch(currentUser, bid, items);

            // Create tracking entries for appended items
            for (com.delivery.deliveryapi.model.DeliveryItem it : created) {
                DeliveryTracking tracking = new DeliveryTracking(it, DeliveryStatus.CREATED, "Item appended to batch", currentUser);
                deliveryTrackingRepository.save(tracking);
            }

            // Build and return updated batch DTO
            java.util.List<com.delivery.deliveryapi.model.DeliveryItem> batchItems = deliveryItemRepository.findByBatchId(bid);
            if (batchItems == null || batchItems.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            com.delivery.deliveryapi.model.DeliveryItem first = batchItems.get(0);
            DeliveryBatchDTO batch = new DeliveryBatchDTO();
            batch.setBatchId(first.getBatchId() != null ? first.getBatchId().toString() : first.getId().toString());
            if (first.getReceiver() != null) {
                batch.setReceiverId(first.getReceiver().getId());
                batch.setReceiverName(first.getReceiver().getDisplayName());
                batch.setReceiverPhone(first.getReceiver().getPhoneE164());
            }
            batch.setDeliveryAddress(first.getDeliveryAddress());
            batch.setDeliveryProvince(first.getDeliveryProvince());
            batch.setDeliveryDistrict(first.getDeliveryDistrict());
            batch.setDeliveryFee(first.getDeliveryFee());
            batch.setStatus(first.getStatus().toString());
            batch.setPaymentMethod(first.getPaymentMethod() != null ? first.getPaymentMethod().getCode() : "COD");
            batch.setEstimatedDeliveryTime(first.getEstimatedDeliveryTime());
            batch.setCreatedAt(first.getCreatedAt());
            batch.setUpdatedAt(first.getUpdatedAt());

            if (first.getDeliveryCompany() != null) {
                batch.setDeliveryCompanyId(first.getDeliveryCompany().getId());
                batch.setDeliveryCompanyName(first.getDeliveryCompany().getName());
            }
            if (first.getDeliveryDriver() != null) {
                batch.setDeliveryDriverId(first.getDeliveryDriver().getId());
                batch.setDeliveryDriverName(first.getDeliveryDriver().getDisplayName());
            }

            java.util.List<DeliveryBatchDTO.DeliveryBatchItemDTO> dtoItems = batchItems.stream()
                .map(d -> {
                    // Fetch photos from delivery_photos table instead of parsing JSON field
                    List<String> itemPhotos = deliveryPhotoRepository
                        .findByDeliveryItemIdAndDeletedFalseOrderBySequenceOrderAsc(d.getId())
                        .stream()
                        .map(DeliveryPhoto::getPhotoUrl)
                        .toList();
                    return new DeliveryBatchDTO.DeliveryBatchItemDTO(
                        d.getId(), d.getItemDescription(), d.getQuantity(), d.getItemValue(), d.getProduct() != null ? d.getProduct().getId() : null, d.getStatus().toString(), itemPhotos);
                })
                .toList();
            batch.setItems(dtoItems);
            batch.setItemCount(dtoItems.size());

            return ResponseEntity.ok(batch);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DeliveryResponse("Error: " + e.getMessage()));
        }
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

        @JsonProperty("paymentMethod")
        private String paymentMethod = "COD"; // "PAID" or "COD"

        @JsonProperty("items")
        private List<DeliveryItemPayload> items; // Array of delivery items (required)

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
        private BigDecimal deliveryFee; // ONE fee for entire delivery (all items share same fee)

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

        public String getSpecialInstructions() { return specialInstructions; }
        public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

        public BigDecimal getDeliveryFee() { return deliveryFee; }
        public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod != null ? paymentMethod : "COD"; }

        public List<DeliveryItemPayload> getItems() { return items; }
        public void setItems(List<DeliveryItemPayload> items) { this.items = items; }
    }

    /**
     * Payload for individual delivery item when creating multi-product deliveries
     */
    public static class DeliveryItemPayload {
        @JsonProperty("productName")
        private String productName; // Required: product catalog name

        @JsonProperty("itemDescription")
        private String itemDescription; // Detailed description of this delivery item

        @JsonProperty("itemPhotos")
        private List<String> itemPhotos;

        @JsonProperty("estimatedValue")
        private BigDecimal estimatedValue;

        @JsonProperty("quantity")
        private Integer quantity = 1; // Default quantity is 1

        @JsonProperty("paymentMethod")
        private String paymentMethod = "COD";

        @JsonProperty("productId")
        private UUID productId; // Optional: if provided, will verify it exists

        // Constructor
        public DeliveryItemPayload() {}

        // Getters and setters
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getItemDescription() { return itemDescription; }
        public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

        public List<String> getItemPhotos() { return itemPhotos; }
        public void setItemPhotos(List<String> itemPhotos) { this.itemPhotos = itemPhotos; }

        public BigDecimal getEstimatedValue() { return estimatedValue; }
        public void setEstimatedValue(BigDecimal estimatedValue) { this.estimatedValue = estimatedValue; }

        public Integer getQuantity() { return quantity != null ? quantity : 1; }
        public void setQuantity(Integer quantity) { this.quantity = quantity != null && quantity > 0 ? quantity : 1; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod != null ? paymentMethod : "COD"; }

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
    }

    public static class DeliveryResponse {
        @JsonProperty("deliveryId")
        private UUID deliveryId;

        @JsonProperty("batchId")
        private UUID batchId;

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
            this.batchId = delivery.getBatchId();
        }

        public DeliveryResponse(String error) {
            this.error = error;
        }

        // Getters and setters
        public UUID getDeliveryId() { return deliveryId; }
        public void setDeliveryId(UUID deliveryId) { this.deliveryId = deliveryId; }

        public UUID getBatchId() { return batchId; }
        public void setBatchId(UUID batchId) { this.batchId = batchId; }

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