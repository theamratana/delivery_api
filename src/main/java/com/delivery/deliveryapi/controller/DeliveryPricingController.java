package com.delivery.deliveryapi.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.service.DeliveryPricingService;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/pricing")
public class DeliveryPricingController {

    private final DeliveryPricingService pricingService;
    private final UserRepository userRepository;

    public DeliveryPricingController(DeliveryPricingService pricingService, UserRepository userRepository) {
        this.pricingService = pricingService;
        this.userRepository = userRepository;
    }

    @PostMapping("/rules")
    @Transactional
    public ResponseEntity<PricingRuleResponse> createPricingRule(@Valid @RequestBody CreatePricingRuleRequest request) {
        try {
            User currentUser = getCurrentUser();

            var rule = pricingService.createPricingRule(
                currentUser,
                request.getRuleName(),
                request.getProvince(),
                request.getDistrict(),
                request.getBaseFee()
            );

            // Set additional properties if provided
            if (request.getHighValueSurcharge() != null) {
                rule.setHighValueSurcharge(request.getHighValueSurcharge());
            }
            if (request.getHighValueThreshold() != null) {
                rule.setHighValueThreshold(request.getHighValueThreshold());
            }
            if (request.getPriority() != null) {
                rule.setPriority(request.getPriority());
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new PricingRuleResponse(rule));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new PricingRuleResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/rules")
    public ResponseEntity<List<PricingRuleResponse>> getPricingRules() {
        try {
            User currentUser = getCurrentUser();
            var rules = pricingService.getUserPricingRules(currentUser);
            var responses = rules.stream()
                    .map(PricingRuleResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/rules/{ruleId}")
    @Transactional
    public ResponseEntity<PricingRuleResponse> updatePricingRule(
            @PathVariable UUID ruleId,
            @RequestBody UpdatePricingRuleRequest request) {
        try {
            User currentUser = getCurrentUser();

            var rule = pricingService.updatePricingRule(
                ruleId,
                request.getRuleName(),
                request.getProvince(),
                request.getDistrict(),
                request.getBaseFee(),
                request.getHighValueSurcharge(),
                request.getHighValueThreshold(),
                request.getPriority()
            );

            return ResponseEntity.ok(new PricingRuleResponse(rule));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new PricingRuleResponse("Error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/rules/{ruleId}")
    @Transactional
    public ResponseEntity<Void> deletePricingRule(@PathVariable UUID ruleId) {
        try {
            User currentUser = getCurrentUser();
            pricingService.deletePricingRule(ruleId, currentUser);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/calculate-price")
    public ResponseEntity<PricingBreakdownResponse> calculateDeliveryPrice(@RequestBody PriceCalculationRequest request) {
        try {
            User currentUser = getCurrentUser();

            // Calculate delivery fee using the pricing service
            BigDecimal deliveryFee = pricingService.calculateDeliveryFee(currentUser, request);

            // Calculate total cost
            BigDecimal itemCost = request.getEstimatedValue() != null ? request.getEstimatedValue() : BigDecimal.ZERO;
            BigDecimal totalCost = itemCost.add(deliveryFee);

            PricingBreakdownResponse response = new PricingBreakdownResponse(
                itemCost,
                deliveryFee,
                totalCost
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new PricingBreakdownResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/find-rule")
    public ResponseEntity<FindRuleResponse> findMatchingRule(
            @org.springframework.web.bind.annotation.RequestParam(required = true) String province,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String district,
            @org.springframework.web.bind.annotation.RequestParam(required = false) BigDecimal totalPrice) {
        try {
            User currentUser = getCurrentUser();

            var matchedRule = pricingService.findMatchingRule(currentUser, province, district, totalPrice);

            if (matchedRule != null) {
                return ResponseEntity.ok(new FindRuleResponse(matchedRule, totalPrice));
            } else {
                return ResponseEntity.ok(new FindRuleResponse("No matching pricing rule found"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new FindRuleResponse("Error: " + e.getMessage()));
        }
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            throw new IllegalStateException("User not authenticated");
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid user ID");
        }
        var optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            throw new IllegalStateException("User not found");
        }
        return optUser.get();
    }

    // Request/Response DTOs
    public static class CreatePricingRuleRequest {
        @JsonProperty("ruleName")
        @NotBlank(message = "Rule name is required")
        private String ruleName;

        @JsonProperty("province")
        private String province;

        @JsonProperty("district")
        private String district;

        @JsonProperty("baseFee")
        @NotNull(message = "Base fee is required")
        @DecimalMin(value = "0.00", message = "Base fee must be positive")
        private java.math.BigDecimal baseFee;

        @JsonProperty("highValueSurcharge")
        @DecimalMin(value = "0.00", message = "High value surcharge must be positive")
        private java.math.BigDecimal highValueSurcharge;

        @JsonProperty("highValueThreshold")
        @DecimalMin(value = "0.00", message = "High value threshold must be positive")
        private java.math.BigDecimal highValueThreshold;

        @JsonProperty("priority")
        @Min(value = 0, message = "Priority must be non-negative")
        private Integer priority;

        // Getters and setters
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }

        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }

        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }

        public java.math.BigDecimal getBaseFee() { return baseFee; }
        public void setBaseFee(java.math.BigDecimal baseFee) { this.baseFee = baseFee; }

        public java.math.BigDecimal getHighValueSurcharge() { return highValueSurcharge; }
        public void setHighValueSurcharge(java.math.BigDecimal highValueSurcharge) { this.highValueSurcharge = highValueSurcharge; }

        public java.math.BigDecimal getHighValueThreshold() { return highValueThreshold; }
        public void setHighValueThreshold(java.math.BigDecimal highValueThreshold) { this.highValueThreshold = highValueThreshold; }

        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
    }

    public static class UpdatePricingRuleRequest {
        @JsonProperty("ruleName")
        private String ruleName;

        @JsonProperty("province")
        private String province;

        @JsonProperty("district")
        private String district;

        @JsonProperty("baseFee")
        private java.math.BigDecimal baseFee;

        @JsonProperty("highValueSurcharge")
        private java.math.BigDecimal highValueSurcharge;

        @JsonProperty("highValueThreshold")
        private java.math.BigDecimal highValueThreshold;

        @JsonProperty("priority")
        private Integer priority;

        // Getters and setters
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }

        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }

        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }

        public java.math.BigDecimal getBaseFee() { return baseFee; }
        public void setBaseFee(java.math.BigDecimal baseFee) { this.baseFee = baseFee; }

        public java.math.BigDecimal getHighValueSurcharge() { return highValueSurcharge; }
        public void setHighValueSurcharge(java.math.BigDecimal highValueSurcharge) { this.highValueSurcharge = highValueSurcharge; }

        public java.math.BigDecimal getHighValueThreshold() { return highValueThreshold; }
        public void setHighValueThreshold(java.math.BigDecimal highValueThreshold) { this.highValueThreshold = highValueThreshold; }

        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
    }

    public static class PricingRuleResponse {
        @JsonProperty("id")
        private String id;

        @JsonProperty("ruleName")
        private String ruleName;

        @JsonProperty("province")
        private String province;

        @JsonProperty("district")
        private String district;

        @JsonProperty("baseFee")
        private java.math.BigDecimal baseFee;

        @JsonProperty("highValueSurcharge")
        private java.math.BigDecimal highValueSurcharge;

        @JsonProperty("highValueThreshold")
        private java.math.BigDecimal highValueThreshold;

        @JsonProperty("priority")
        private Integer priority;

        @JsonProperty("error")
        private String error;

        public PricingRuleResponse(com.delivery.deliveryapi.model.DeliveryPricingRule rule) {
            this.id = rule.getId().toString();
            this.ruleName = rule.getRuleName();
            this.province = rule.getProvince();
            this.district = rule.getDistrict();
            this.baseFee = rule.getBaseFee();
            this.highValueSurcharge = rule.getHighValueSurcharge();
            this.highValueThreshold = rule.getHighValueThreshold();
            this.priority = rule.getPriority();
        }

        public PricingRuleResponse(String error) {
            this.error = error;
        }

        // Getters
        public String getId() { return id; }
        public String getRuleName() { return ruleName; }
        public String getProvince() { return province; }
        public String getDistrict() { return district; }
        public java.math.BigDecimal getBaseFee() { return baseFee; }
        public java.math.BigDecimal getHighValueSurcharge() { return highValueSurcharge; }
        public java.math.BigDecimal getHighValueThreshold() { return highValueThreshold; }
        public Integer getPriority() { return priority; }
        public String getError() { return error; }
    }

    public static class PriceCalculationRequest {
        @JsonProperty("deliveryProvince")
        private String deliveryProvince;

        @JsonProperty("deliveryDistrict")
        private String deliveryDistrict;

        @JsonProperty("pickupProvince")
        private String pickupProvince;

        @JsonProperty("estimatedValue")
        private java.math.BigDecimal estimatedValue;

        @JsonProperty("deliveryFee")
        private java.math.BigDecimal deliveryFee;

        @JsonProperty("deliveryFeeModel")
        private String deliveryFeeModel;

        // Getters and setters
        public String getDeliveryProvince() { return deliveryProvince; }
        public void setDeliveryProvince(String deliveryProvince) { this.deliveryProvince = deliveryProvince; }

        public String getDeliveryDistrict() { return deliveryDistrict; }
        public void setDeliveryDistrict(String deliveryDistrict) { this.deliveryDistrict = deliveryDistrict; }

        public String getPickupProvince() { return pickupProvince; }
        public void setPickupProvince(String pickupProvince) { this.pickupProvince = pickupProvince; }

        public java.math.BigDecimal getEstimatedValue() { return estimatedValue; }
        public void setEstimatedValue(java.math.BigDecimal estimatedValue) { this.estimatedValue = estimatedValue; }

        public java.math.BigDecimal getDeliveryFee() { return deliveryFee; }
        public void setDeliveryFee(java.math.BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

        public String getDeliveryFeeModel() { return deliveryFeeModel; }
        public void setDeliveryFeeModel(String deliveryFeeModel) { this.deliveryFeeModel = deliveryFeeModel; }
    }

    public static class PricingBreakdownResponse {
        @JsonProperty("itemCost")
        private java.math.BigDecimal itemCost;

        @JsonProperty("deliveryFee")
        private java.math.BigDecimal deliveryFee;

        @JsonProperty("totalCost")
        private java.math.BigDecimal totalCost;

        @JsonProperty("error")
        private String error;

        public PricingBreakdownResponse(java.math.BigDecimal itemCost, java.math.BigDecimal deliveryFee, java.math.BigDecimal totalCost) {
            this.itemCost = itemCost;
            this.deliveryFee = deliveryFee;
            this.totalCost = totalCost;
        }

        public PricingBreakdownResponse(String error) {
            this.error = error;
        }

        // Getters
        public java.math.BigDecimal getItemCost() { return itemCost; }
        public java.math.BigDecimal getDeliveryFee() { return deliveryFee; }
        public java.math.BigDecimal getTotalCost() { return totalCost; }
        public String getError() { return error; }
    }

    public static class FindRuleResponse {
        @JsonProperty("ruleId")
        private String ruleId;

        @JsonProperty("ruleName")
        private String ruleName;

        @JsonProperty("province")
        private String province;

        @JsonProperty("district")
        private String district;

        @JsonProperty("baseFee")
        private java.math.BigDecimal baseFee;

        @JsonProperty("highValueSurcharge")
        private java.math.BigDecimal highValueSurcharge;

        @JsonProperty("highValueThreshold")
        private java.math.BigDecimal highValueThreshold;

        @JsonProperty("priority")
        private Integer priority;

        @JsonProperty("calculatedFee")
        private java.math.BigDecimal calculatedFee;

        @JsonProperty("error")
        private String error;

        public FindRuleResponse(com.delivery.deliveryapi.model.DeliveryPricingRule rule, java.math.BigDecimal totalPrice) {
            this.ruleId = rule.getId().toString();
            this.ruleName = rule.getRuleName();
            this.province = rule.getProvince();
            this.district = rule.getDistrict();
            this.baseFee = rule.getBaseFee();
            this.highValueSurcharge = rule.getHighValueSurcharge();
            this.highValueThreshold = rule.getHighValueThreshold();
            this.priority = rule.getPriority();

            // Calculate fee based on total price
            java.math.BigDecimal fee = rule.getBaseFee();
            if (totalPrice != null && rule.getHighValueThreshold() != null &&
                totalPrice.compareTo(rule.getHighValueThreshold()) > 0) {
                fee = fee.add(rule.getHighValueSurcharge() != null ? rule.getHighValueSurcharge() : java.math.BigDecimal.ZERO);
            }
            this.calculatedFee = fee.setScale(2, java.math.RoundingMode.HALF_UP);
        }

        public FindRuleResponse(String error) {
            this.error = error;
        }

        // Getters
        public String getRuleId() { return ruleId; }
        public String getRuleName() { return ruleName; }
        public String getProvince() { return province; }
        public String getDistrict() { return district; }
        public java.math.BigDecimal getBaseFee() { return baseFee; }
        public java.math.BigDecimal getHighValueSurcharge() { return highValueSurcharge; }
        public java.math.BigDecimal getHighValueThreshold() { return highValueThreshold; }
        public Integer getPriority() { return priority; }
        public java.math.BigDecimal getCalculatedFee() { return calculatedFee; }
        public String getError() { return error; }
    }
}