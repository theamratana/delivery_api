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
import com.delivery.deliveryapi.model.Image;
import com.delivery.deliveryapi.model.Product;
import com.delivery.deliveryapi.model.ProductImage;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.ImageRepository;
import com.delivery.deliveryapi.repo.ProductImageRepository;
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

    @MockBean
    private ImageRepository imageRepository;

    @MockBean
    private ProductImageRepository productImageRepository;

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
    void testCreateProductFromDeliverySetsLastSellPrice() {
        when(productCategoryRepository.findByCode("OTHER")).thenReturn(Optional.of(new com.delivery.deliveryapi.model.ProductCategory()));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        BigDecimal itemValue = new BigDecimal("12.34");
        Product p = productService.createProductFromDelivery(user, "From Delivery", itemValue, BigDecimal.ZERO);
        assertNotNull(p);
        assertEquals(itemValue, p.getDefaultPrice());
        assertEquals(itemValue, p.getLastSellPrice());
    }

    @Test
    void testAddPhotoToProduct() {
        UUID pId = product.getId();
        when(productRepository.findById(pId)).thenReturn(Optional.of(product));
        Image image = new Image();
        image.setId(UUID.randomUUID());
        image.setCompany(company);
        when(imageRepository.findById(image.getId())).thenReturn(Optional.of(image));
        when(productImageRepository.findByProductIdOrderByPhotoIndexAsc(pId)).thenReturn(java.util.Collections.emptyList());
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.addPhotoToProduct(pId, user, image.getId().toString());
        assertNotNull(updated);
        verify(productImageRepository, times(1)).save(any(ProductImage.class));
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testRemovePhotoFromProduct() {
        ProductImage pi = new ProductImage();
        pi.setId(UUID.randomUUID());
        pi.setProduct(product);
        Image image = new Image();
        image.setId(UUID.randomUUID());
        image.setUploader(user);
        pi.setImage(image);
        when(productImageRepository.findById(pi.getId())).thenReturn(Optional.of(pi));
        doNothing().when(productImageRepository).delete(pi);
        when(productImageRepository.findByProductIdOrderByPhotoIndexAsc(product.getId())).thenReturn(java.util.Collections.emptyList());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.removePhotoFromProduct(product.getId(), user, pi.getId());
        assertNotNull(result);
        verify(productImageRepository, times(1)).delete(pi);
    }
}
