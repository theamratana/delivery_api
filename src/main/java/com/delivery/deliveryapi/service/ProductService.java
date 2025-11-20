package com.delivery.deliveryapi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.Product;
import com.delivery.deliveryapi.model.ProductCategory;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.ProductCategoryRepository;
import com.delivery.deliveryapi.repo.ProductRepository;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    public ProductService(ProductRepository productRepository,
                         ProductCategoryRepository productCategoryRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
    }

    @Transactional
    public Product createProductFromDelivery(User sender, String itemDescription, BigDecimal itemValue, BigDecimal deliveryFee) {
        log.info("Creating product from delivery for sender: {}", sender.getId());

        Company company = sender.getCompany();
        if (company == null) {
            throw new IllegalArgumentException("User must be assigned to a company to create products");
        }

        // Create product from delivery details
        Product product = new Product();
        product.setCompany(company);
        product.setName(itemDescription.length() > 100 ? itemDescription.substring(0, 100) : itemDescription);
        product.setDescription(itemDescription);
        product.setDefaultPrice(itemValue);
        product.setCategory(getDefaultCategory()); // Default category
        product.setIsActive(true);

        product = productRepository.save(product);
        log.info("Created product: {} for company: {}", product.getId(), company.getId());

        return product;
    }

    @Transactional
    public Product findOrCreateProduct(User sender, String itemDescription, BigDecimal itemValue) {
        Company company = sender.getCompany();
        if (company == null) {
            throw new IllegalArgumentException("User must be assigned to a company to use products");
        }

        // Try to find existing product by name and company
        List<Product> existingProducts = productRepository.searchProductsByName(company.getId(), itemDescription);
        if (!existingProducts.isEmpty()) {
            Product existing = existingProducts.get(0);
            // Update usage statistics
            existing.setUsageCount(existing.getUsageCount() + 1);
            existing.setLastUsedAt(OffsetDateTime.now());
            productRepository.save(existing);
            log.info("Using existing product: {} for company: {}", existing.getId(), company.getId());
            return existing;
        }

        // Create new product directly
        Product product = new Product();
        product.setCompany(company);
        product.setName(itemDescription.length() > 100 ? itemDescription.substring(0, 100) : itemDescription);
        product.setDescription(itemDescription);
        product.setDefaultPrice(itemValue);
        product.setCategory(getDefaultCategory()); // Default category
        product.setIsActive(true);

        product = productRepository.save(product);
        log.info("Created product: {} for company: {}", product.getId(), company.getId());

        return product;
    }

    public Product getProductById(UUID productId, User user) {
        Optional<Product> product = productRepository.findById(productId);
        if (product.isEmpty()) {
            throw new IllegalArgumentException("Product not found");
        }
        
        Product foundProduct = product.get();
        
        // Verify user has access to this product (same company)
        if (user.getCompany() == null || !foundProduct.getCompany().getId().equals(user.getCompany().getId())) {
            throw new IllegalArgumentException("Access denied to this product");
        }
        
        return foundProduct;
    }

    public List<Product> getCompanyProducts(User user) {
        if (user.getCompany() == null) {
            return List.of();
        }
        return productRepository.findByCompanyIdAndIsActiveTrueOrderByUsageCountDesc(user.getCompany().getId());
    }

    public List<Product> searchCompanyProducts(User user, String searchTerm) {
        if (user.getCompany() == null) {
            return List.of();
        }
        return productRepository.searchProductsByName(user.getCompany().getId(), searchTerm);
    }

    public List<Product> getProductSuggestions(User user, String query) {
        if (user.getCompany() == null) {
            return List.of();
        }
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return productRepository.findSuggestionsByName(user.getCompany().getId(), query.trim());
    }

    public List<Product> searchAllProducts(String searchTerm) {
        return productRepository.searchProductsByNameAll(searchTerm);
    }

    public List<Product> getAllProducts() {
        return productRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }

    @Transactional
    public Product updateProduct(UUID productId, User user, String name, String description,
                               ProductCategory category, BigDecimal defaultPrice,
                               BigDecimal buyingPrice, BigDecimal sellingPrice, Boolean isPublished) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required");
        }

        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found");
        }

        Product product = productOpt.get();

        // Check if user belongs to the same company
        if (!product.getCompany().getId().equals(user.getCompany().getId())) {
            throw new IllegalArgumentException("Access denied: Product belongs to different company");
        }

        if (name != null && !name.trim().isEmpty()) {
            product.setName(name.trim());
        }
        if (description != null) {
            product.setDescription(description);
        }
        if (category != null) {
            product.setCategory(category);
        }
        if (defaultPrice != null) {
            product.setDefaultPrice(defaultPrice);
        }
        if (buyingPrice != null) {
            product.setBuyingPrice(buyingPrice);
        }
        if (sellingPrice != null) {
            product.setSellingPrice(sellingPrice);
        }
        if (isPublished != null) {
            product.setIsPublished(isPublished);
        }

        return productRepository.save(product);
    }

    @Transactional
    public Product createProduct(User user, String name, String description,
                                 ProductCategory category, BigDecimal defaultPrice,
                                 BigDecimal buyingPrice, BigDecimal sellingPrice, Boolean isPublished) {
        if (user.getCompany() == null) {
            throw new IllegalArgumentException("User must belong to a company to create products");
        }
        Product product = new Product();
        product.setCompany(user.getCompany());
        product.setName(name != null ? name.trim() : null);
        product.setDescription(description);
        product.setCategory(category != null ? category : getDefaultCategory());
        product.setDefaultPrice(defaultPrice != null ? defaultPrice : java.math.BigDecimal.ZERO);
        product.setBuyingPrice(buyingPrice != null ? buyingPrice : java.math.BigDecimal.ZERO);
        product.setSellingPrice(sellingPrice != null ? sellingPrice : java.math.BigDecimal.ZERO);
        product.setIsPublished(isPublished != null ? isPublished : Boolean.FALSE);
        product.setIsActive(true);
        return productRepository.save(product);
    }

    @Transactional
    public void deactivateProduct(UUID productId, User user) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required");
        }

        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found");
        }

        Product product = productOpt.get();

        // Check if user belongs to the same company
        if (!product.getCompany().getId().equals(user.getCompany().getId())) {
            throw new IllegalArgumentException("Access denied: Product belongs to different company");
        }

        product.setIsActive(false);
        productRepository.save(product);
    }

    private ProductCategory getDefaultCategory() {
        return productCategoryRepository.findByCode("OTHER")
                .orElseThrow(() -> new IllegalStateException("Default 'OTHER' category not found"));
    }
}