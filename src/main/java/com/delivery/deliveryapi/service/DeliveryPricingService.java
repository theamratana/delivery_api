package com.delivery.deliveryapi.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.deliveryapi.controller.DeliveryController.CreateDeliveryRequest;
import com.delivery.deliveryapi.controller.DeliveryPricingController;
import com.delivery.deliveryapi.model.DeliveryPricingRule;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.DeliveryPricingRuleRepository;

@Service
public class DeliveryPricingService {

    private final DeliveryPricingRuleRepository pricingRuleRepository;

    public DeliveryPricingService(DeliveryPricingRuleRepository pricingRuleRepository) {
        this.pricingRuleRepository = pricingRuleRepository;
    }

    @Transactional
    public DeliveryPricingRule createPricingRule(User user, String ruleName, BigDecimal baseFee) {
        DeliveryPricingRule rule = new DeliveryPricingRule(user.getCompany(), ruleName, baseFee);
        return pricingRuleRepository.save(rule);
    }

    @Transactional
    public DeliveryPricingRule createPricingRule(User user, String ruleName, String province,
                                                String district, BigDecimal baseFee) {
        if (user.getCompany() == null) {
            throw new IllegalArgumentException("User must be assigned to a company to create pricing rules. User: " + user.getId() + ", company: null");
        }
        DeliveryPricingRule rule = new DeliveryPricingRule(user.getCompany(), ruleName, baseFee);
        rule.setProvince(province);
        rule.setDistrict(district);
        return pricingRuleRepository.save(rule);
    }

    @Transactional
    public DeliveryPricingRule updatePricingRule(UUID ruleId, String ruleName, BigDecimal baseFee,
                                                BigDecimal highValueSurcharge, BigDecimal highValueThreshold) {
        Optional<DeliveryPricingRule> optRule = pricingRuleRepository.findById(ruleId);
        if (optRule.isEmpty()) {
            throw new IllegalArgumentException("Pricing rule not found");
        }

        DeliveryPricingRule rule = optRule.get();
        if (ruleName != null) rule.setRuleName(ruleName);
        if (baseFee != null) rule.setBaseFee(baseFee);
        if (highValueSurcharge != null) rule.setHighValueSurcharge(highValueSurcharge);
        if (highValueThreshold != null) rule.setHighValueThreshold(highValueThreshold);

        return pricingRuleRepository.save(rule);
    }

    @Transactional
    public void deletePricingRule(UUID ruleId, User user) {
        Optional<DeliveryPricingRule> optRule = pricingRuleRepository.findById(ruleId);
        if (optRule.isEmpty()) {
            throw new IllegalArgumentException("Pricing rule not found");
        }

        DeliveryPricingRule rule = optRule.get();
        if (!rule.getCompany().getId().equals(user.getCompany().getId())) {
            throw new IllegalArgumentException("You can only delete pricing rules from your company");
        }

        pricingRuleRepository.delete(rule);
    }

    public List<DeliveryPricingRule> getUserPricingRules(User user) {
        return pricingRuleRepository.findByCompanyAndIsActive(user.getCompany(), true);
    }

    public BigDecimal calculateDeliveryFee(User user, CreateDeliveryRequest request) {
        // If user specified a custom fee, use it
        if (request.getDeliveryFee() != null) {
            return request.getDeliveryFee();
        }

        // Find applicable pricing rules for the delivery location
        List<DeliveryPricingRule> applicableRules = pricingRuleRepository
            .findApplicableRules(user.getCompany(), request.getDeliveryProvince(), request.getDeliveryDistrict());

        if (!applicableRules.isEmpty()) {
            // Use the highest priority rule
            DeliveryPricingRule rule = applicableRules.get(0);
            return calculateFeeFromRule(rule, request.getEstimatedValue());
        }

        // Fallback to default calculation if no rules found
        return calculateDefaultFee(request);
    }

    public BigDecimal calculateDeliveryFee(User user, DeliveryPricingController.PriceCalculationRequest request) {
        // If user specified a custom fee, use it
        if (request.getDeliveryFee() != null) {
            return request.getDeliveryFee();
        }

        // Find applicable pricing rules for the delivery location
        List<DeliveryPricingRule> applicableRules = pricingRuleRepository
            .findApplicableRules(user.getCompany(), request.getDeliveryProvince(), request.getDeliveryDistrict());

        if (!applicableRules.isEmpty()) {
            // Use the highest priority rule
            DeliveryPricingRule rule = applicableRules.get(0);
            return calculateFeeFromRule(rule, request.getEstimatedValue());
        }

        // Fallback to default calculation if no rules found
        return calculateDefaultFee(request);
    }

