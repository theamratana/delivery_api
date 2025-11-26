package com.delivery.deliveryapi.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.Product;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.service.ProductService;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.model.UserRole;

class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductController productController;

    private User user;
    private Company company;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        company = new Company();
        user = new User();
        user.setCompany(company);
        user.setUserRole(UserRole.OWNER);
        when(userRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.of(user));
    }

    @Test
    void createProductWithPhoto_shouldReturnCreated() {
        // Arrange
        UUID userId = user.getId();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userId.toString());
        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        Product product = new Product();
        product.setCompany(company);
        product.setName("My Product");
        when(productService.createProduct(any(User.class), eq("My Product"), any(), any(), any(), any(), any(), any(), any(), anyList())).thenReturn(product);

        ProductController.CreateProductRequest req = new ProductController.CreateProductRequest();
        req.setName("My Product");
        req.setProductPhotos(java.util.List.of(UUID.randomUUID().toString()));

        ResponseEntity<?> resp;
        try {
            // ensure DTO mapping works as expected
            com.delivery.deliveryapi.dto.ProductDTO.fromProduct(product);
            resp = productController.createProduct(req);
            assertEquals(201, resp.getStatusCode().value());
            verify(productService, times(1)).createProduct(any(User.class), eq("My Product"), any(), any(), any(), any(), any(), any(), any(), anyList());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Controller threw exception: " + ex.getMessage());
        }
    }
}
