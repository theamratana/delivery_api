package com.delivery.deliveryapi.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "product_categories",
    indexes = {
        @Index(name = "idx_product_categories_code", columnList = "code", unique = true),
        @Index(name = "idx_product_categories_active", columnList = "is_active")
    }
)
public class ProductCategory extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name; // English name

    @Column(name = "khmer_name", length = 100)
    private String khmerName; // Khmer name

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    // Constructors
    public ProductCategory() {}

    public ProductCategory(String code, String name, String khmerName) {
        this.code = code;
        this.name = name;
        this.khmerName = khmerName;
    }

    public ProductCategory(String code, String name, String khmerName, Integer sortOrder) {
        this.code = code;
        this.name = name;
        this.khmerName = khmerName;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getKhmerName() { return khmerName; }
    public void setKhmerName(String khmerName) { this.khmerName = khmerName; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}