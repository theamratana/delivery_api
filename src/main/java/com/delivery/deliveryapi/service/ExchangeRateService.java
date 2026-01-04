package com.delivery.deliveryapi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.delivery.deliveryapi.model.ExchangeRate;
import com.delivery.deliveryapi.repo.ExchangeRateRepository;
import com.delivery.deliveryapi.service.base.BaseServiceImpl;

@Service
public class ExchangeRateService extends BaseServiceImpl<ExchangeRate, ExchangeRateRepository> {

    private static final BigDecimal DEFAULT_USD_TO_KHR = new BigDecimal("4000.0000");

    public ExchangeRateService(ExchangeRateRepository repository) {
        super(repository);
    }

    /**
     * Get the current exchange rate for a company.
     * Defaults to 4000 for USD->KHR if no rate is configured.
     */
    public BigDecimal getExchangeRateForCompany(String fromCurrency, String toCurrency, UUID companyId) {
        return getExchangeRateForCompanyAsOf(fromCurrency, toCurrency, companyId, OffsetDateTime.now());
    }

    /**
     * Get the exchange rate for a company as of a specific date/time.
     */
    public BigDecimal getExchangeRateForCompanyAsOf(String fromCurrency, String toCurrency, UUID companyId, OffsetDateTime asOfDate) {
        return repository.findLatestRateForCompany(fromCurrency, toCurrency, companyId, asOfDate)
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
}
