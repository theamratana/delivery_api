package com.delivery.deliveryapi.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

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
            log.debug("getProduct: currentUser={}, role={}, companyId={} requesting productId={}", 
                    currentUser.getId(), currentUser.getUserRole(), currentUser.getCompany() != null ? currentUser.getCompany().getId() : "null", productId);
            Product product = productService.getProductById(productId, currentUser);
            return ResponseEntity.ok(ProductDTO.fromProduct(product));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            log.error("Failed to create product", e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/search")
    @Transactional
    public ResponseEntity<ProductSearchResponse> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean published,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            User currentUser = getCurrentUser();
            
            // Apply search filters
            List<Product> products;
            if (query != null && !query.trim().isEmpty()) {
                products = productService.searchCompanyProducts(currentUser, query.trim());
            } else {
                products = productService.getCompanyProducts(currentUser);
            }
            
            // Filter by category if provided
            if (category != null && !category.trim().isEmpty()) {
                String categoryLower = category.trim().toLowerCase();
                products = products.stream()
                    .filter(p -> p.getCategory() != null && 
                            (p.getCategory().getName().toLowerCase().contains(categoryLower) ||
                             p.getCategory().getCode().toLowerCase().contains(categoryLower)))
                    .collect(Collectors.toList());
            }
            
            // Filter by published status if provided
            if (published != null) {
                products = products.stream()
                    .filter(p -> published.equals(p.getIsPublished()))
                    .collect(Collectors.toList());
            }
            
            // Calculate pagination
            int total = products.size();
            int start = Math.min(page * limit, total);
            int end = Math.min(start + limit, total);
            
            List<ProductDTO> productDTOs = products.subList(start, end).stream()
                    .map(ProductDTO::fromProduct)
                    .collect(Collectors.toList());
            
            ProductSearchResponse response = new ProductSearchResponse();
            response.setProducts(productDTOs);
            response.setTotal(total);
            response.setPage(page);
            response.setLimit(limit);
            response.setHasMore(end < total);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Product search failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ProductSearchResponse());
        }
    }

    @GetMapping("/suggestions")
    @Transactional
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
            var role = currentUser.getUserRole();
            if (role == null || (role != com.delivery.deliveryapi.model.UserRole.OWNER
                    && role != com.delivery.deliveryapi.model.UserRole.MANAGER
                    && role != com.delivery.deliveryapi.model.UserRole.STAFF)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
                    Product updatedProduct = productService.updateProduct(productId, currentUser,
                            request.getName(), request.getDescription(),
                            request.getCategory(), request.getDefaultPrice(),
                                request.getBuyingPrice(), request.getSellingPrice(), request.getLastSellPrice(), request.getIsPublished(), request.getProductPhotos());
            return ResponseEntity.ok(updatedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody CreateProductRequest request) {
        try {
            User currentUser = getCurrentUser();
            log.debug("createProduct: currentUser={}", currentUser.getId());
            // Only allow product creation for Owner/Manager/Staff
            var role = currentUser.getUserRole();
            log.debug("createProduct: role={}", role);
            if (role == null || (role != com.delivery.deliveryapi.model.UserRole.OWNER
                    && role != com.delivery.deliveryapi.model.UserRole.MANAGER
                    && role != com.delivery.deliveryapi.model.UserRole.STAFF)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (currentUser.getCompany() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
                log.debug("createProduct: calling productService.createProduct");
                Product created = productService.createProduct(currentUser,
                    request.getName(), request.getDescription(), request.getCategory(), request.getDefaultPrice(), request.getBuyingPrice(), request.getSellingPrice(), request.getLastSellPrice(), request.getIsPublished(), request.getProductPhotos());
                log.debug("createProduct: productService.createProduct returned id={}", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(ProductDTO.fromProduct(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{productId}/photos")
    public ResponseEntity<ProductDTO> addPhotoToProduct(@PathVariable UUID productId, @RequestBody AddPhotoRequest request) {
        try {
            User currentUser = getCurrentUser();
            var role = currentUser.getUserRole();
            if (role == null || (role != com.delivery.deliveryapi.model.UserRole.OWNER
                    && role != com.delivery.deliveryapi.model.UserRole.MANAGER
                    && role != com.delivery.deliveryapi.model.UserRole.STAFF)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
            Product updated = productService.addPhotoToProduct(productId, currentUser, request.getImageRef());
            return ResponseEntity.ok(ProductDTO.fromProduct(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{productId}/photos/{photoId}")
    public ResponseEntity<ProductDTO> removePhotoFromProduct(@PathVariable UUID productId, @PathVariable UUID photoId) {
        try {
            User currentUser = getCurrentUser();
            var role = currentUser.getUserRole();
            if (role == null || (role != com.delivery.deliveryapi.model.UserRole.OWNER
                    && role != com.delivery.deliveryapi.model.UserRole.MANAGER
                    && role != com.delivery.deliveryapi.model.UserRole.STAFF)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
            Product updated = productService.removePhotoFromProduct(productId, currentUser, photoId);
            return ResponseEntity.ok(ProductDTO.fromProduct(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{productId}/deactivate")
    public ResponseEntity<String> deactivateProduct(@PathVariable UUID productId) {
        try {
            User currentUser = getCurrentUser();
            var role = currentUser.getUserRole();
            if (role == null || (role != com.delivery.deliveryapi.model.UserRole.OWNER
                    && role != com.delivery.deliveryapi.model.UserRole.MANAGER
                    && role != com.delivery.deliveryapi.model.UserRole.STAFF)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized");
            }
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
        
        @JsonProperty("buyingPrice")
        private java.math.BigDecimal buyingPrice;

        @JsonProperty("sellingPrice")
        private java.math.BigDecimal sellingPrice;
        
        @JsonProperty("lastSellPrice")
        private java.math.BigDecimal lastSellPrice;

        @JsonProperty("isPublished")
        private Boolean isPublished;

        @JsonProperty("productPhotos")
        private java.util.List<String> productPhotos;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public ProductCategory getCategory() { return category; }
        public void setCategory(ProductCategory category) { this.category = category; }

        public java.math.BigDecimal getDefaultPrice() { return defaultPrice; }
        public void setDefaultPrice(java.math.BigDecimal defaultPrice) { this.defaultPrice = defaultPrice; }
        public java.math.BigDecimal getBuyingPrice() { return buyingPrice; }
        public void setBuyingPrice(java.math.BigDecimal buyingPrice) { this.buyingPrice = buyingPrice; }
        public java.math.BigDecimal getSellingPrice() { return sellingPrice; }
        public void setSellingPrice(java.math.BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
        public java.math.BigDecimal getLastSellPrice() { return lastSellPrice; }
        public void setLastSellPrice(java.math.BigDecimal lastSellPrice) { this.lastSellPrice = lastSellPrice; }
        public Boolean getIsPublished() { return isPublished; }
        public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }
        public java.util.List<String> getProductPhotos() { return productPhotos; }
        public void setProductPhotos(java.util.List<String> productPhotos) { this.productPhotos = productPhotos; }
    }

    public static class CreateProductRequest {
        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("category")
        private ProductCategory category;

        @JsonProperty("defaultPrice")
        private java.math.BigDecimal defaultPrice;

        @JsonProperty("buyingPrice")
        private java.math.BigDecimal buyingPrice;

        @JsonProperty("sellingPrice")
        private java.math.BigDecimal sellingPrice;
        
        @JsonProperty("lastSellPrice")
        private java.math.BigDecimal lastSellPrice;

        @JsonProperty("isPublished")
        private Boolean isPublished;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public ProductCategory getCategory() { return category; }
        public void setCategory(ProductCategory category) { this.category = category; }
        public java.math.BigDecimal getDefaultPrice() { return defaultPrice; }
        public void setDefaultPrice(java.math.BigDecimal defaultPrice) { this.defaultPrice = defaultPrice; }
        public java.math.BigDecimal getBuyingPrice() { return buyingPrice; }
        public void setBuyingPrice(java.math.BigDecimal buyingPrice) { this.buyingPrice = buyingPrice; }
        public java.math.BigDecimal getSellingPrice() { return sellingPrice; }
        public void setSellingPrice(java.math.BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
        public java.math.BigDecimal getLastSellPrice() { return lastSellPrice; }
        public void setLastSellPrice(java.math.BigDecimal lastSellPrice) { this.lastSellPrice = lastSellPrice; }
        public Boolean getIsPublished() { return isPublished; }
        public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }

        @JsonProperty("productPhotos")
        private java.util.List<String> productPhotos;

        public java.util.List<String> getProductPhotos() { return productPhotos; }
        public void setProductPhotos(java.util.List<String> productPhotos) { this.productPhotos = productPhotos; }
    }

    public static class AddPhotoRequest {
        @JsonProperty("imageRef")
        private String imageRef;
        public String getImageRef() { return imageRef; }
        public void setImageRef(String imageRef) { this.imageRef = imageRef; }
    }
    
    public static class ProductSearchResponse {
        @JsonProperty("products")
        private List<ProductDTO> products;
        
        @JsonProperty("total")
        private Integer total;
        
        @JsonProperty("page")
        private Integer page;
        
        @JsonProperty("limit")
        private Integer limit;
        
        @JsonProperty("hasMore")
        private Boolean hasMore;
        
        public ProductSearchResponse() {
            this.products = new java.util.ArrayList<>();
            this.total = 0;
            this.page = 0;
            this.limit = 20;
            this.hasMore = false;
        }
        
        public List<ProductDTO> getProducts() { return products; }
        public void setProducts(List<ProductDTO> products) { this.products = products; }
        
        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
        
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
        
        public Boolean getHasMore() { return hasMore; }
        public void setHasMore(Boolean hasMore) { this.hasMore = hasMore; }
    }
}