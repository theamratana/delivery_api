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
    
    @JsonProperty("defaultPrice")
    private BigDecimal defaultPrice;
    
    @JsonProperty("buyingPrice")
    private BigDecimal buyingPrice;
    
    @JsonProperty("sellingPrice")
    private BigDecimal sellingPrice;
    
    @JsonProperty("isPublished")
    private Boolean isPublished;
    
    @JsonProperty("weightKg")
    private BigDecimal weightKg;
    
    @JsonProperty("dimensions")
    private String dimensions;
    
    @JsonProperty("isActive")
    private Boolean isActive;
    
    @JsonProperty("usageCount")
    private Integer usageCount;
    
    @JsonProperty("lastUsedAt")
    private OffsetDateTime lastUsedAt;
    
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
        dto.setDefaultPrice(product.getDefaultPrice());
        dto.setBuyingPrice(product.getBuyingPrice());
        dto.setSellingPrice(product.getSellingPrice());
        dto.setIsPublished(product.getIsPublished());
        dto.setWeightKg(product.getWeightKg());
        dto.setDimensions(product.getDimensions());
        dto.setIsActive(product.getIsActive());
        dto.setUsageCount(product.getUsageCount());
        dto.setLastUsedAt(product.getLastUsedAt());
        
        if (product.getCompany() != null) {
            dto.setCompanyId(product.getCompany().getId());
            dto.setCompanyName(product.getCompany().getName());
        }
        
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
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

    public BigDecimal getDefaultPrice() { return defaultPrice; }
    public void setDefaultPrice(BigDecimal defaultPrice) { this.defaultPrice = defaultPrice; }

    public BigDecimal getBuyingPrice() { return buyingPrice; }
    public void setBuyingPrice(BigDecimal buyingPrice) { this.buyingPrice = buyingPrice; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }

    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(OffsetDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}
