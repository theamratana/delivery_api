package com.delivery.deliveryapi.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.dto.ProductDTO;
import com.delivery.deliveryapi.model.Product;
import com.delivery.deliveryapi.model.ProductCategory;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.service.ProductService;
import com.fasterxml.jackson.annotation.JsonProperty;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    public ProductController(ProductService productService, UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Transactional
    public ResponseEntity<List<ProductDTO>> getCompanyProducts(@RequestParam(required = false) String search) {
        try {
            User currentUser = getCurrentUser();

            // Regular users see only their company's products
            List<Product> products;
            if (search != null && !search.trim().isEmpty()) {
                products = productService.searchCompanyProducts(currentUser, search.trim());
            } else {
                products = productService.getCompanyProducts(currentUser);
            }

            List<ProductDTO> productDTOs = products.stream()
                    .map(ProductDTO::fromProduct)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(productDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of());
        }
    }

    @GetMapping("/{productId}")
    @Transactional
    public ResponseEntity<ProductDTO> getProduct(@PathVariable UUID productId) {
        try {
            User currentUser = getCurrentUser();
            Product product = productService.getProductById(productId, currentUser);
            return ResponseEntity.ok(ProductDTO.fromProduct(product));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<ProductDTO>> getProductSuggestions(@RequestParam String query) {
        try {
            User currentUser = getCurrentUser();
            List<Product> suggestions = productService.getProductSuggestions(currentUser, query);
            List<ProductDTO> suggestionDTOs = suggestions.stream()
                    .map(ProductDTO::fromProduct)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(suggestionDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of());
        }
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID productId,
                                                @RequestBody UpdateProductRequest request) {
        try {
            User currentUser = getCurrentUser();
            Product updatedProduct = productService.updateProduct(productId, currentUser,
                    request.getName(), request.getDescription(),
                    request.getCategory(), request.getDefaultPrice());
            return ResponseEntity.ok(updatedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/{productId}/deactivate")
    public ResponseEntity<String> deactivateProduct(@PathVariable UUID productId) {
        try {
            User currentUser = getCurrentUser();
            productService.deactivateProduct(productId, currentUser);
            return ResponseEntity.ok("Product deactivated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to deactivate product");
        }
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            throw new IllegalArgumentException("User not authenticated");
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID is null");
        }
        var optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        return optUser.get();
    }

    public static class UpdateProductRequest {
        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("category")
        private ProductCategory category;

        @JsonProperty("defaultPrice")
        private java.math.BigDecimal defaultPrice;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public ProductCategory getCategory() { return category; }
        public void setCategory(ProductCategory category) { this.category = category; }

        public java.math.BigDecimal getDefaultPrice() { return defaultPrice; }
        public void setDefaultPrice(java.math.BigDecimal defaultPrice) { this.defaultPrice = defaultPrice; }
    }
}