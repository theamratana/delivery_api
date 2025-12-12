package com.delivery.deliveryapi.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.delivery.deliveryapi.controller.DeliveryController.CreateDeliveryRequest;
import com.delivery.deliveryapi.controller.DeliveryController.DeliveryItemPayload;
import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.DeliveryItem;
import com.delivery.deliveryapi.model.DeliveryPackage;
import com.delivery.deliveryapi.model.DeliveryPackageStatus;
import com.delivery.deliveryapi.model.Product;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserType;
import com.delivery.deliveryapi.repo.CompanyRepository;
import com.delivery.deliveryapi.repo.DeliveryItemRepository;
import com.delivery.deliveryapi.repo.DeliveryPhotoRepository;
import com.delivery.deliveryapi.repo.ProductRepository;
import com.delivery.deliveryapi.repo.UserRepository;

@SpringBootTest
class DeliveryServiceCreateAndAppendTests {

    @Autowired
    DeliveryService deliveryService;

    @MockBean
    DeliveryItemRepository deliveryItemRepository;

    @MockBean
    UserRepository userRepository;

    @MockBean
    CompanyRepository companyRepository;

    @MockBean
    DeliveryPhotoRepository deliveryPhotoRepository;

    @MockBean
    DeliveryPricingService deliveryPricingService;

    @MockBean
    ProductService productService;

    @MockBean
    ProductRepository productRepository;

    @MockBean
    com.delivery.deliveryapi.repo.DeliveryPackageRepository deliveryPackageRepository;

    private User buildSender() {
        User sender = new User();
        sender.setUserType(UserType.COMPANY);
        Company c = new Company();
        c.setName("SenderCo");
        sender.setCompany(c);
        return sender;
    }

    @Test
    void createDelivery_createsBatchAndItem_success() {
        User sender = buildSender();

        // Prepare request with one item
        CreateDeliveryRequest req = new CreateDeliveryRequest();
        req.setReceiverPhone("+1234567890");
        req.setDeliveryType("DRIVER");
        req.setDriverPhone("+1999888777");
        req.setDeliveryAddress("Somewhere");
        req.setDeliveryProvince("Prov");
        req.setDeliveryDistrict("Dist");

        DeliveryItemPayload payload = new DeliveryItemPayload();
        payload.setItemDescription("Test item");
        payload.setProductName("Widget");
        payload.setEstimatedValue(new BigDecimal("10.00"));
        req.setItems(List.of(payload));

        // Mocks
        when(userRepository.findByPhoneE164(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // companyRepository not used for DRIVER deliveries in this test

        when(deliveryPricingService.calculateDeliveryFee(any(User.class), any(com.delivery.deliveryapi.controller.DeliveryController.CreateDeliveryRequest.class)))
            .thenReturn(new BigDecimal("5.00"));

        Product createdProduct = new Product();
        createdProduct.setId(UUID.randomUUID());
        createdProduct.setCompany(sender.getCompany());
        when(productService.createProductFromDelivery(any(User.class), anyString(), any(), any())).thenReturn(createdProduct);
        doNothing().when(productRepository).flush();

        when(deliveryPackageRepository.save(any(DeliveryPackage.class))).thenAnswer(inv -> {
            DeliveryPackage p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setStatus(DeliveryPackageStatus.CREATED);
            return p;
        });

        when(deliveryItemRepository.save(any(DeliveryItem.class))).thenAnswer(inv -> {
            DeliveryItem d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        DeliveryItem result = deliveryService.createDelivery(sender, req);
        assertNotNull(result);
        assertNotNull(result.getBatchId());
        verify(deliveryItemRepository, times(1)).save(any(DeliveryItem.class));
    }

    @Test
    void appendItemsToBatch_appendsSuccessfully() {
        User sender = buildSender();

        UUID batchId = UUID.randomUUID();

        DeliveryPackage pkg = new DeliveryPackage();
        pkg.setId(batchId);
        pkg.setStatus(DeliveryPackageStatus.CREATED);

        DeliveryItem context = new DeliveryItem();
        context.setId(UUID.randomUUID());
        context.setSender(sender);
        context.setDeliveryFee(new BigDecimal("7.50"));

        when(deliveryPackageRepository.findById(batchId)).thenReturn(Optional.of(pkg));
        when(deliveryItemRepository.findByBatchId(batchId)).thenReturn(List.of(context));

        // Prepare payload to append
        DeliveryItemPayload payload = new DeliveryItemPayload();
        payload.setItemDescription("Appended item");
        payload.setProductName("Gadget");
        payload.setEstimatedValue(new BigDecimal("3.25"));

        when(productRepository.searchProductsByName(any(), anyString())).thenReturn(List.of());
        Product createdProduct = new Product();
        createdProduct.setId(UUID.randomUUID());
        createdProduct.setCompany(sender.getCompany());
        when(productService.createProductFromDelivery(any(User.class), anyString(), any(), any())).thenReturn(createdProduct);
        doNothing().when(productRepository).flush();

        when(deliveryItemRepository.save(any(DeliveryItem.class))).thenAnswer(inv -> {
            DeliveryItem d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        var created = deliveryService.appendItemsToBatch(sender, batchId, List.of(payload));
        assertNotNull(created);
        assertEquals(1, created.size());
        assertEquals(batchId, created.get(0).getBatchId());
    }
}
