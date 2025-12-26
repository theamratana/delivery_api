package com.delivery.deliveryapi.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.Product;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.ProductRepository;
import com.delivery.deliveryapi.repo.ProductCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private ProductCategoryRepository productCategoryRepository;

    private Company company;
    private User user;
    private Product product;

    @BeforeEach
    void setup() {
        company = new Company();
        user = new User();
        user.setCompany(company);

        product = new Product();
        // product and user share the same company instance - IDs will be set by the entity defaults
        // product ID remains the default
        product.setCompany(company);
    }

    @Test
    void testCreateProductFromDelivery() {
        when(productCategoryRepository.findByCode("OTHER")).thenReturn(Optional.of(new com.delivery.deliveryapi.model.ProductCategory()));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        BigDecimal itemValue = new BigDecimal("12.34");
        Product p = productService.createProductFromDelivery(user, "From Delivery", itemValue, BigDecimal.ZERO);
        assertNotNull(p);
        assertEquals("From Delivery", p.getName());
    }
}
