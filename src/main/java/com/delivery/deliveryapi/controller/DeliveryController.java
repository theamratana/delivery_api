package com.delivery.deliveryapi.controller;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.dto.DeliveryBatchDTO;
import com.delivery.deliveryapi.dto.ReceiverSuggestion;
import com.delivery.deliveryapi.dto.StatusDisplayDTO;
import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.DeliveryItem;
import com.delivery.deliveryapi.model.DeliveryPhoto;
import com.delivery.deliveryapi.model.DeliveryStatus;
import com.delivery.deliveryapi.model.DeliveryTracking;
import com.delivery.deliveryapi.model.District;
import com.delivery.deliveryapi.model.Province;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserType;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeliveryController.class);

    private final DeliveryService deliveryService;
    private final DeliveryItemRepository deliveryItemRepository;
    private final DeliveryTrackingRepository deliveryTrackingRepository;
    private final DeliveryPhotoRepository deliveryPhotoRepository;
    private final UserRepository userRepository;
    private final com.delivery.deliveryapi.repo.DistrictRepository districtRepository;
    private final com.delivery.deliveryapi.repo.ProvinceRepository provinceRepository;
    private final ObjectMapper objectMapper;

    public DeliveryController(DeliveryService deliveryService,
                            DeliveryItemRepository deliveryItemRepository,
                            DeliveryTrackingRepository deliveryTrackingRepository,
                            DeliveryPhotoRepository deliveryPhotoRepository,
                            UserRepository userRepository,
                            com.delivery.deliveryapi.repo.DistrictRepository districtRepository,
                            com.delivery.deliveryapi.repo.ProvinceRepository provinceRepository) {
        this.deliveryService = deliveryService;
        this.deliveryItemRepository = deliveryItemRepository;
        this.deliveryTrackingRepository = deliveryTrackingRepository;
        this.deliveryPhotoRepository = deliveryPhotoRepository;
        this.userRepository = userRepository;
        this.districtRepository = districtRepository;
        this.provinceRepository = provinceRepository;
        this.objectMapper = new ObjectMapper();
    }

    public static class SummaryRequest {
        @JsonProperty("startDate")
        private String startDate; // ISO8601 date or datetime

        @JsonProperty("endDate")
        private String endDate; // ISO8601 date or datetime

        // getters & setters
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
    }

    @PostMapping("/summary")
    public ResponseEntity<?> getDeliverySummaryByStatus(@RequestBody SummaryRequest req) {
        // Auth
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId;
        try { userId = UUID.fromString(userIdStr); } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // Parse dates with some reasonable defaults
        OffsetDateTime end = null;
        OffsetDateTime start = null;
        try {
            if (req.getEndDate() != null && !req.getEndDate().isBlank()) {
                try { end = OffsetDateTime.parse(req.getEndDate()); }
                catch (DateTimeException dt) { // maybe date-only
                    end = LocalDate.parse(req.getEndDate()).atTime(23,59,59).atOffset(OffsetDateTime.now().getOffset());
                }
            }
            if (req.getStartDate() != null && !req.getStartDate().isBlank()) {
                try { start = OffsetDateTime.parse(req.getStartDate()); }
                catch (DateTimeException dt) { start = LocalDate.parse(req.getStartDate()).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()); }
            }
        } catch (DateTimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date format. Use ISO8601 date or datetime"));
        }

        if (end == null) end = OffsetDateTime.now();
        if (start == null) start = end.minusDays(30); // default to last 30 days

        // Query repo for counts grouped by status
        List<Object[]> rows = deliveryItemRepository.countStatusByUserInRange(userId, start, end);

        // Build result map with all statuses present and defaults to 0
        Map<String, Long> result = new LinkedHashMap<>();
        for (DeliveryStatus s : DeliveryStatus.values()) {
            result.put(s.toString(), 0L);
        }

        for (Object[] row : rows) {
            if (row == null || row.length < 2) continue;
            DeliveryStatus status = (DeliveryStatus) row[0];
            Number count = (Number) row[1];
            result.put(status.toString(), count != null ? count.longValue() : 0L);
        }

        // Also produce grouped counts by responsible party (Sender, DeliveryCompany, Receiver)
        Map<String, Map<String, Long>> groups = new LinkedHashMap<>();
        groups.put("Sender", new LinkedHashMap<>());
        groups.put("DeliveryCompany", new LinkedHashMap<>());
        groups.put("Receiver", new LinkedHashMap<>());

        // Map statuses into groups and populate
        for (Map.Entry<String, Long> e : result.entrySet()) {
            DeliveryStatus sEnum = DeliveryStatus.valueOf(e.getKey());
            String group = statusGroupFor(sEnum);
            Map<String, Long> groupMap = groups.get(group);
            if (groupMap == null) groupMap = new LinkedHashMap<>();
            groupMap.put(e.getKey(), e.getValue());
            groups.put(group, groupMap);
        }

        // Build final response with both the detailed counts and grouped view
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("counts", result);
        response.put("groups", groups);
        // Also include a small mapping to help UIs show which group a status belongs to
        Map<String, String> statusToGroup = new LinkedHashMap<>();
        for (DeliveryStatus s : DeliveryStatus.values()) statusToGroup.put(s.toString(), statusGroupFor(s));
        response.put("statusToGroup", statusToGroup);

        return ResponseEntity.ok(response);
    }

    /**
     * Get the responsible party for a delivery status (for UI grouping by responsibility)
     * This is different from StatusGroup which groups by delivery lifecycle stage
     */
    private static String statusGroupFor(DeliveryStatus status) {
        return switch (status) {
            case CREATED, DROPPED_OFF, CANCELLED, FAILED, FAILED_DELIVERY -> "Sender";
            case ASSIGNED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, RETURNED -> "DeliveryCompany";
            case DELIVERED -> "Receiver";
            default -> "Sender";
        };
    }

    @PostMapping("/verify-customer")
    public ResponseEntity<CustomerVerificationResponse> verifyCustomer(@RequestBody VerifyCustomerRequest request) {
        try {
            // Get current user
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            UUID userId = UUID.fromString(userIdStr);
            var optUser = userRepository.findById(userId);
            if (optUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            User currentUser = optUser.get();
            
            if (currentUser.getCompany() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Search for existing customer by phone + company
            var existingCustomer = userRepository.findByPhoneE164AndCompanyAndUserType(
                request.receiverPhone.trim(),
                currentUser.getCompany(),
                UserType.CUSTOMER
            );

            if (existingCustomer.isEmpty()) {
                // New customer
                return ResponseEntity.ok(new CustomerVerificationResponse(false, null, false, null, null));
            }

            User customer = existingCustomer.get();
            CustomerInfo currentInfo = new CustomerInfo(
                customer.getDisplayName(),
                customer.getPhoneE164(),
                customer.getDefaultAddress(),
                customer.getDefaultProvinceId(),
                customer.getDefaultDistrictId(),
                customer.getDefaultProvinceId() != null ? 
                    provinceRepository.findById(customer.getDefaultProvinceId()).map(Province::getName).orElse(null) : null,
                customer.getDefaultDistrictId() != null ? 
                    districtRepository.findById(customer.getDefaultDistrictId()).map(District::getName).orElse(null) : null
            );

            // Check for changes
            Map<String, FieldChange> changes = new LinkedHashMap<>();
            boolean hasChanges = false;

            // Compare name
            if (request.receiverName != null && !request.receiverName.trim().isEmpty()) {
                if (customer.getDisplayName() == null || !request.receiverName.trim().equals(customer.getDisplayName())) {
                    changes.put("name", new FieldChange(customer.getDisplayName(), request.receiverName.trim()));
                    hasChanges = true;
                }
            }

            // Compare address
            if (request.deliveryAddress != null && !request.deliveryAddress.trim().isEmpty()) {
                if (customer.getDefaultAddress() == null || !request.deliveryAddress.trim().equals(customer.getDefaultAddress())) {
                    changes.put("address", new FieldChange(customer.getDefaultAddress(), request.deliveryAddress.trim()));
                    hasChanges = true;
                }
            }

            // Compare province (by ID)
            if (request.deliveryProvinceId != null) {
                if (customer.getDefaultProvinceId() == null || !request.deliveryProvinceId.equals(customer.getDefaultProvinceId())) {
                    String oldProvinceName = customer.getDefaultProvinceId() != null ?
                        provinceRepository.findById(customer.getDefaultProvinceId()).map(Province::getName).orElse(null) : null;
                    String newProvinceName = provinceRepository.findById(request.deliveryProvinceId).map(Province::getName).orElse(null);
                    changes.put("province", new FieldChange(oldProvinceName, newProvinceName));
                    hasChanges = true;
                }
            }

            // Compare district (by ID)
            if (request.deliveryDistrictId != null) {
                if (customer.getDefaultDistrictId() == null || !request.deliveryDistrictId.equals(customer.getDefaultDistrictId())) {
                    String oldDistrictName = customer.getDefaultDistrictId() != null ?
                        districtRepository.findById(customer.getDefaultDistrictId()).map(District::getName).orElse(null) : null;
                    String newDistrictName = districtRepository.findById(request.deliveryDistrictId).map(District::getName).orElse(null);
                    changes.put("district", new FieldChange(oldDistrictName, newDistrictName));
                    hasChanges = true;
                }
            }

            return ResponseEntity.ok(new CustomerVerificationResponse(
                true,
                customer.getId().toString(),
                hasChanges,
                currentInfo,
                hasChanges ? changes : null
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
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

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<DeliveryBatchDTO> getDeliveryBatch(@PathVariable UUID batchId) {
        // Find all items in this batch
        List<DeliveryItem> batchItems = deliveryItemRepository.findByBatchId(batchId);
        if (batchItems == null || batchItems.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Create batch from first item (all share same context)
        DeliveryItem first = batchItems.get(0);
        DeliveryBatchDTO batch = new DeliveryBatchDTO();
        batch.setBatchId(first.getBatchId().toString());
        
        User receiver = first.getReceiver();
        if (receiver != null) {
            batch.setReceiverId(receiver.getId());
            batch.setReceiverName(receiver.getDisplayName());
            batch.setReceiverPhone(receiver.getPhoneE164());
        }
        
        // Set sender information from denormalized fields
        batch.setSenderName(first.getSenderName());
        batch.setSenderPhone(first.getSenderPhone());
        
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
        batch.setStatusDisplay(StatusDisplayDTO.fromDeliveryStatus(first.getStatus()));
        batch.setPaymentMethod(first.getPaymentMethod().toString().toLowerCase());
        batch.setEstimatedDeliveryTime(first.getEstimatedDeliveryTime());
        batch.setCreatedAt(first.getCreatedAt());
        batch.setUpdatedAt(first.getUpdatedAt());
        
        // Add all items to batch
        List<DeliveryBatchDTO.DeliveryBatchItemDTO> items = batchItems.stream()
            .filter(d -> !d.isDeleted())
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
                    itemPhotos,
                    d.getLastStatusNote(),
                    d.getItemDiscount()
                );
            })
            .toList();
        batch.setItems(items);
        batch.setItemCount(items.size());
        
        // Fetch batch-level delivery photos (package photos with sequenceOrder >= 1000)
        List<String> deliveryPhotos = deliveryPhotoRepository
            .findByDeliveryItemIdAndDeletedFalseOrderBySequenceOrderAsc(first.getId())
            .stream()
            .filter(p -> p.getSequenceOrder() != null && p.getSequenceOrder() >= 1000)
            .map(DeliveryPhoto::getPhotoUrl)
            .toList();
        batch.setDeliveryPhotos(deliveryPhotos);
        
        // Calculate totals
        calculateBatchTotals(batch, batchItems);
        
        return ResponseEntity.ok(batch);
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
        
        // Set sender information from denormalized fields
        batch.setSenderName(first.getSenderName());
        batch.setSenderPhone(first.getSenderPhone());
        
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
        batch.setStatusDisplay(StatusDisplayDTO.fromDeliveryStatus(first.getStatus()));
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
                    itemPhotos,
                    d.getLastStatusNote(),
                    d.getItemDiscount()
                );
            })
            .toList();
        batch.setItems(items);
        batch.setItemCount(items.size());
        
        // Fetch batch-level delivery photos
        List<String> deliveryPhotos = deliveryPhotoRepository
            .findByDeliveryItemIdAndDeletedFalseOrderBySequenceOrderAsc(first.getId())
            .stream()
            .filter(p -> p.getSequenceOrder() != null && p.getSequenceOrder() >= 1000)
            .map(DeliveryPhoto::getPhotoUrl)
            .toList();
        batch.setDeliveryPhotos(deliveryPhotos);
        
        // Calculate totals
        calculateBatchTotals(batch, batchItems);
        
        return ResponseEntity.ok(batch);
    }

    @GetMapping("/receiver-suggestions/{phone}")
    @Transactional
    public ResponseEntity<List<ReceiverSuggestion>> getReceiverSuggestions(@PathVariable String phone) {
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

        User currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getCompany() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(List.of());
        }

        String normalizedPhone = phone.replaceAll("\\s+", "").trim();
        
        // Find deliveries from same company's delivery history only
        List<DeliveryItem> deliveries = deliveryItemRepository
            .findByReceiverPhoneAndSenderCompany(normalizedPhone, currentUser.getCompany().getId());
        
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

                // Set sender information from denormalized fields
                batch.setSenderName(item.getSenderName());
                batch.setSenderPhone(item.getSenderPhone());

                batch.setDeliveryAddress(item.getDeliveryAddress());
                batch.setDeliveryProvince(item.getDeliveryProvince());
                batch.setDeliveryDistrict(item.getDeliveryDistrict());
                batch.setDeliveryFee(item.getDeliveryFee());
                
                // Pricing and currency fields
                batch.setCurrency(item.getCurrency());
                batch.setDeliveryDiscount(item.getDeliveryDiscount());
                batch.setOrderDiscount(item.getOrderDiscount());
                batch.setActualDeliveryCost(item.getActualDeliveryCost());
                batch.setKhrAmount(item.getKhrAmount());
                batch.setExchangeRateUsed(item.getExchangeRateUsed());
                
                batch.setStatus(item.getStatus().toString());
                batch.setStatusDisplay(StatusDisplayDTO.fromDeliveryStatus(item.getStatus()));
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
                itemPhotos,
                item.getLastStatusNote(),
                item.getItemDiscount()
            ));
        }
        
        // Calculate totals for each batch
        batches.forEach((key, batch) -> {
            List<DeliveryItem> batchItems = deliveries.stream()
                .filter(d -> {
                    if (d.getBatchId() != null) {
                        return ("batch:" + d.getBatchId().toString()).equals(key);
                    } else {
                        String legacyKey = String.format("legacy:%s|%s|%s|%s",
                            d.getReceiver() != null ? d.getReceiver().getId() : "unknown",
                            d.getDeliveryAddress() != null ? d.getDeliveryAddress() : "",
                            d.getDeliveryProvince() != null ? d.getDeliveryProvince() : "",
                            d.getDeliveryDistrict() != null ? d.getDeliveryDistrict() : ""
                        );
                        return legacyKey.equals(key);
                    }
                })
                .toList();
            calculateBatchTotals(batch, batchItems);
            
            // Fetch delivery photos for this batch
            if (!batchItems.isEmpty()) {
                DeliveryItem firstItem = batchItems.get(0);
                List<String> deliveryPhotos = deliveryPhotoRepository
                    .findByDeliveryItemIdAndDeletedFalseOrderBySequenceOrderAsc(firstItem.getId())
                    .stream()
                    .filter(p -> p.getSequenceOrder() != null && p.getSequenceOrder() >= 1000)
                    .map(DeliveryPhoto::getPhotoUrl)
                    .toList();
                batch.setDeliveryPhotos(deliveryPhotos);
            }
        });
        
        // Set item count for each batch
        batches.values().forEach(batch -> batch.setItemCount(batch.getItems().size()));
        
        return ResponseEntity.ok(new java.util.ArrayList<>(batches.values()));
    }

    @PutMapping("/batch/{batchId}")
    @Transactional
    public ResponseEntity<?> updateDeliveryBatch(@PathVariable String batchId, @RequestBody UpdateDeliveryRequest request) {
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

            java.util.UUID bid = java.util.UUID.fromString(batchId);
            java.util.List<com.delivery.deliveryapi.model.DeliveryItem> updated = deliveryService.updateDeliveryBatch(currentUser, bid, request);

            if (updated == null || updated.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DeliveryResponse("Delivery batch not found or unauthorized"));
            }

            // Build response
            var first = updated.get(0);
            var batch = new DeliveryBatchDTO();
            batch.setBatchId(first.getBatchId().toString());
            batch.setReceiverId(first.getReceiver() != null ? first.getReceiver().getId() : null);
            // Use displayName instead of getFullName() since we update displayName in findOrCreateReceiver
            if (first.getReceiver() != null) {
                userRepository.flush(); // Ensure any pending updates are written to DB
                var freshReceiverOpt = userRepository.findById(first.getReceiver().getId());
                if (freshReceiverOpt.isPresent()) {
                    var freshReceiver = freshReceiverOpt.get();
                    // displayName is what we update, not firstName/lastName
                    batch.setReceiverName(freshReceiver.getDisplayName() != null ? freshReceiver.getDisplayName() : freshReceiver.getFullName());
                    batch.setReceiverPhone(freshReceiver.getPhoneE164());
                } else {
                    batch.setReceiverName(first.getReceiver().getDisplayName() != null ? first.getReceiver().getDisplayName() : first.getReceiver().getFullName());
                    batch.setReceiverPhone(first.getReceiver().getPhoneE164());
                }
            } else {
                batch.setReceiverName(null);
                batch.setReceiverPhone(null);
            }
            batch.setSenderName(first.getSenderName());
            batch.setSenderPhone(first.getSenderPhone());
            batch.setDeliveryAddress(first.getDeliveryAddress());
            batch.setDeliveryProvince(first.getDeliveryProvince());
            batch.setDeliveryDistrict(first.getDeliveryDistrict());
            batch.setDeliveryFee(first.getDeliveryFee());
            batch.setCurrency(first.getCurrency());
            batch.setDeliveryDiscount(first.getDeliveryDiscount());
            batch.setOrderDiscount(first.getOrderDiscount());
            batch.setSubTotal(first.getSubTotal());
            batch.setGrandTotal(first.getGrandTotal());
            batch.setActualDeliveryCost(first.getActualDeliveryCost());
            batch.setKhrAmount(first.getKhrAmount());
            batch.setExchangeRateUsed(first.getExchangeRateUsed());
            batch.setDeliveryCompanyId(first.getDeliveryCompany() != null ? first.getDeliveryCompany().getId() : null);
            batch.setDeliveryCompanyName(first.getDeliveryCompany() != null ? first.getDeliveryCompany().getName() : null);
            batch.setDeliveryDriverId(first.getDeliveryDriver() != null ? first.getDeliveryDriver().getId() : null);
            batch.setDeliveryDriverName(first.getDeliveryDriver() != null ? first.getDeliveryDriver().getFullName() : null);
            batch.setStatus(first.getStatus().name());
            batch.setStatusDisplay(StatusDisplayDTO.fromDeliveryStatus(first.getStatus()));
            batch.setPaymentMethod(first.getPaymentMethod().name().toLowerCase());
            batch.setEstimatedDeliveryTime(first.getEstimatedDeliveryTime());
            batch.setCreatedAt(first.getCreatedAt());
            batch.setUpdatedAt(first.getUpdatedAt());

            // Get delivery photos from first item
            var photos = deliveryPhotoRepository.findByDeliveryItemIdOrderBySequenceOrderAsc(first.getId());
            batch.setDeliveryPhotos(photos.stream().map(DeliveryPhoto::getPhotoUrl).toList());

            // Build items
            var items = updated.stream().map(item -> {
                var dto = new DeliveryBatchDTO.DeliveryBatchItemDTO();
                dto.setItemId(item.getId());
                dto.setItemDescription(item.getItemDescription());
                dto.setQuantity(item.getQuantity());
                dto.setItemValue(item.getItemValue());
                dto.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
                dto.setStatus(item.getStatus().name());
                dto.setLastStatusNote(item.getLastStatusNote());
                
                // Item photos
                if (item.getPhotoUrls() != null && !item.getPhotoUrls().isBlank()) {
                    try {
                        var arr = objectMapper.readValue(item.getPhotoUrls(), java.util.List.class);
                        dto.setItemPhotos(arr);
                    } catch (Exception ignored) {
                        dto.setItemPhotos(java.util.List.of());
                    }
                } else {
                    dto.setItemPhotos(java.util.List.of());
                }
                dto.setItemDiscount(item.getItemDiscount());
                return dto;
            }).toList();

            batch.setItems(items);
            batch.setItemCount(items.size());

            return ResponseEntity.ok(batch);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new DeliveryResponse("Invalid batch ID"));
        } catch (Exception e) {
            log.error("Error updating delivery batch", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new DeliveryResponse("Error updating delivery: " + e.getMessage()));
        }
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
            
            // Set sender information from denormalized fields
            batch.setSenderName(first.getSenderName());
            batch.setSenderPhone(first.getSenderPhone());
            
            batch.setDeliveryAddress(first.getDeliveryAddress());
            batch.setDeliveryProvince(first.getDeliveryProvince());
            batch.setDeliveryDistrict(first.getDeliveryDistrict());
            batch.setDeliveryFee(first.getDeliveryFee());
            
            batch.setStatus(first.getStatus().toString());
            batch.setStatusDisplay(StatusDisplayDTO.fromDeliveryStatus(first.getStatus()));
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
                        d.getId(), d.getItemDescription(), d.getQuantity(), d.getItemValue(), d.getProduct() != null ? d.getProduct().getId() : null, d.getStatus().toString(), itemPhotos, d.getLastStatusNote(), d.getItemDiscount());
                })
                .toList();
            batch.setItems(dtoItems);
            batch.setItemCount(dtoItems.size());
            
            // Fetch delivery photos
            List<String> deliveryPhotos = deliveryPhotoRepository
                .findByDeliveryItemIdAndDeletedFalseOrderBySequenceOrderAsc(first.getId())
                .stream()
                .filter(p -> p.getSequenceOrder() != null && p.getSequenceOrder() >= 1000)
                .map(DeliveryPhoto::getPhotoUrl)
                .toList();
            batch.setDeliveryPhotos(deliveryPhotos);
            
            // Calculate totals
            calculateBatchTotals(batch, batchItems);

            return ResponseEntity.ok(batch);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DeliveryResponse("Error: " + e.getMessage()));
        }
    }

    public static class UpdateStatusRequest {
        @JsonProperty("status")
        private String status;

        @JsonProperty("note")
        private String note;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> changeDeliveryStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequest req) {
        // Authentication
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId;
        try { userId = UUID.fromString(userIdStr); } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User currentUser = optUser.get();

        var optItem = deliveryItemRepository.findById(id).filter(d -> !d.isDeleted());
        if (optItem.isEmpty()) return ResponseEntity.notFound().build();
        DeliveryItem item = optItem.get();

        // Authorization: user must be involved or company owner/manager or system admin
        boolean allowed = false;
        if (item.getSender() != null && item.getSender().getId().equals(currentUser.getId())) allowed = true;
        if (item.getReceiver() != null && item.getReceiver().getId().equals(currentUser.getId())) allowed = true;
        if (item.getDeliveryDriver() != null && item.getDeliveryDriver().getId().equals(currentUser.getId())) allowed = true;
        if (!allowed && item.getDeliveryCompany() != null && currentUser.getCompany() != null && item.getDeliveryCompany().getId().equals(currentUser.getCompany().getId())) {
            // company members: only OWNER or MANAGER allowed to update package/delivery status for company deliveries
            if (currentUser.getUserRole() != null && (currentUser.getUserRole().name().equals("OWNER") || currentUser.getUserRole().name().equals("MANAGER") || currentUser.getUserRole().name().equals("SYSTEM_ADMINISTRATOR"))) {
                allowed = true;
            }
        }

        if (!allowed) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions to change delivery status"));
        }

        // Parse and validate status
        if (req == null || req.getStatus() == null || req.getStatus().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }

        DeliveryStatus newStatus;
        try {
            newStatus = DeliveryStatus.valueOf(req.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value"));
        }

        // Update status on the delivery item and create a tracking entry
        item.setStatus(newStatus);
        // Store the last status note for quick access on the delivery item
        item.setLastStatusNote(req.getNote() != null ? req.getNote().trim() : null);
        deliveryItemRepository.save(item);

        DeliveryTracking tracking = new DeliveryTracking(item, newStatus, req.getNote() != null ? req.getNote() : "Status updated via API", currentUser);
        deliveryTrackingRepository.save(tracking);

        return ResponseEntity.ok(Map.of(
            "status", newStatus.toString(), 
            "statusDisplay", StatusDisplayDTO.fromDeliveryStatus(newStatus),
            "deliveryId", item.getId()
        ));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<?> getDeliveryTracking(@PathVariable UUID id) {
        // Auth
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId;
        try { userId = UUID.fromString(userIdStr); } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User currentUser = optUser.get();

        // Find delivery item
        var optItem = deliveryItemRepository.findById(id).filter(d -> !d.isDeleted());
        if (optItem.isEmpty()) return ResponseEntity.notFound().build();
        DeliveryItem item = optItem.get();

        // Authorization: same rules as status change
        boolean allowed = false;
        if (item.getSender() != null && item.getSender().getId().equals(currentUser.getId())) allowed = true;
        if (item.getReceiver() != null && item.getReceiver().getId().equals(currentUser.getId())) allowed = true;
        if (item.getDeliveryDriver() != null && item.getDeliveryDriver().getId().equals(currentUser.getId())) allowed = true;
        if (!allowed && item.getDeliveryCompany() != null && currentUser.getCompany() != null && item.getDeliveryCompany().getId().equals(currentUser.getCompany().getId())) {
            if (currentUser.getUserRole() != null && (currentUser.getUserRole().name().equals("OWNER") || currentUser.getUserRole().name().equals("MANAGER") || currentUser.getUserRole().name().equals("SYSTEM_ADMINISTRATOR"))) {
                allowed = true;
            }
        }

        if (!allowed) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions to view tracking history"));

        // Fetch tracking entries
        List<DeliveryTracking> tracks = deliveryTrackingRepository.findByDeliveryItemIdAndDeletedFalseOrderByTimestampDesc(item.getId());

        // Map to DTO
        List<Map<String, Object>> dto = tracks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("status", t.getStatus() != null ? t.getStatus().toString() : null);
            
            // Add status display information with translations and grouping
            if (t.getStatus() != null) {
                Map<String, Object> statusDisplay = new LinkedHashMap<>();
                statusDisplay.put("code", t.getStatus().toString());
                statusDisplay.put("english", t.getStatus().getEnglishName());
                statusDisplay.put("khmer", t.getStatus().getKhmerName());
                
                Map<String, Object> groupInfo = new LinkedHashMap<>();
                groupInfo.put("code", t.getStatus().getGroup().name());
                groupInfo.put("english", t.getStatus().getGroup().getEnglishName());
                groupInfo.put("khmer", t.getStatus().getGroup().getKhmerName());
                statusDisplay.put("group", groupInfo);
                
                m.put("statusDisplay", statusDisplay);
            }
            
            m.put("description", t.getDescription());
            m.put("timestamp", t.getTimestamp());
            m.put("statusUpdatedById", t.getStatusUpdatedBy() != null ? t.getStatusUpdatedBy().getId() : null);
            m.put("statusUpdatedByName", t.getStatusUpdatedBy() != null ? t.getStatusUpdatedBy().getDisplayName() : null);
            return m;
        }).toList();

        return ResponseEntity.ok(dto);
    }

    // Request/Response DTOs
    public static class UpdateDeliveryRequest {
        @JsonProperty("senderName")
        private String senderName; // Optional: override authenticated user's name

        @JsonProperty("senderPhone")
        private String senderPhone; // Optional: override authenticated user's phone

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
        private String paymentMethod;

        @JsonProperty("items")
        private List<DeliveryItemPayload> items; // Updated list of delivery items

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

        @JsonProperty("deliveryDiscount")
        private BigDecimal deliveryDiscount;

        @JsonProperty("orderDiscount")
        private BigDecimal orderDiscount;

        @JsonProperty("actualDeliveryCost")
        private BigDecimal actualDeliveryCost;

        @JsonProperty("specialInstructions")
        private String specialInstructions;

        @JsonProperty("deliveryPhotos")
        private List<String> deliveryPhotos;

        // Getters and setters for UpdateDeliveryRequest
        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }

        public String getSenderPhone() { return senderPhone; }
        public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }

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

        public BigDecimal getDeliveryDiscount() { return deliveryDiscount; }
        public void setDeliveryDiscount(BigDecimal deliveryDiscount) { this.deliveryDiscount = deliveryDiscount; }

        public BigDecimal getOrderDiscount() { return orderDiscount; }
        public void setOrderDiscount(BigDecimal orderDiscount) { this.orderDiscount = orderDiscount; }

        public BigDecimal getActualDeliveryCost() { return actualDeliveryCost; }
        public void setActualDeliveryCost(BigDecimal actualDeliveryCost) { this.actualDeliveryCost = actualDeliveryCost; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

        public List<String> getDeliveryPhotos() { return deliveryPhotos; }
        public void setDeliveryPhotos(List<String> deliveryPhotos) { this.deliveryPhotos = deliveryPhotos; }

        public List<DeliveryItemPayload> getItems() { return items; }
        public void setItems(List<DeliveryItemPayload> items) { this.items = items; }
    }

    public static class CreateDeliveryRequest {
        @JsonProperty("senderName")
        private String senderName; // Optional: override authenticated user's name

        @JsonProperty("senderPhone")
        private String senderPhone; // Optional: override authenticated user's phone

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

        @JsonProperty("deliveryDiscount")
        private BigDecimal deliveryDiscount; // Discount on delivery fee

        @JsonProperty("orderDiscount")
        private BigDecimal orderDiscount; // Order-wide discount

        @JsonProperty("actualDeliveryCost")
        private BigDecimal actualDeliveryCost; // Real delivery cost (even when free)

        @JsonProperty("specialInstructions")
        private String specialInstructions;

        @JsonProperty("deliveryPhotos")
        private List<String> deliveryPhotos; // Package/delivery photos (optional)

        // Getters and setters for CreateDeliveryRequest
        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }

        public String getSenderPhone() { return senderPhone; }
        public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }

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

        public BigDecimal getDeliveryDiscount() { return deliveryDiscount; }
        public void setDeliveryDiscount(BigDecimal deliveryDiscount) { this.deliveryDiscount = deliveryDiscount; }

        public BigDecimal getOrderDiscount() { return orderDiscount; }
        public void setOrderDiscount(BigDecimal orderDiscount) { this.orderDiscount = orderDiscount; }

        public BigDecimal getActualDeliveryCost() { return actualDeliveryCost; }
        public void setActualDeliveryCost(BigDecimal actualDeliveryCost) { this.actualDeliveryCost = actualDeliveryCost; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod != null ? paymentMethod : "COD"; }

        public List<String> getDeliveryPhotos() { return deliveryPhotos; }
        public void setDeliveryPhotos(List<String> deliveryPhotos) { this.deliveryPhotos = deliveryPhotos; }

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

        @JsonProperty("price")
        private BigDecimal price; // Item price (preferred)

        @JsonProperty("estimatedValue")
        private BigDecimal estimatedValue; // Deprecated: use 'price' instead

        @JsonProperty("itemDiscount")
        private BigDecimal itemDiscount; // Discount on this specific item

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

        public BigDecimal getPrice() { 
            // Prefer 'price' but fall back to 'estimatedValue' for backwards compatibility
            return price != null ? price : estimatedValue; 
        }
        public void setPrice(BigDecimal price) { this.price = price; }

        public BigDecimal getEstimatedValue() { return getPrice(); } // Delegate to getPrice()
        public void setEstimatedValue(BigDecimal estimatedValue) { 
            // Support old field name
            if (this.price == null) {
                this.price = estimatedValue;
            }
            this.estimatedValue = estimatedValue;
        }

        public Integer getQuantity() { return quantity != null ? quantity : 1; }
        public void setQuantity(Integer quantity) { this.quantity = quantity != null && quantity > 0 ? quantity : 1; }

        public BigDecimal getItemDiscount() { return itemDiscount; }
        public void setItemDiscount(BigDecimal itemDiscount) { this.itemDiscount = itemDiscount; }

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

        @JsonProperty("statusDisplay")
        private StatusDisplayDTO statusDisplay;

        @JsonProperty("trackingCode")
        private String trackingCode;

        @JsonProperty("estimatedDelivery")
        private String estimatedDelivery;

        @JsonProperty("error")
        private String error;

        public DeliveryResponse(DeliveryItem delivery) {
            this.deliveryId = delivery.getId();
            this.status = delivery.getStatus().toString();
            this.statusDisplay = StatusDisplayDTO.fromDeliveryStatus(delivery.getStatus());
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

    /**
     * Calculate subTotal and grandTotal for a batch
     * Formula: 
     * - Item Total = (price * quantity) - itemDiscount
     * - Sub Total = Sum of all Item Totals + deliveryFee - deliveryDiscount
     * - Grand Total = subTotal - orderDiscount
     */
    private void calculateBatchTotals(DeliveryBatchDTO batch, List<DeliveryItem> batchItems) {
        BigDecimal itemsTotal = BigDecimal.ZERO;
        
        // Calculate total from all items (price * quantity - itemDiscount)
        for (DeliveryItem item : batchItems) {
            BigDecimal itemValue = item.getItemValue() != null ? item.getItemValue() : BigDecimal.ZERO;
            int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
            BigDecimal itemDiscount = item.getItemDiscount() != null ? item.getItemDiscount() : BigDecimal.ZERO;
            
            BigDecimal itemTotal = itemValue.multiply(BigDecimal.valueOf(quantity)).subtract(itemDiscount);
            itemsTotal = itemsTotal.add(itemTotal);
        }
        
        // Get first item for shared fields
        DeliveryItem first = batchItems.get(0);
        BigDecimal deliveryFee = first.getDeliveryFee() != null ? first.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal deliveryDiscount = first.getDeliveryDiscount() != null ? first.getDeliveryDiscount() : BigDecimal.ZERO;
        BigDecimal orderDiscount = first.getOrderDiscount() != null ? first.getOrderDiscount() : BigDecimal.ZERO;
        
        // Calculate subTotal and grandTotal
        BigDecimal subTotal = itemsTotal.add(deliveryFee).subtract(deliveryDiscount);
        BigDecimal grandTotal = subTotal.subtract(orderDiscount);
        
        batch.setSubTotal(subTotal);
        batch.setGrandTotal(grandTotal);
        
        // Set discount fields at batch level
        batch.setDeliveryDiscount(deliveryDiscount);
        batch.setOrderDiscount(orderDiscount);
        batch.setCurrency(first.getCurrency() != null ? first.getCurrency() : "USD");
        batch.setActualDeliveryCost(first.getActualDeliveryCost());
        batch.setKhrAmount(first.getKhrAmount());
        batch.setExchangeRateUsed(first.getExchangeRateUsed());
    }

    // Customer verification DTOs
    public static class VerifyCustomerRequest {
        @JsonProperty("receiverPhone")
        public String receiverPhone;

        @JsonProperty("receiverName")
        public String receiverName;

        @JsonProperty("deliveryAddress")
        public String deliveryAddress;

        @JsonProperty("deliveryProvinceId")
        public UUID deliveryProvinceId;

        @JsonProperty("deliveryDistrictId")
        public UUID deliveryDistrictId;
    }

    public static class CustomerVerificationResponse {
        @JsonProperty("exists")
        public boolean exists;

        @JsonProperty("customerId")
        public String customerId;

        @JsonProperty("hasChanges")
        public boolean hasChanges;

        @JsonProperty("currentInfo")
        public CustomerInfo currentInfo;

        @JsonProperty("changes")
        public Map<String, FieldChange> changes;

        public CustomerVerificationResponse(boolean exists, String customerId, boolean hasChanges, 
                                          CustomerInfo currentInfo, Map<String, FieldChange> changes) {
            this.exists = exists;
            this.customerId = customerId;
            this.hasChanges = hasChanges;
            this.currentInfo = currentInfo;
            this.changes = changes;
        }
    }

    public static class CustomerInfo {
        @JsonProperty("name")
        public String name;

        @JsonProperty("phone")
        public String phone;

        @JsonProperty("address")
        public String address;

        @JsonProperty("provinceId")
        public UUID provinceId;

        @JsonProperty("districtId")
        public UUID districtId;

        @JsonProperty("provinceName")
        public String provinceName;

        @JsonProperty("districtName")
        public String districtName;

        public CustomerInfo(String name, String phone, String address, UUID provinceId, 
                          UUID districtId, String provinceName, String districtName) {
            this.name = name;
            this.phone = phone;
            this.address = address;
            this.provinceId = provinceId;
            this.districtId = districtId;
            this.provinceName = provinceName;
            this.districtName = districtName;
        }
    }

    public static class FieldChange {
        @JsonProperty("old")
        public String oldValue;

        @JsonProperty("new")
        public String newValue;

        public FieldChange(String oldValue, String newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}