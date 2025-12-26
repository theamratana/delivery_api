package com.delivery.deliveryapi.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delivery.deliveryapi.model.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByCompanyIdAndIsActiveTrueOrderByCreatedAtDesc(UUID companyId);

    List<Product> findByCompanyIdAndCategoryIdAndIsActiveTrueOrderByCreatedAtDesc(UUID companyId, UUID categoryId);

    @Query("SELECT p FROM Product p WHERE p.company.id = :companyId AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findMostUsedProducts(@Param("companyId") UUID companyId);

    @Query("SELECT p FROM Product p WHERE p.company.id = :companyId AND p.isActive = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY p.createdAt DESC")
    List<Product> searchProductsByName(@Param("companyId") UUID companyId, @Param("searchTerm") String searchTerm);

    List<Product> findByIsActiveTrueOrderByCreatedAtDesc();

    @Query("SELECT p FROM Product p WHERE p.company.id = :companyId AND p.isActive = true AND LOWER(p.name) LIKE LOWER(CONCAT(:query, '%')) ORDER BY p.createdAt DESC")
    List<Product> findSuggestionsByName(@Param("companyId") UUID companyId, @Param("query") String query);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY p.createdAt DESC")
    List<Product> searchProductsByNameAll(@Param("searchTerm") String searchTerm);
}