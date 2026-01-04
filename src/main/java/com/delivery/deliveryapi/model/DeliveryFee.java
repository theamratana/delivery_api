package com.delivery.deliveryapi.model;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DeliveryFee entity for managing delivery pricing.
 * 
 * Supports hierarchical pricing with fallback logic:
 * 1. District-specific: province + district (most specific)
 * 2. Province-wide: province only (district = null)
 * 3. Default/Global: both province and district = null (fallback)
 * 
 * Examples:
 * - Default everywhere: province=null, district=null, fee=1.5
 * - Phnom Penh province-wide: province=PP, district=null, fee=2.0
 * - Chamkarmon exception: province=PP, district=Chamkarmon, fee=1.5
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "delivery_fees",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_delivery_fee_target_province_district",
            columnNames = {"target_company_id", "province_id", "district_id"}
        )
    },
    indexes = {
        @Index(name = "idx_delivery_fee_company", columnList = "company_id"),
        @Index(name = "idx_delivery_fee_target", columnList = "target_company_id"),
        @Index(name = "idx_delivery_fee_province", columnList = "province_id"),
        @Index(name = "idx_delivery_fee_active", columnList = "is_active"),
        @Index(name = "idx_delivery_fee_lookup", 
               columnList = "target_company_id, province_id, district_id"),
        @Index(name = "idx_delivery_fee_company_target", 
               columnList = "company_id, target_company_id")
    }
)
public class DeliveryFee extends TenantAuditableEntity {
    
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();
    
    // The company this fee applies to (can be same as company or a partner)
    @Column(name = "target_company_id", nullable = false)
    private UUID targetCompanyId;
    
    // Province: null for default/global pricing
    @Column(name = "province_id")
    private UUID provinceId;
    
    // District: null for province-wide pricing
    @Column(name = "district_id")
    private UUID districtId;
    
    @Column(name = "fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";
    
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
