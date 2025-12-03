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
     * Find the latest active exchange rate for a currency pair.
     */
    @Query("SELECT er FROM ExchangeRate er WHERE er.fromCurrency = :fromCurrency " +
           "AND er.toCurrency = :toCurrency AND er.isActive = true " +
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
}
