package com.delivery.deliveryapi.service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery.deliveryapi.controller.DeliveryController.CreateDeliveryRequest;
import com.delivery.deliveryapi.model.DeliveryPricingRule;
import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.DeliveryPricingRuleRepository;

@ExtendWith(MockitoExtension.class)
class DeliveryPricingServiceTest {

    @Mock
    private DeliveryPricingRuleRepository pricingRuleRepository;

    @InjectMocks
    private DeliveryPricingService pricingService;

    private User testUser;
    private Company testCompany;
    private UUID userId;
    private UUID companyId;

    @BeforeEach
    void setUp() throws Exception {
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        
        testCompany = new Company();
        Field companyIdField = Company.class.getDeclaredField("id");
        companyIdField.setAccessible(true);
        companyIdField.set(testCompany, companyId);
        testCompany.setName("Test Company");
        
        testUser = new User();
        // Use reflection to set the ID since it's private
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testUser, userId);
        testUser.setUsername("testuser");
        testUser.setCompany(testCompany);
    }

    @Test
    void testCreatePricingRule() {
        // Given
        String ruleName = "Test Rule";
        BigDecimal baseFee = BigDecimal.valueOf(2.50);

        DeliveryPricingRule savedRule = new DeliveryPricingRule(testCompany, ruleName, baseFee);
        setRuleId(savedRule, UUID.randomUUID());

        when(pricingRuleRepository.save(any(DeliveryPricingRule.class))).thenReturn(savedRule);

        // When
        DeliveryPricingRule result = pricingService.createPricingRule(testUser, ruleName, baseFee);

        // Then
        assertNotNull(result);
        assertEquals(ruleName, result.getRuleName());
        assertEquals(baseFee, result.getBaseFee());
        assertEquals(testCompany, result.getCompany());
        verify(pricingRuleRepository).save(any(DeliveryPricingRule.class));
    }

    @Test
    void testCreatePricingRuleWithLocation() {
        // Given
        String ruleName = "Phnom Penh Rule";
        String province = "Phnom Penh";
        String district = "Chamkarmon";
        BigDecimal baseFee = BigDecimal.valueOf(2.00);

        DeliveryPricingRule savedRule = new DeliveryPricingRule(testCompany, ruleName, baseFee);
        savedRule.setProvince(province);
        savedRule.setDistrict(district);
        setRuleId(savedRule, UUID.randomUUID());

        when(pricingRuleRepository.save(any(DeliveryPricingRule.class))).thenReturn(savedRule);

        // When
        DeliveryPricingRule result = pricingService.createPricingRule(testUser, ruleName, province, district, baseFee);

        // Then
        assertNotNull(result);
        assertEquals(ruleName, result.getRuleName());
        assertEquals(province, result.getProvince());
        assertEquals(district, result.getDistrict());
        assertEquals(baseFee, result.getBaseFee());
        verify(pricingRuleRepository).save(any(DeliveryPricingRule.class));
    }

    @Test
    void testUpdatePricingRule() {
        // Given
        UUID ruleId = UUID.randomUUID();
        String newRuleName = "Updated Rule";
        BigDecimal newBaseFee = BigDecimal.valueOf(3.00);

        DeliveryPricingRule existingRule = new DeliveryPricingRule(testCompany, "Old Rule", BigDecimal.valueOf(2.00));
        setRuleId(existingRule, ruleId);

        DeliveryPricingRule updatedRule = new DeliveryPricingRule(testCompany, newRuleName, newBaseFee);
        setRuleId(updatedRule, ruleId);

        when(pricingRuleRepository.findById(eq(ruleId))).thenReturn(Optional.of(existingRule));
        when(pricingRuleRepository.save(any(DeliveryPricingRule.class))).thenReturn(updatedRule);

        // When
        DeliveryPricingRule result = pricingService.updatePricingRule(ruleId, newRuleName, newBaseFee, null, null);

        // Then
        assertNotNull(result);
        assertEquals(newRuleName, result.getRuleName());
        assertEquals(newBaseFee, result.getBaseFee());
        verify(pricingRuleRepository).findById(eq(ruleId));
        verify(pricingRuleRepository).save(existingRule);
    }

    @Test
    void testUpdatePricingRuleNotFound() {
        // Given
        UUID ruleId = UUID.randomUUID();
        when(pricingRuleRepository.findById(eq(ruleId))).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            pricingService.updatePricingRule(ruleId, "Test", BigDecimal.ONE, null, null));
    }

    @Test
    void testDeletePricingRule() throws Exception {
        // Given
        UUID ruleId = UUID.randomUUID();
        DeliveryPricingRule rule = new DeliveryPricingRule(testCompany, "Test Rule", BigDecimal.ONE);
        setRuleId(rule, ruleId);

        when(pricingRuleRepository.findById(eq(ruleId))).thenReturn(Optional.of(rule));

        // When
        pricingService.deletePricingRule(ruleId, testUser);

        // Then
        verify(pricingRuleRepository).findById(eq(ruleId));
        verify(pricingRuleRepository).delete(rule);
    }

    @Test
    void testDeletePricingRuleNotFound() {
        // Given
        UUID ruleId = UUID.randomUUID();
        when(pricingRuleRepository.findById(eq(ruleId))).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            pricingService.deletePricingRule(ruleId, testUser));
    }

    @Test
    void testDeletePricingRuleWrongUser() throws Exception {
        // Given
        UUID ruleId = UUID.randomUUID();
        User otherUser = new User();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(otherUser, UUID.randomUUID());
        
        Company otherCompany = new Company();
        Field companyIdField = Company.class.getDeclaredField("id");
        companyIdField.setAccessible(true);
        companyIdField.set(otherCompany, UUID.randomUUID());
        otherCompany.setName("Other Company");
        otherUser.setCompany(otherCompany);

        DeliveryPricingRule rule = new DeliveryPricingRule(otherCompany, "Test Rule", BigDecimal.ONE);
        setRuleId(rule, ruleId);

        when(pricingRuleRepository.findById(eq(ruleId))).thenReturn(Optional.of(rule));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            pricingService.deletePricingRule(ruleId, testUser));
    }

    @Test
    void testGetUserPricingRules() {
        // Given
        List<DeliveryPricingRule> rules = Arrays.asList(
            new DeliveryPricingRule(testCompany, "Rule 1", BigDecimal.ONE),
            new DeliveryPricingRule(testCompany, "Rule 2", BigDecimal.valueOf(2))
        );

        when(pricingRuleRepository.findByCompanyAndIsActive(testCompany, true)).thenReturn(rules);

        // When
        List<DeliveryPricingRule> result = pricingService.getUserPricingRules(testUser);

        // Then
        assertEquals(2, result.size());
        verify(pricingRuleRepository).findByCompanyAndIsActive(testCompany, true);
    }

    @Test
    void testCalculateDeliveryFeeWithCustomFee() {
        // Given
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setDeliveryFee(BigDecimal.valueOf(5.00));

        // When
        BigDecimal result = pricingService.calculateDeliveryFee(testUser, request);

        // Then
        assertEquals(BigDecimal.valueOf(5.00), result);
        verify(pricingRuleRepository, never()).findApplicableRules(any(), any(), any());
    }

    @Test
    void testCalculateDeliveryFeeWithPricingRule() {
        // Given
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setDeliveryProvince("Phnom Penh");
        request.setDeliveryDistrict("Chamkarmon");
        request.setEstimatedValue(BigDecimal.valueOf(50));

        DeliveryPricingRule rule = new DeliveryPricingRule(testCompany, "Phnom Penh Rule", BigDecimal.valueOf(2.00));
        rule.setProvince("Phnom Penh");
        rule.setPriority(10);

        when(pricingRuleRepository.findApplicableRules(testCompany, "Phnom Penh", "Chamkarmon"))
            .thenReturn(Arrays.asList(rule));

        // When
        BigDecimal result = pricingService.calculateDeliveryFee(testUser, request);

        // Then
        assertEquals(BigDecimal.valueOf(2.00).setScale(2), result.setScale(2));
        verify(pricingRuleRepository).findApplicableRules(testCompany, "Phnom Penh", "Chamkarmon");
    }

    @Test
    void testCalculateDeliveryFeeWithHighValueSurcharge() {
        // Given
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setDeliveryProvince("Phnom Penh");
        request.setEstimatedValue(BigDecimal.valueOf(150)); // Above threshold

        DeliveryPricingRule rule = new DeliveryPricingRule(testCompany, "High Value Rule", BigDecimal.valueOf(2.00));
        rule.setHighValueThreshold(BigDecimal.valueOf(100));
        rule.setHighValueSurcharge(BigDecimal.valueOf(5.00));

        when(pricingRuleRepository.findApplicableRules(testCompany, "Phnom Penh", null))
            .thenReturn(Arrays.asList(rule));

        // When
        BigDecimal result = pricingService.calculateDeliveryFee(testUser, request);

        // Then
        assertEquals(BigDecimal.valueOf(7.00).setScale(2), result.setScale(2)); // 2.00 + 5.00
    }

    @Test
    void testCalculateDeliveryFeeDefaultCalculation() {
        // Given
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setPickupProvince("Phnom Penh");
        request.setDeliveryProvince("Phnom Penh");
        request.setEstimatedValue(BigDecimal.valueOf(50));

        when(pricingRuleRepository.findApplicableRules(testCompany, "Phnom Penh", null))
            .thenReturn(Arrays.asList());

        // When
        BigDecimal result = pricingService.calculateDeliveryFee(testUser, request);

        // Then
        assertEquals(BigDecimal.valueOf(2.00).setScale(2), result.setScale(2)); // Default Phnom Penh rate
    }

    @Test
    void testCalculateDeliveryFeeDefaultWithProvinceMultiplier() {
        // Given
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setPickupProvince("Kandal");
        request.setDeliveryProvince("Phnom Penh");
        request.setEstimatedValue(BigDecimal.valueOf(50));

        when(pricingRuleRepository.findApplicableRules(testCompany, "Phnom Penh", null))
            .thenReturn(Arrays.asList());

        // When
        BigDecimal result = pricingService.calculateDeliveryFee(testUser, request);

        // Then
        assertEquals(BigDecimal.valueOf(3.00).setScale(2), result.setScale(2)); // 2.00 * 1.5
    }

    @Test
    void testCalculateDeliveryFeeFreeModel() {
        // Given
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setDeliveryFeeModel("FREE");

        when(pricingRuleRepository.findApplicableRules(testCompany, null, null))
            .thenReturn(Arrays.asList());

        // When
        BigDecimal result = pricingService.calculateDeliveryFee(testUser, request);

        // Then
        assertEquals(BigDecimal.ZERO.setScale(2), result.setScale(2));
    }

    private void setRuleId(DeliveryPricingRule rule, UUID id) {
        try {
            Field idField = DeliveryPricingRule.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(rule, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set rule ID", e);
        }
    }
}