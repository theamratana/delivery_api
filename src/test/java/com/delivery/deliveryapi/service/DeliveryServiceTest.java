package com.delivery.deliveryapi.service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.delivery.deliveryapi.controller.DeliveryController.CreateDeliveryRequest;
import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.DeliveryItem;
import com.delivery.deliveryapi.model.District;
import com.delivery.deliveryapi.model.Province;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.CompanyRepository;
import com.delivery.deliveryapi.repo.DeliveryItemRepository;
import com.delivery.deliveryapi.repo.DeliveryPhotoRepository;
import com.delivery.deliveryapi.repo.ProductRepository;
import com.delivery.deliveryapi.repo.UserRepository;

class DeliveryServiceTest {

    @Mock
    private DeliveryItemRepository deliveryItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DeliveryPhotoRepository deliveryPhotoRepository;

    @Mock
    private DeliveryPricingService deliveryPricingService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testPickupAutoDetectionFromSender() throws Exception {
        // Arrange
        Province province = new Province();
        Field provinceIdField = Province.class.getDeclaredField("id");
        provinceIdField.setAccessible(true);
        provinceIdField.set(province, UUID.randomUUID());
        province.setName("Bangkok");

        District district = new District();
        Field districtIdField = District.class.getDeclaredField("id");
        districtIdField.setAccessible(true);
        districtIdField.set(district, UUID.randomUUID());
        district.setName("Siam");
        district.setProvince(province);

        Company company = new Company();
        Field companyIdField = Company.class.getDeclaredField("id");
        companyIdField.setAccessible(true);
        companyIdField.set(company, UUID.randomUUID());
        company.setAddress("123 Main St");
        company.setDistrict(district);

        User sender = new User();
        Field senderIdField = User.class.getDeclaredField("id");
        senderIdField.setAccessible(true);
        senderIdField.set(sender, UUID.randomUUID());
        sender.setCompany(company); // Set the company on sender

        User receiver = new User();
        Field receiverIdField = User.class.getDeclaredField("id");
        receiverIdField.setAccessible(true);
        receiverIdField.set(receiver, UUID.randomUUID());

        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setReceiverPhone("0812345678");
        request.setDeliveryType("COMPANY");
        request.setCompanyName("Test Company");
        request.setCompanyPhone("021234567");
        request.setItemDescription("Test Item");
        // Pickup fields are null - should auto-populate from sender
        request.setPickupAddress(null);
        request.setPickupProvince(null);
        request.setPickupDistrict(null);
        request.setDeliveryAddress("456 Delivery St");
        request.setDeliveryProvince("Bangkok");
        request.setDeliveryDistrict("Sukhumvit");
        request.setEstimatedValue(BigDecimal.valueOf(1000));

        DeliveryItem savedDelivery = new DeliveryItem();
        savedDelivery.setId(UUID.randomUUID());

        when(userRepository.findByPhoneE164("0812345678")).thenReturn(Optional.of(receiver));
        when(companyRepository.findByName("Test Company")).thenReturn(Optional.of(company));
        when(deliveryPricingService.calculateDeliveryFee(any(User.class), any(CreateDeliveryRequest.class))).thenReturn(BigDecimal.valueOf(50));
        when(deliveryItemRepository.save(any(DeliveryItem.class))).thenReturn(savedDelivery);

        // Act
        DeliveryItem result = deliveryService.createDelivery(sender, request);

        // Assert
        assertNotNull(result);
        verify(deliveryItemRepository).save(argThat(delivery -> {
            // Verify pickup fields were auto-populated from sender
            assertEquals("123 Main St", delivery.getPickupAddress());
            assertEquals("Bangkok", delivery.getPickupProvince());
            assertEquals("Siam", delivery.getPickupDistrict());
            return true;
        }));
    }

    @Test
    void testPickupUsesRequestValuesWhenProvided() throws Exception {
        // Arrange
        Province province = new Province();
        Field provinceIdField = Province.class.getDeclaredField("id");
        provinceIdField.setAccessible(true);
        provinceIdField.set(province, UUID.randomUUID());
        province.setName("Bangkok");

        District district = new District();
        Field districtIdField = District.class.getDeclaredField("id");
        districtIdField.setAccessible(true);
        districtIdField.set(district, UUID.randomUUID());
        district.setName("Siam");
        district.setProvince(province);

        Company company = new Company();
        Field companyIdField = Company.class.getDeclaredField("id");
        companyIdField.setAccessible(true);
        companyIdField.set(company, UUID.randomUUID());
        company.setAddress("123 Main St");
        company.setDistrict(district);

        User sender = new User();
        Field senderIdField = User.class.getDeclaredField("id");
        senderIdField.setAccessible(true);
        senderIdField.set(sender, UUID.randomUUID());
        sender.setCompany(company); // Set the company on sender

        User receiver = new User();
        Field receiverIdField = User.class.getDeclaredField("id");
        receiverIdField.setAccessible(true);
        receiverIdField.set(receiver, UUID.randomUUID());

        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setReceiverPhone("0812345678");
        request.setDeliveryType("COMPANY");
        request.setCompanyName("Test Company");
        request.setCompanyPhone("021234567");
        request.setItemDescription("Test Item");
        // Pickup fields provided in request - should use these instead of sender's
        request.setPickupAddress("789 Pickup St");
        request.setPickupProvince("Chiang Mai");
        request.setPickupDistrict("Old City");
        request.setDeliveryAddress("456 Delivery St");
        request.setDeliveryProvince("Bangkok");
        request.setDeliveryDistrict("Sukhumvit");
        request.setEstimatedValue(BigDecimal.valueOf(1000));

        DeliveryItem savedDelivery = new DeliveryItem();
        savedDelivery.setId(UUID.randomUUID());

        when(userRepository.findByPhoneE164("0812345678")).thenReturn(Optional.of(receiver));
        when(companyRepository.findByName("Test Company")).thenReturn(Optional.of(company));
        when(deliveryPricingService.calculateDeliveryFee(any(User.class), any(CreateDeliveryRequest.class))).thenReturn(BigDecimal.valueOf(50));
        when(deliveryItemRepository.save(any(DeliveryItem.class))).thenReturn(savedDelivery);

        // Act
        DeliveryItem result = deliveryService.createDelivery(sender, request);

        // Assert
        assertNotNull(result);
        verify(deliveryItemRepository).save(argThat(delivery -> {
            // Verify pickup fields used request values, not sender's
            assertEquals("789 Pickup St", delivery.getPickupAddress());
            assertEquals("Chiang Mai", delivery.getPickupProvince());
            assertEquals("Old City", delivery.getPickupDistrict());
            return true;
        }));
    }
}