package com.delivery.deliveryapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.delivery.deliveryapi.controller.DeliveryController.DeliveryItemPayload;
import com.delivery.deliveryapi.model.DeliveryItem;
import com.delivery.deliveryapi.model.DeliveryPackage;
import com.delivery.deliveryapi.model.DeliveryPackageStatus;
import com.delivery.deliveryapi.model.Product;
import com.delivery.deliveryapi.model.User;

import java.util.ArrayList;

@SpringBootTest
public class DeliveryServicePackageTests {

    @Autowired
    private DeliveryService deliveryService;

    @MockBean
    private com.delivery.deliveryapi.repo.DeliveryItemRepository deliveryItemRepository;

    @MockBean
    private com.delivery.deliveryapi.repo.UserRepository userRepository;

    @MockBean
    private com.delivery.deliveryapi.repo.CompanyRepository companyRepository;

    @MockBean
    private com.delivery.deliveryapi.repo.DeliveryPhotoRepository deliveryPhotoRepository;

    @MockBean
    private DeliveryPricingService deliveryPricingService;

    @MockBean
    private ProductService productService;

    @MockBean
    private com.delivery.deliveryapi.repo.ProductRepository productRepository;

    @MockBean
    private com.delivery.deliveryapi.repo.DeliveryPackageRepository deliveryPackageRepository;

    private User sender;

    @BeforeEach
    public void setup() {
        // Spring will autowire deliveryService
        sender = new User();
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sender, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Ensure sender has a company (DeliveryService expects sender.getCompany() != null in product lookup)
        com.delivery.deliveryapi.model.Company c = new com.delivery.deliveryapi.model.Company();
        try {
            java.lang.reflect.Field cid = com.delivery.deliveryapi.model.Company.class.getDeclaredField("id");
            cid.setAccessible(true);
            cid.set(c, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        c.setName("SenderCo");
        sender.setCompany(c);
    }

    @Test
    public void appendItemsToBatch_whenPackageNotFound_throws() {
        UUID batchId = UUID.randomUUID();
        when(deliveryPackageRepository.findById(batchId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            deliveryService.appendItemsToBatch(sender, batchId, List.of());
        });
    }

    @Test
    public void appendItemsToBatch_whenPackageNotAppendable_throws() {
        UUID batchId = UUID.randomUUID();
        DeliveryPackage pkg = new DeliveryPackage();
        pkg.setId(batchId);
        pkg.setStatus(DeliveryPackageStatus.PICKED_UP);
        when(deliveryPackageRepository.findById(batchId)).thenReturn(Optional.of(pkg));

        assertThrows(IllegalArgumentException.class, () -> {
            deliveryService.appendItemsToBatch(sender, batchId, List.of());
        });
    }

    @Test
    public void appendItemsToBatch_success_createsItems_and_keepsFee() {
        UUID batchId = UUID.randomUUID();
        DeliveryPackage pkg = new DeliveryPackage();
        pkg.setId(batchId);
        pkg.setStatus(DeliveryPackageStatus.CREATED);
        pkg.setDeliveryFee(BigDecimal.valueOf(12.34));
        when(deliveryPackageRepository.findById(batchId)).thenReturn(Optional.of(pkg));

        // existing context item
        DeliveryItem context = new DeliveryItem();
        context.setId(UUID.randomUUID());
        context.setSender(sender);
        context.setDeliveryFee(pkg.getDeliveryFee());
        context.setBatchId(batchId);
        when(deliveryItemRepository.findByBatchId(batchId)).thenReturn(List.of(context));

        // payload with productName -> create product
        DeliveryItemPayload payload = new DeliveryItemPayload();
        payload.setProductName("Test Product");
        payload.setItemDescription("desc");
        payload.setEstimatedValue(BigDecimal.TEN);
        payload.setQuantity(2);

        Product createdProduct = new Product();
        createdProduct.setId(UUID.randomUUID());
        when(productRepository.searchProductsByName(sender.getCompany() != null ? sender.getCompany().getId() : null, "Test Product"))
            .thenReturn(new ArrayList<>());
        when(productService.createProductFromDelivery(any(), any(), any(), any())).thenReturn(createdProduct);

        // capture saved items
        when(deliveryItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = deliveryService.appendItemsToBatch(sender, batchId, List.of(payload));

        // verify one item created
        assertEquals(1, result.size());
        DeliveryItem created = result.get(0);
        assertEquals(pkg.getDeliveryFee(), created.getDeliveryFee());
        assertEquals(batchId, created.getBatchId());
        assertEquals(2, created.getQuantity());

        // ensure save was called
        verify(deliveryItemRepository).save(any());
    }
}
