package com.delivery.deliveryapi.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.deliveryapi.model.ProductCategory;
import com.delivery.deliveryapi.repo.ProductCategoryRepository;

@Service
public class ProductCategoryService {

    private static final Logger log = LoggerFactory.getLogger(ProductCategoryService.class);

    private final ProductCategoryRepository productCategoryRepository;

    public ProductCategoryService(ProductCategoryRepository productCategoryRepository) {
        this.productCategoryRepository = productCategoryRepository;
    }

    public List<ProductCategory> getAllActiveCategories() {
        return productCategoryRepository.findActiveCategoriesOrdered();
    }

    public List<ProductCategory> getAllCategories() {
        return productCategoryRepository.findAllOrdered();
    }

    public Optional<ProductCategory> findByCode(String code) {
        return productCategoryRepository.findByCode(code);
    }

    public Optional<ProductCategory> findById(java.util.UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return productCategoryRepository.findById(id);
    }

    @Transactional
    public void initializeDefaultCategories() {
        createCategoryIfNotExists("ELECTRONICS", "Electronics", "អេឡិចត្រូនិច", 1);
        createCategoryIfNotExists("CLOTHING", "Clothing", "សម្លៀកបំពាក់", 2);
        createCategoryIfNotExists("FOOD", "Food", "អាហារ", 3);
        createCategoryIfNotExists("BOOKS", "Books", "សៀវភៅ", 4);
        createCategoryIfNotExists("COSMETICS", "Cosmetics", "គ្រឿងសម្អាង", 5);
        createCategoryIfNotExists("MEDICINE", "Medicine", "ឱសថ", 6);
        createCategoryIfNotExists("DOCUMENTS", "Documents", "ឯកសារ", 7);
        createCategoryIfNotExists("OTHER", "Other", "ផ្សេងៗ", 99);

        log.info("Default product categories initialized");
    }

    private void createCategoryIfNotExists(String code, String name, String khmerName, int sortOrder) {
        if (!productCategoryRepository.existsByCode(code)) {
            ProductCategory category = new ProductCategory(code, name, khmerName, sortOrder);
            productCategoryRepository.save(category);
            log.info("Created category: {} ({})", code, name);
        }
    }

    @Transactional
    public ProductCategory createCategory(String code, String name, String khmerName, Integer sortOrder) {
        if (productCategoryRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Category code already exists: " + code);
        }

        ProductCategory category = new ProductCategory(code, name, khmerName, sortOrder);
        return productCategoryRepository.save(category);
    }

    @Transactional
    public ProductCategory updateCategory(java.util.UUID categoryId, String name, String khmerName, Integer sortOrder, Boolean isActive) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID is required");
        }

        Optional<ProductCategory> categoryOpt = productCategoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Category not found");
        }

        ProductCategory category = categoryOpt.get();

        if (name != null && !name.trim().isEmpty()) {
            category.setName(name.trim());
        }
        if (khmerName != null) {
            category.setKhmerName(khmerName);
        }
        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }
        if (isActive != null) {
            category.setIsActive(isActive);
        }

        return productCategoryRepository.save(category);
    }

    @Transactional
    public void deactivateCategory(java.util.UUID categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID is required");
        }

        Optional<ProductCategory> categoryOpt = productCategoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Category not found");
        }

        ProductCategory category = categoryOpt.get();
        category.setIsActive(false);
        productCategoryRepository.save(category);
    }
}