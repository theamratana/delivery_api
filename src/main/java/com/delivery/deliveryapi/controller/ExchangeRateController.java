package com.delivery.deliveryapi.controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.model.ExchangeRate;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserRole;
import com.delivery.deliveryapi.repo.ExchangeRateRepository;
import com.delivery.deliveryapi.repo.UserRepository;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/exchange-rates")
public class ExchangeRateController {

    private final ExchangeRateRepository exchangeRateRepository;
    private final UserRepository userRepository;

    public ExchangeRateController(ExchangeRateRepository exchangeRateRepository, UserRepository userRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<ExchangeRateResponse>> listExchangeRates(
            @RequestParam(required = false) String fromCurrency,
            @RequestParam(required = false) String toCurrency) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getCompany() == null) {
                return ResponseEntity.badRequest().build();
            }

            List<ExchangeRate> rates = exchangeRateRepository.findAll(Sort.by(Sort.Direction.DESC, "effectiveDate"));
            
            // Filter by company and optional currency parameters
            var filteredRates = rates.stream()
                .filter(rate -> rate.getCompany() != null && 
                               rate.getCompany().getId().equals(currentUser.getCompany().getId()))
                .filter(rate -> fromCurrency == null || rate.getFromCurrency().equalsIgnoreCase(fromCurrency))
                .filter(rate -> toCurrency == null || rate.getToCurrency().equalsIgnoreCase(toCurrency))
                .map(ExchangeRateResponse::new)
                .collect(Collectors.toList());

