package com.delivery.deliveryapi.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery.deliveryapi.model.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    List<ProductImage> findByProductIdOrderByPhotoIndexAsc(UUID productId);
}
