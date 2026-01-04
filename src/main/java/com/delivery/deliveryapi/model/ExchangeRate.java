package com.delivery.deliveryapi.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Exchange rate for currency conversion.
 * Company-specific exchange rates tracked via TenantAuditableEntity.
 * Default: 1 USD = 4000 KHR
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "exchange_rates",
    indexes = {
        @Index(name = "idx_exchange_rates_effective_date", columnList = "effective_date"),
        @Index(name = "idx_exchange_rates_from_to", columnList = "from_currency, to_currency")
    }
)
public class ExchangeRate extends TenantAuditableEntity {

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

}
