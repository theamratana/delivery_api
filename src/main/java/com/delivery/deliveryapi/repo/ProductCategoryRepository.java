package com.delivery.deliveryapi.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.delivery.deliveryapi.model.ProductCategory;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {

    Optional<ProductCategory> findByCode(String code);

    List<ProductCategory> findByIsActiveTrueOrderBySortOrderAsc();

    @Query("SELECT pc FROM ProductCategory pc WHERE pc.isActive = true ORDER BY pc.sortOrder ASC, pc.name ASC")
    List<ProductCategory> findActiveCategoriesOrdered();

    @Query("SELECT pc FROM ProductCategory pc ORDER BY pc.sortOrder ASC, pc.name ASC")
    List<ProductCategory> findAllOrdered();

    boolean existsByCode(String code);
}