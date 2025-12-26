package com.delivery.deliveryapi.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "products",
    indexes = {
        @Index(name = "idx_products_company_id", columnList = "company_id"),
        @Index(name = "idx_products_category", columnList = "category_id"),
        @Index(name = "idx_products_active", columnList = "is_active"),
        @Index(name = "idx_products_created_at", columnList = "created_at")
    }
)
public class Product extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Column(name = "buying_price", precision = 10, scale = 2)
    private BigDecimal buyingPrice = java.math.BigDecimal.ZERO;

    @Column(name = "selling_price", precision = 10, scale = 2)
    private BigDecimal sellingPrice = java.math.BigDecimal.ZERO;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "dimensions", length = 50)
    private String dimensions; // e.g., "10x5x2cm"

    @Column(name = "attributes", columnDefinition = "jsonb")
    private String attributes; // JSONB for flexible attributes like color, size, material, etc.

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("photoIndex ASC")
    private List<ProductPhoto> productPhotos = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    // Constructors
    public Product() {}

    public Product(Company company, String name, String description) {
        this.company = company;
        this.name = name;
        this.description = description;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public BigDecimal getBuyingPrice() { return buyingPrice; }
    public void setBuyingPrice(BigDecimal buyingPrice) { this.buyingPrice = buyingPrice; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }

    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }

    public List<ProductPhoto> getProductPhotos() { return productPhotos; }
    public void setProductPhotos(List<ProductPhoto> productPhotos) { this.productPhotos = productPhotos; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }
}