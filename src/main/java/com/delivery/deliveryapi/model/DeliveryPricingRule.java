package com.delivery.deliveryapi.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery_pricing_rules",
    indexes = {
        @Index(name = "idx_pricing_rules_company_id", columnList = "company_id"),
        @Index(name = "idx_pricing_rules_province", columnList = "province"),
        @Index(name = "idx_pricing_rules_district", columnList = "district"),
        @Index(name = "idx_pricing_rules_active", columnList = "is_active")
    }
)
public class DeliveryPricingRule extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "province")
    private String province; // null means applies to all provinces

    @Column(name = "district")
    private String district; // null means applies to all districts in province

    @Column(name = "base_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal baseFee;

    @Column(name = "high_value_surcharge", precision = 10, scale = 2)
    private BigDecimal highValueSurcharge = BigDecimal.ZERO;

    @Column(name = "high_value_threshold", precision = 10, scale = 2)
    private BigDecimal highValueThreshold = BigDecimal.valueOf(100);

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0; // Higher priority rules are checked first

    // Constructors
    public DeliveryPricingRule() {}

    public DeliveryPricingRule(Company company, String ruleName, BigDecimal baseFee) {
        this.company = company;
        this.ruleName = ruleName;
        this.baseFee = baseFee;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public BigDecimal getBaseFee() { return baseFee; }
    public void setBaseFee(BigDecimal baseFee) { this.baseFee = baseFee; }

    public BigDecimal getHighValueSurcharge() { return highValueSurcharge; }
    public void setHighValueSurcharge(BigDecimal highValueSurcharge) { this.highValueSurcharge = highValueSurcharge; }

    public BigDecimal getHighValueThreshold() { return highValueThreshold; }
    public void setHighValueThreshold(BigDecimal highValueThreshold) { this.highValueThreshold = highValueThreshold; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}