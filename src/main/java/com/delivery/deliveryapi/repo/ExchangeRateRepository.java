package com.delivery.deliveryapi.repo;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delivery.deliveryapi.model.ExchangeRate;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID>, JpaSpecificationExecutor<ExchangeRate> {
    
    /**
     * Find the latest active exchange rate for a currency pair and company.
     */
    @Query("SELECT er FROM ExchangeRate er WHERE er.fromCurrency = :fromCurrency " +
           "AND er.toCurrency = :toCurrency AND er.isActive = true " +
           "AND er.effectiveDate <= :asOfDate " +
           "AND er.companyId = :companyId " +
           "ORDER BY er.effectiveDate DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRateForCompany(
        @Param("fromCurrency") String fromCurrency,
        @Param("toCurrency") String toCurrency,
        @Param("companyId") UUID companyId,
        @Param("asOfDate") OffsetDateTime asOfDate
    );

    /**
     * Find the current active exchange rate for a company (as of now).
     */
    default Optional<ExchangeRate> findCurrentRateForCompany(String fromCurrency, String toCurrency, UUID companyId) {
        return findLatestRateForCompany(fromCurrency, toCurrency, companyId, OffsetDateTime.now());
    }
}
