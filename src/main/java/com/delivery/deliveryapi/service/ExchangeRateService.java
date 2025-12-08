package com.delivery.deliveryapi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.delivery.deliveryapi.model.ExchangeRate;
import com.delivery.deliveryapi.repo.ExchangeRateRepository;

@Service
public class ExchangeRateService {

    private static final BigDecimal DEFAULT_USD_TO_KHR = new BigDecimal("4000.0000");
    
    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    /**
     * Get the current exchange rate for a company.
     * Falls back to global rate if company has no specific rate.
     * Defaults to 4000 for USD->KHR if no rate is configured.
     */
    public BigDecimal getExchangeRateForCompany(String fromCurrency, String toCurrency, UUID companyId) {
        return getExchangeRateForCompanyAsOf(fromCurrency, toCurrency, companyId, OffsetDateTime.now());
    }

    /**
     * Get the exchange rate for a company as of a specific date/time.
     * Falls back to global rate if company has no specific rate.
     */
    public BigDecimal getExchangeRateForCompanyAsOf(String fromCurrency, String toCurrency, UUID companyId, OffsetDateTime asOfDate) {
        if (companyId == null) {
            return getExchangeRateAsOf(fromCurrency, toCurrency, asOfDate);
        }
        return exchangeRateRepository.findLatestRateForCompany(fromCurrency, toCurrency, companyId, asOfDate)
            .map(ExchangeRate::getRate)
            .orElseGet(() -> getDefaultRate(fromCurrency, toCurrency));
    }

    /**
     * Get the current exchange rate between two currencies (global rate only).
     * Defaults to 4000 for USD->KHR if no rate is configured.
     */
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        return getExchangeRateAsOf(fromCurrency, toCurrency, OffsetDateTime.now());
    }

    /**
     * Get the exchange rate as of a specific date/time (global rate only).
     */
    public BigDecimal getExchangeRateAsOf(String fromCurrency, String toCurrency, OffsetDateTime asOfDate) {
        return exchangeRateRepository.findLatestRate(fromCurrency, toCurrency, asOfDate)
            .map(ExchangeRate::getRate)
            .orElseGet(() -> getDefaultRate(fromCurrency, toCurrency));
    }

    /**
     * Get default exchange rate if no configured rate exists.
     */
    private BigDecimal getDefaultRate(String fromCurrency, String toCurrency) {
        if ("USD".equals(fromCurrency) && "KHR".equals(toCurrency)) {
            return DEFAULT_USD_TO_KHR;
        }
        // For other currency pairs, return 1:1 as default
        return BigDecimal.ONE;
    }

    /**
     * Convert an amount from one currency to another for a specific company.
     */
    public BigDecimal convertForCompany(BigDecimal amount, String fromCurrency, String toCurrency, UUID companyId) {
        if (amount == null) {
            return null;
        }
        BigDecimal rate = getExchangeRateForCompany(fromCurrency, toCurrency, companyId);
        return amount.multiply(rate);
    }

    /**
     * Convert an amount from one currency to another (using global rate).
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            return null;
        }
        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(rate);
    }
}