            return ResponseEntity.ok(filteredRates);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/current")
    public ResponseEntity<ExchangeRateResponse> getCurrentRate(
            @RequestParam(defaultValue = "USD") String fromCurrency,
            @RequestParam(defaultValue = "KHR") String toCurrency) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getCompany() == null) {
                return ResponseEntity.badRequest().build();
            }

            Optional<ExchangeRate> optRate = exchangeRateRepository.findCurrentRateForCompany(
                fromCurrency.toUpperCase(),
                toCurrency.toUpperCase(),
                currentUser.getCompany().getId()
            );

            if (optRate.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(new ExchangeRateResponse(optRate.get()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(@PathVariable UUID id) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getCompany() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            Optional<ExchangeRate> optRate = exchangeRateRepository.findById(id);
            if (optRate.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ExchangeRate rate = optRate.get();

            // Verify ownership
            if (rate.getCompany() == null || 
                !rate.getCompany().getId().equals(currentUser.getCompany().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(new ExchangeRateResponse(rate));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Object> createExchangeRate(@RequestBody @Valid CreateExchangeRateRequest req) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getCompany() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User is not part of any company"));
            }

            // Check if user has appropriate role (OWNER or MANAGER)
            if (currentUser.getUserRole() != UserRole.OWNER && 
                currentUser.getUserRole() != UserRole.MANAGER) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only company owners or manager can create exchange rates"));
            }

            // Create new exchange rate
            ExchangeRate exchangeRate = new ExchangeRate();
            exchangeRate.setFromCurrency(req.fromCurrency.toUpperCase());
            exchangeRate.setToCurrency(req.toCurrency.toUpperCase());
            exchangeRate.setRate(req.rate);
            exchangeRate.setEffectiveDate(req.effectiveDate != null ? req.effectiveDate : OffsetDateTime.now());
            exchangeRate.setIsActive(req.isActive != null ? req.isActive : true);
            if (req.notes != null && !req.notes.trim().isEmpty()) {
                exchangeRate.setNotes(req.notes.trim());
            }
            exchangeRate.setCompany(currentUser.getCompany());

            exchangeRate = exchangeRateRepository.save(exchangeRate);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ExchangeRateResponse(exchangeRate));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Object> updateExchangeRate(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateExchangeRateRequest req) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getCompany() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User is not part of any company"));
            }

            // Check if user has appropriate role (OWNER or MANAGER)
            if (currentUser.getUserRole() != UserRole.OWNER && 
                currentUser.getUserRole() != UserRole.MANAGER) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only company owners or managers can update exchange rates"));
            }

            Optional<ExchangeRate> optRate = exchangeRateRepository.findById(id);
            if (optRate.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ExchangeRate rate = optRate.get();

            // Verify ownership
            if (rate.getCompany() == null || 
                !rate.getCompany().getId().equals(currentUser.getCompany().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only edit exchange rates for your company"));
            }

            // Update fields
            if (req.fromCurrency != null && !req.fromCurrency.trim().isEmpty()) {
                rate.setFromCurrency(req.fromCurrency.trim().toUpperCase());
            }
            if (req.toCurrency != null && !req.toCurrency.trim().isEmpty()) {
                rate.setToCurrency(req.toCurrency.trim().toUpperCase());
            }
            if (req.rate != null) {
                rate.setRate(req.rate);
            }
            if (req.effectiveDate != null) {
                rate.setEffectiveDate(req.effectiveDate);
            }
            if (req.isActive != null) {
                rate.setIsActive(req.isActive);
            }
            if (req.notes != null) {
                rate.setNotes(req.notes.trim());
            }

            exchangeRateRepository.save(rate);

            return ResponseEntity.ok(new ExchangeRateResponse(rate));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            throw new IllegalStateException("User not authenticated");
        }
        UUID userId = UUID.fromString(userIdStr);
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    // DTOs
    public static class CreateExchangeRateRequest {
        @JsonProperty("fromCurrency")
        @NotBlank(message = "From currency is required")
        public String fromCurrency;

        @JsonProperty("toCurrency")
        @NotBlank(message = "To currency is required")
        public String toCurrency;

        @JsonProperty("rate")
        @NotNull(message = "Rate is required")
        @DecimalMin(value = "0.0001", message = "Rate must be greater than 0")
        public BigDecimal rate;

        @JsonProperty("effectiveDate")
        public OffsetDateTime effectiveDate;

        @JsonProperty("isActive")
        public Boolean isActive;

        @JsonProperty("notes")
        public String notes;
    }

    public static class UpdateExchangeRateRequest {
        @JsonProperty("fromCurrency")
        public String fromCurrency;

        @JsonProperty("toCurrency")
        public String toCurrency;

        @JsonProperty("rate")
        @DecimalMin(value = "0.0001", message = "Rate must be greater than 0")
        public BigDecimal rate;

        @JsonProperty("effectiveDate")
        public OffsetDateTime effectiveDate;

        @JsonProperty("isActive")
        public Boolean isActive;

        @JsonProperty("notes")
        public String notes;
    }

    public static class ExchangeRateResponse {
        @JsonProperty("id")
        public String id;

        @JsonProperty("fromCurrency")
        public String fromCurrency;

        @JsonProperty("toCurrency")
        public String toCurrency;

        @JsonProperty("rate")
        public BigDecimal rate;

        @JsonProperty("effectiveDate")
        public OffsetDateTime effectiveDate;

        @JsonProperty("isActive")
        public Boolean isActive;

        @JsonProperty("notes")
        public String notes;

        @JsonProperty("companyId")
        public String companyId;

        @JsonProperty("createdAt")
        public OffsetDateTime createdAt;

        @JsonProperty("updatedAt")
        public OffsetDateTime updatedAt;

        public ExchangeRateResponse(ExchangeRate rate) {
            this.id = rate.getId().toString();
            this.fromCurrency = rate.getFromCurrency();
            this.toCurrency = rate.getToCurrency();
            this.rate = rate.getRate();
            this.effectiveDate = rate.getEffectiveDate();
            this.isActive = rate.getIsActive();
            this.notes = rate.getNotes();
            this.companyId = rate.getCompany() != null ? rate.getCompany().getId().toString() : null;
            this.createdAt = rate.getCreatedAt();
            this.updatedAt = rate.getUpdatedAt();
        }
    }
}
