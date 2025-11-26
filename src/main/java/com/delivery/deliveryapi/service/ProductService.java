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
import com.delivery.deliveryapi.repo.ImageRepository;
import com.delivery.deliveryapi.repo.ProductCategoryRepository;
import com.delivery.deliveryapi.repo.ProductRepository;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ImageRepository imageRepository;
    private final com.delivery.deliveryapi.repo.ProductImageRepository productImageRepository;

    public ProductService(ProductRepository productRepository,
                         ProductCategoryRepository productCategoryRepository,
                         ImageRepository imageRepository,
                         com.delivery.deliveryapi.repo.ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.imageRepository = imageRepository;
        this.productImageRepository = productImageRepository;
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
        product.setLastSellPrice(itemValue);
        product.setLastSellPrice(itemValue);
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
                               BigDecimal buyingPrice, BigDecimal sellingPrice, BigDecimal lastSellPrice, Boolean isPublished,
                               java.util.List<String> productPhotos) {
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
        if (lastSellPrice != null) {
            product.setLastSellPrice(lastSellPrice);
        }
        if (isPublished != null) {
            product.setIsPublished(isPublished);
        }
        if (productPhotos != null) {
            validateProductPhotos(user, productPhotos);
            // productPhotos may be array of image URLs or image IDs; resolve to Image entities
            List<com.delivery.deliveryapi.model.ProductImage> mapped = mapToProductImages(product, productPhotos);
            product.setProductImages(mapped);
        }

        return productRepository.save(product);
    }

    @Transactional
    public Product createProduct(User user, String name, String description,
                                 ProductCategory category, BigDecimal defaultPrice,
                                 BigDecimal buyingPrice, BigDecimal sellingPrice, BigDecimal lastSellPrice, Boolean isPublished,
                                 java.util.List<String> productPhotos) {
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
        product.setLastSellPrice(lastSellPrice != null ? lastSellPrice : java.math.BigDecimal.ZERO);
        product.setIsPublished(isPublished != null ? isPublished : Boolean.FALSE);
        if (productPhotos != null) {
            validateProductPhotos(user, productPhotos);
            List<com.delivery.deliveryapi.model.ProductImage> mapped = mapToProductImages(product, productPhotos);
            product.setProductImages(mapped);
        } else {
            product.setProductImages(new java.util.ArrayList<>());
        }
        product.setIsActive(true);
        return productRepository.save(product);
    }

    private void validateProductPhotos(User user, java.util.List<String> productPhotos) {
        for (String url : productPhotos) {
            com.delivery.deliveryapi.model.Image image = findImageByRef(url);
            if (image == null) throw new IllegalArgumentException("Photo not found: " + url);
            // allow same-company or same-uploader
            boolean allowed = false;
            if (user.getCompany() != null && image.getCompany() != null && user.getCompany().getId().equals(image.getCompany().getId())) {
                allowed = true;
            }
            if (image.getUploader() != null && image.getUploader().getId().equals(user.getId())) {
                allowed = true;
            }
            if (!allowed) throw new IllegalArgumentException("Photo does not belong to your company or you: " + url);
        }
    }

    private com.delivery.deliveryapi.model.Image findImageByRef(String ref) {
        com.delivery.deliveryapi.model.Image image = null;
        java.util.Optional<java.util.UUID> idOpt = parseUuid(ref);
        if (idOpt.isPresent()) {
            image = imageRepository.findById(idOpt.get()).orElse(null);
        }
        if (image == null) {
            var opt = imageRepository.findByUrl(ref);
            if (opt.isPresent()) image = opt.get();
        }
        return image;
    }

    private java.util.Optional<java.util.UUID> parseUuid(String ref) {
        try {
            return java.util.Optional.of(java.util.UUID.fromString(ref));
        } catch (IllegalArgumentException ex) {
            // Not a UUID - treat as URL when not parseable
            return java.util.Optional.empty();
        }
    }

    private List<com.delivery.deliveryapi.model.ProductImage> mapToProductImages(Product product, java.util.List<String> productPhotos) {
        List<com.delivery.deliveryapi.model.ProductImage> images = new java.util.ArrayList<>();
        int idx = 0;
        for (String ref : productPhotos) {
            com.delivery.deliveryapi.model.Image image = null;
            // Try parse as UUID
            image = findImageByRef(ref);
            if (image == null) throw new IllegalArgumentException("Photo not found: " + ref);
            // Ownership already validated via validateProductPhotos
            com.delivery.deliveryapi.model.ProductImage pi = new com.delivery.deliveryapi.model.ProductImage();
            pi.setProduct(product);
            pi.setImage(image);
            pi.setPhotoIndex(idx++);
            images.add(pi);
        }
        return images;
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

    @Transactional
    public Product addPhotoToProduct(UUID productId, User user, String imageRef) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) throw new IllegalArgumentException("Product not found: " + productId);
        Product product = productOpt.get();
        // Check user company access
        if (!product.getCompany().getId().equals(user.getCompany().getId())) {
            throw new IllegalArgumentException("Access denied: Product belongs to different company");
        }
        validateProductPhotos(user, java.util.List.of(imageRef));
        com.delivery.deliveryapi.model.Image image = findImageByRef(imageRef);
        if (image == null) throw new IllegalArgumentException("Image not found: " + imageRef);
        int nextIndex = 0;
        var existing = productImageRepository.findByProductIdOrderByPhotoIndexAsc(product.getId());
        if (existing != null && !existing.isEmpty()) nextIndex = existing.get(existing.size() - 1).getPhotoIndex() + 1;
        com.delivery.deliveryapi.model.ProductImage pi = new com.delivery.deliveryapi.model.ProductImage();
        pi.setProduct(product);
        pi.setImage(image);
        pi.setPhotoIndex(nextIndex);
        productImageRepository.save(pi);
        // Refresh product images
        var updated = productImageRepository.findByProductIdOrderByPhotoIndexAsc(product.getId());
        product.setProductImages(updated);
        productRepository.save(product);
        return product;
    }

    @Transactional
    public Product removePhotoFromProduct(UUID productId, User user, UUID photoId) {
        Optional<com.delivery.deliveryapi.model.ProductImage> pio = productImageRepository.findById(photoId);
        if (pio.isEmpty()) throw new IllegalArgumentException("Photo not found: " + photoId);
        com.delivery.deliveryapi.model.ProductImage pi = pio.get();
        if (!pi.getProduct().getId().equals(productId)) throw new IllegalArgumentException("Photo does not belong to product");
        // Allow remove if user belongs to same company or uploaded the image
        boolean allowed = false;
        if (user.getCompany() != null && pi.getImage().getCompany() != null && user.getCompany().getId().equals(pi.getImage().getCompany().getId())) allowed = true;
        if (pi.getImage().getUploader() != null && pi.getImage().getUploader().getId().equals(user.getId())) allowed = true;
        if (!allowed) throw new IllegalArgumentException("User not allowed to remove this photo");
        productImageRepository.delete(pi);
        // Refresh product images and save
        var product = pi.getProduct();
        var updated = productImageRepository.findByProductIdOrderByPhotoIndexAsc(product.getId());
        product.setProductImages(updated);
        productRepository.save(product);
        return product;
    }

    private ProductCategory getDefaultCategory() {
        return productCategoryRepository.findByCode("OTHER")
                .orElseThrow(() -> new IllegalStateException("Default 'OTHER' category not found"));
    }
}