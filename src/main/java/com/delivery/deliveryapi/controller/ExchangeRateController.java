package com.delivery.deliveryapi.controller;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.controller.base.BaseApiController;
import com.delivery.deliveryapi.model.ExchangeRate;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.ExchangeRateRepository;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.service.ExchangeRateService;

@RestController
@RequestMapping("/exchange-rates")
public class ExchangeRateController extends BaseApiController<ExchangeRate> {

    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateController(ExchangeRateService service, ExchangeRateRepository exchangeRateRepository, UserRepository userRepository) {
        super(service, userRepository);
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentRate(
            @RequestParam(defaultValue = "USD") String fromCurrency,
            @RequestParam(defaultValue = "KHR") String toCurrency) {
        try {
            User currentUser = getCurrentUser();
            UUID companyId = getCompanyId(currentUser);

            Optional<ExchangeRate> optRate = exchangeRateRepository.findCurrentRateForCompany(
                fromCurrency.toUpperCase(),
                toCurrency.toUpperCase(),
                companyId
            );

            if (optRate.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(optRate.get());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
