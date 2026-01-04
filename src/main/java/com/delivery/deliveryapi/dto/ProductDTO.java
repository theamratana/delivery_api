package com.delivery.deliveryapi.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.delivery.deliveryapi.model.Product;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
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

    @Data
    public static class ImageDTO {
        @JsonProperty("id")
        private String id;
        @JsonProperty("url")
        private String url;
        public ImageDTO() {}
        public ImageDTO(String id, String url) { this.id = id; this.url = url; }
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
}