    private BigDecimal calculateFeeFromRule(DeliveryPricingRule rule, BigDecimal itemValue) {
        BigDecimal fee = rule.getBaseFee();

        // Add high-value surcharge if applicable
        if (itemValue != null && rule.getHighValueThreshold() != null &&
            itemValue.compareTo(rule.getHighValueThreshold()) > 0) {
            fee = fee.add(rule.getHighValueSurcharge() != null ? rule.getHighValueSurcharge() : BigDecimal.ZERO);
        }

        return fee.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDefaultFee(CreateDeliveryRequest request) {
        // Default calculation (same as before)
        BigDecimal baseFee = BigDecimal.valueOf(2.00);

        // Geographic multiplier
        BigDecimal locationMultiplier = BigDecimal.valueOf(1.0);
        if (!"Phnom Penh".equalsIgnoreCase(request.getPickupProvince()) ||
            !"Phnom Penh".equalsIgnoreCase(request.getDeliveryProvince())) {
            locationMultiplier = BigDecimal.valueOf(1.5);
        }

        // High-value item surcharge
        if (request.getEstimatedValue() != null && request.getEstimatedValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            baseFee = baseFee.add(BigDecimal.valueOf(1.00));
        }

        BigDecimal calculatedFee = baseFee.multiply(locationMultiplier);

        // Apply delivery fee model adjustments
        if ("FREE".equalsIgnoreCase(request.getDeliveryFeeModel())) {
            calculatedFee = BigDecimal.ZERO;
        }

        return calculatedFee.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDefaultFee(DeliveryPricingController.PriceCalculationRequest request) {
        // Default calculation (same as before)
        BigDecimal baseFee = BigDecimal.valueOf(2.00);

        // Geographic multiplier
        BigDecimal locationMultiplier = BigDecimal.valueOf(1.0);
        if (!"Phnom Penh".equalsIgnoreCase(request.getPickupProvince()) ||
            !"Phnom Penh".equalsIgnoreCase(request.getDeliveryProvince())) {
            locationMultiplier = BigDecimal.valueOf(1.5);
        }

        // High-value item surcharge
        if (request.getEstimatedValue() != null && request.getEstimatedValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            baseFee = baseFee.add(BigDecimal.valueOf(1.00));
        }

        BigDecimal calculatedFee = baseFee.multiply(locationMultiplier);

        // Apply delivery fee model adjustments
        if ("FREE".equalsIgnoreCase(request.getDeliveryFeeModel())) {
            calculatedFee = BigDecimal.ZERO;
        }

        return calculatedFee.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    // DTOs for API
    public static class PricingRuleRequest {
        private String ruleName;
        private String province;
        private String district;
        private BigDecimal baseFee;
        private BigDecimal highValueSurcharge;
        private BigDecimal highValueThreshold;
        private Integer priority;

        // Getters and setters
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }

        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }

        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }

        public BigDecimal getBaseFee() { return baseFee; }
        public void setBaseFee(BigDecimal baseFee) { this.baseFee = baseFee; }

        public BigDecimal getHighValueSurcharge() { return highValueSurcharge; }
        public void setHighValueSurcharge(BigDecimal highValueSurcharge) { this.highValueSurcharge = highValueSurcharge; }

        public BigDecimal getHighValueThreshold() { return highValueThreshold; }
        public void setHighValueThreshold(BigDecimal highValueThreshold) { this.highValueThreshold = highValueThreshold; }

        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
    }

    public static class PricingRuleResponse {
        private String id;
        private String ruleName;
        private String province;
        private String district;
        private BigDecimal baseFee;
        private BigDecimal highValueSurcharge;
        private BigDecimal highValueThreshold;
        private Integer priority;

        public PricingRuleResponse(DeliveryPricingRule rule) {
            this.id = rule.getId().toString();
            this.ruleName = rule.getRuleName();
            this.province = rule.getProvince();
            this.district = rule.getDistrict();
            this.baseFee = rule.getBaseFee();
            this.highValueSurcharge = rule.getHighValueSurcharge();
            this.highValueThreshold = rule.getHighValueThreshold();
            this.priority = rule.getPriority();
        }

        // Getters
        public String getId() { return id; }
        public String getRuleName() { return ruleName; }
        public String getProvince() { return province; }
        public String getDistrict() { return district; }
        public BigDecimal getBaseFee() { return baseFee; }
        public BigDecimal getHighValueSurcharge() { return highValueSurcharge; }
        public BigDecimal getHighValueThreshold() { return highValueThreshold; }
        public Integer getPriority() { return priority; }
    }
}