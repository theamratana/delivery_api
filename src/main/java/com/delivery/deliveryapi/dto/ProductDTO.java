package com.delivery.deliveryapi.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.delivery.deliveryapi.model.Product;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductDTO {
    
    @JsonProperty("id")
    private UUID id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("categoryId")
    private UUID categoryId;
    
    @JsonProperty("categoryName")
    private String categoryName;
    
    @JsonProperty("categoryKhmerName")
    private String categoryKhmerName;
    
    @JsonProperty("buyingPrice")
    private BigDecimal buyingPrice;
    
    @JsonProperty("sellingPrice")
    private BigDecimal sellingPrice;
    
    @JsonProperty("fullPrice")
    private BigDecimal fullPrice;
    
    @JsonProperty("isPublished")
    private Boolean isPublished;
    
    @JsonProperty("attributes")
    private String attributes;
    
    @JsonProperty("productPhotos")
    private java.util.List<ImageDTO> productPhotos;

    public static class ImageDTO {
        @JsonProperty("id")
        private String id;
        @JsonProperty("url")
        private String url;
        public ImageDTO() {}
        public ImageDTO(String id, String url) { this.id = id; this.url = url; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
    
    @JsonProperty("isActive")
    private Boolean isActive;
    
    @JsonProperty("companyId")
    private UUID companyId;
    
    @JsonProperty("companyName")
    private String companyName;

    public ProductDTO() {}

    public static ProductDTO fromProduct(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setBuyingPrice(product.getBuyingPrice());
        dto.setSellingPrice(product.getSellingPrice());
        dto.setFullPrice(product.getFullPrice());
        dto.setIsPublished(product.getIsPublished());
        dto.setAttributes(product.getAttributes());
        dto.setIsActive(product.getIsActive());
        
        if (product.getCompany() != null) {
            dto.setCompanyId(product.getCompany().getId());
            dto.setCompanyName(product.getCompany().getName());
        }
        
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
            dto.setCategoryKhmerName(product.getCategory().getKhmerName());
        }
        if (product.getProductPhotos() != null) {
            java.util.List<ImageDTO> photos = product.getProductPhotos().stream()
                    .map(photo -> new ImageDTO(photo.getId().toString(), photo.getPhotoUrl()))
                    .toList();
            dto.setProductPhotos(photos);
        }
        
        return dto;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryKhmerName() { return categoryKhmerName; }
    public void setCategoryKhmerName(String categoryKhmerName) { this.categoryKhmerName = categoryKhmerName; }

    public BigDecimal getBuyingPrice() { return buyingPrice; }
    public void setBuyingPrice(BigDecimal buyingPrice) { this.buyingPrice = buyingPrice; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public BigDecimal getFullPrice() { return fullPrice; }
    public void setFullPrice(BigDecimal fullPrice) { this.fullPrice = fullPrice; }

    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }

    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public java.util.List<ImageDTO> getProductPhotos() { return productPhotos; }
    public void setProductPhotos(java.util.List<ImageDTO> productPhotos) { this.productPhotos = productPhotos; }
}
