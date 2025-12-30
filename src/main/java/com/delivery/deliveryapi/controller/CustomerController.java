package com.delivery.deliveryapi.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

import com.delivery.deliveryapi.model.District;
import com.delivery.deliveryapi.model.Province;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserType;
import com.delivery.deliveryapi.repo.DistrictRepository;
import com.delivery.deliveryapi.repo.ProvinceRepository;
import com.delivery.deliveryapi.repo.UserRepository;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;
    private final ProvinceRepository provinceRepository;

    public CustomerController(UserRepository userRepository, DistrictRepository districtRepository,
                             ProvinceRepository provinceRepository) {
        this.userRepository = userRepository;
        this.districtRepository = districtRepository;
        this.provinceRepository = provinceRepository;
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> listCustomers(@RequestParam(required = false) String search) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getCompany() == null) {
                return ResponseEntity.badRequest().build();
            }

            List<User> customers;
            if (search != null && !search.trim().isEmpty()) {
                customers = userRepository.searchCustomersByCompany(
                    currentUser.getCompany().getId(),
                    UserType.CUSTOMER,
                    search.trim()
                );
            } else {
                customers = userRepository.findByCompanyIdAndUserType(
                    currentUser.getCompany().getId(),
                    UserType.CUSTOMER
                );
            }

            var response = customers.stream()
                .map(c -> new CustomerResponse(c, provinceRepository, districtRepository))
                .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable UUID id) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getCompany() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            Optional<User> optCustomer = userRepository.findById(id);
            if (optCustomer.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User customer = optCustomer.get();

            // Verify ownership
            if (customer.getCompany() == null || 
                !customer.getCompany().getId().equals(currentUser.getCompany().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(new CustomerResponse(customer, provinceRepository, districtRepository));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Object> updateCustomer(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateCustomerRequest req) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getCompany() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User is not part of any company"));
            }

            Optional<User> optCustomer = userRepository.findById(id);
            if (optCustomer.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User customer = optCustomer.get();

            // Verify ownership
            if (customer.getCompany() == null || 
                !customer.getCompany().getId().equals(currentUser.getCompany().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only edit customers from your company"));
            }

            // Update fields
            if (req.name != null && !req.name.trim().isEmpty()) {
                customer.setDisplayName(req.name.trim());
            }
            if (req.address != null) {
                customer.setDefaultAddress(req.address.trim());
            }
            if (req.provinceId != null) {
                customer.setDefaultProvinceId(req.provinceId);
            }
            if (req.districtId != null) {
                customer.setDefaultDistrictId(req.districtId);
            }

            userRepository.save(customer);

            return ResponseEntity.ok(new CustomerResponse(customer, provinceRepository, districtRepository));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Object> createCustomer(@RequestBody @Valid CreateCustomerRequest req) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getCompany() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User is not part of any company"));
            }

            // Check if customer already exists for this company
            Optional<User> existing = userRepository.findByPhoneE164AndCompanyAndUserType(
                req.phone.trim(),
                currentUser.getCompany(),
                UserType.CUSTOMER
            );

            if (existing.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Customer with this phone number already exists"));
            }

            // Create new customer
            User customer = new User();
            customer.setPhoneE164(req.phone.trim());
            if (req.name != null && !req.name.trim().isEmpty()) {
                customer.setDisplayName(req.name.trim());
            }
            customer.setCompany(currentUser.getCompany());
            customer.setUserType(UserType.CUSTOMER);
            customer.setIncomplete(true);
            customer.setActive(true);
            
            if (req.address != null && !req.address.trim().isEmpty()) {
                customer.setDefaultAddress(req.address.trim());
            }
            if (req.provinceId != null) {
                customer.setDefaultProvinceId(req.provinceId);
            }
            if (req.districtId != null) {
                customer.setDefaultDistrictId(req.districtId);
            }

            customer = userRepository.save(customer);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CustomerResponse(customer, provinceRepository, districtRepository));
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
    public static class UpdateCustomerRequest {
        @JsonProperty("name")
        public String name;

        @JsonProperty("address")
        public String address;

        @JsonProperty("provinceId")
        public UUID provinceId;

        @JsonProperty("districtId")
        public UUID districtId;
    }

    public static class CreateCustomerRequest {
        @JsonProperty("phone")
        @NotBlank(message = "Phone is required")
        public String phone;

        @JsonProperty("name")
        public String name;

        @JsonProperty("address")
        public String address;

        @JsonProperty("provinceId")
        public UUID provinceId;

        @JsonProperty("districtId")
        public UUID districtId;
    }

    public static class CustomerResponse {
        @JsonProperty("id")
        public String id;

        @JsonProperty("phone")
        public String phone;

        @JsonProperty("name")
        public String name;

        @JsonProperty("address")
        public String address;

        @JsonProperty("provinceName")
        public String provinceName;

        @JsonProperty("districtName")
        public String districtName;

        @JsonProperty("totalDeliveries")
        public long totalDeliveries;

        @JsonProperty("lastDeliveryDate")
        public String lastDeliveryDate;

        public CustomerResponse(User customer, ProvinceRepository provinceRepo, DistrictRepository districtRepo) {
            this.id = customer.getId().toString();
            this.phone = customer.getPhoneE164();
            this.name = customer.getDisplayName();
            this.address = customer.getDefaultAddress();
            
            // Load province and district names
            if (customer.getDefaultProvinceId() != null) {
                this.provinceName = provinceRepo.findById(customer.getDefaultProvinceId())
                    .map(Province::getName)
                    .orElse(null);
            }
            if (customer.getDefaultDistrictId() != null) {
                this.districtName = districtRepo.findById(customer.getDefaultDistrictId())
                    .map(District::getName)
                    .orElse(null);
            }

            // Delivery module removed - set defaults
            this.totalDeliveries = 0L;
            this.lastDeliveryDate = null;
        }
    }
}
