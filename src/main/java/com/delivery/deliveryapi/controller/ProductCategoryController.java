package com.delivery.deliveryapi.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.model.ProductCategory;
import com.delivery.deliveryapi.service.ProductCategoryService;
import com.fasterxml.jackson.annotation.JsonProperty;

@RestController
@RequestMapping("/product-categories")
public class ProductCategoryController {

    private static final Logger log = LoggerFactory.getLogger(ProductCategoryController.class);

    private final ProductCategoryService productCategoryService;

    public ProductCategoryController(ProductCategoryService productCategoryService) {
        this.productCategoryService = productCategoryService;
    }

    @GetMapping
    public ResponseEntity<List<ProductCategory>> getAllCategories() {
        try {
            List<ProductCategory> categories = productCategoryService.getAllActiveCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<List<ProductCategory>> getAllCategoriesIncludingInactive() {
        try {
            List<ProductCategory> categories = productCategoryService.getAllCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<ProductCategory> createCategory(@RequestBody CreateCategoryRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.info("Creating category - Auth: {}, Authorities: {}", 
                auth != null ? auth.getName() : "null", 
                auth != null ? auth.getAuthorities() : "null");
                
            ProductCategory category = productCategoryService.createCategory(
                    request.getCode(),
                    request.getName(),
                    request.getKhmerName(),
                    request.getSortOrder()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(category);
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument when creating category: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        } catch (Exception e) {
            log.error("Error creating category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<ProductCategory> updateCategory(@PathVariable UUID categoryId,
                                                         @RequestBody UpdateCategoryRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.info("Updating category {} - Auth: {}, Authorities: {}", 
                categoryId,
                auth != null ? auth.getName() : "null", 
                auth != null ? auth.getAuthorities() : "null");
                
            ProductCategory category = productCategoryService.updateCategory(
                    categoryId,
                    request.getCode(),
                    request.getName(),
                    request.getKhmerName(),
                    request.getSortOrder(),
                    request.getIsActive()
            );
            return ResponseEntity.ok(category);
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument when updating category: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        } catch (Exception e) {
            log.error("Error updating category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/{categoryId}/deactivate")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<String> deactivateCategory(@PathVariable UUID categoryId) {
        try {
            productCategoryService.deactivateCategory(categoryId);
            return ResponseEntity.ok("Category deactivated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to deactivate category");
        }
    }

    @PostMapping("/initialize-defaults")
    public ResponseEntity<String> initializeDefaults() {
        try {
            productCategoryService.initializeDefaultCategories();
            return ResponseEntity.ok("Default categories initialized successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to initialize default categories: " + e.getMessage());
        }
    }

    public static class CreateCategoryRequest {
        @JsonProperty("code")
        private String code;

        @JsonProperty("name")
        private String name;

        @JsonProperty("khmerName")
        private String khmerName;

        @JsonProperty("sortOrder")
        private Integer sortOrder;

        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getKhmerName() { return khmerName; }
        public void setKhmerName(String khmerName) { this.khmerName = khmerName; }

        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UpdateCategoryRequest {
        @JsonProperty("code")
        private String code;

        @JsonProperty("name")
        private String name;

        @JsonProperty("khmerName")
        private String khmerName;

        @JsonProperty("sortOrder")
        private Integer sortOrder;

        @JsonProperty("isActive")
        private Boolean isActive;

        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getKhmerName() { return khmerName; }
        public void setKhmerName(String khmerName) { this.khmerName = khmerName; }

        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
}