package com.delivery.deliveryapi.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Exchange rate for currency conversion.
 * Can be company-specific or system-wide (company_id = null).
 * Default: 1 USD = 4000 KHR
 */
@Entity
@Table(name = "exchange_rates",
    indexes = {
        @Index(name = "idx_exchange_rates_effective_date", columnList = "effective_date"),
        @Index(name = "idx_exchange_rates_from_to", columnList = "from_currency, to_currency")
    }
)
public class ExchangeRate extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency = "USD";

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency = "KHR";

    @Column(name = "rate", nullable = false, precision = 12, scale = 4)
    private BigDecimal rate = new BigDecimal("4000.0000"); // Default: 1 USD = 4000 KHR

    @Column(name = "effective_date", nullable = false)
    private OffsetDateTime effectiveDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }

    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public OffsetDateTime getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(OffsetDateTime effectiveDate) { this.effectiveDate = effectiveDate; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
}
