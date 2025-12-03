package com.delivery.deliveryapi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
     * Get the current exchange rate between two currencies.
     * Defaults to 4000 for USD->KHR if no rate is configured.
     */
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        return getExchangeRateAsOf(fromCurrency, toCurrency, OffsetDateTime.now());
    }

    /**
     * Get the exchange rate as of a specific date/time.
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
     * Convert an amount from one currency to another.
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            return null;
        }
        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(rate);
    }
}
