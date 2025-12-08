package com.delivery.deliveryapi.repo;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delivery.deliveryapi.model.ExchangeRate;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {
    
    /**
     * Find the latest active exchange rate for a currency pair and company.
     * If company-specific rate not found, falls back to global rate (company_id = null).
     */
    @Query("SELECT er FROM ExchangeRate er WHERE er.fromCurrency = :fromCurrency " +
           "AND er.toCurrency = :toCurrency AND er.isActive = true " +
           "AND er.effectiveDate <= :asOfDate " +
           "AND (er.company.id = :companyId OR er.company IS NULL) " +
           "ORDER BY CASE WHEN er.company.id = :companyId THEN 0 ELSE 1 END, " +
           "er.effectiveDate DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRateForCompany(
        @Param("fromCurrency") String fromCurrency,
        @Param("toCurrency") String toCurrency,
        @Param("companyId") UUID companyId,
        @Param("asOfDate") OffsetDateTime asOfDate
    );

    /**
     * Find the latest active exchange rate for a currency pair (global only).
     */
    @Query("SELECT er FROM ExchangeRate er WHERE er.fromCurrency = :fromCurrency " +
           "AND er.toCurrency = :toCurrency AND er.isActive = true " +
           "AND er.company IS NULL " +
           "AND er.effectiveDate <= :asOfDate " +
           "ORDER BY er.effectiveDate DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRate(
        @Param("fromCurrency") String fromCurrency,
        @Param("toCurrency") String toCurrency,
        @Param("asOfDate") OffsetDateTime asOfDate
    );

    /**
     * Find the current active exchange rate (as of now).
     */
    default Optional<ExchangeRate> findCurrentRate(String fromCurrency, String toCurrency) {
        return findLatestRate(fromCurrency, toCurrency, OffsetDateTime.now());
    }

    /**
     * Find the current active exchange rate for a company (as of now).
     */
    default Optional<ExchangeRate> findCurrentRateForCompany(String fromCurrency, String toCurrency, UUID companyId) {
        return findLatestRateForCompany(fromCurrency, toCurrency, companyId, OffsetDateTime.now());
    }
}
