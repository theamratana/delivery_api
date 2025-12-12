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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.CompanyCategory;
import com.delivery.deliveryapi.model.District;
import com.delivery.deliveryapi.model.Province;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserRole;
import com.delivery.deliveryapi.repo.CompanyCategoryRepository;
import com.delivery.deliveryapi.repo.CompanyRepository;
import com.delivery.deliveryapi.repo.DistrictRepository;
import com.delivery.deliveryapi.repo.ProvinceRepository;
import com.delivery.deliveryapi.repo.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;
    private final ProvinceRepository provinceRepository;
    private final CompanyCategoryRepository companyCategoryRepository;

    public CompanyController(CompanyRepository companyRepository, UserRepository userRepository, 
                            DistrictRepository districtRepository, ProvinceRepository provinceRepository,
                            CompanyCategoryRepository companyCategoryRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.districtRepository = districtRepository;
        this.provinceRepository = provinceRepository;
        this.companyCategoryRepository = companyCategoryRepository;
    }

    public record UpdateCompanyRequest(
        @NotBlank(message = "Company name is required") String name,
        String address,
        String phoneNumber,
        UUID districtId,
        UUID provinceId,
        UUID categoryId
    ) {}

    public record CompanyResponse(
        UUID id,
        String name,
        String address,
        DistrictInfo district,
        boolean active
    ) {}

    public record DistrictInfo(
        UUID id,
        String name,
        String nameKh,
        ProvinceInfo province
    ) {}

    public record ProvinceInfo(
        UUID id,
        String name,
        String nameKh
    ) {}

    @GetMapping("/my")
    public ResponseEntity<Object> getMyCompany() {
        // Get current user
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = optUser.get();

        // Check if user has a company
        if (user.getCompany() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "User is not part of any company"));
        }

        Company company = user.getCompany();

        return ResponseEntity.ok(new CompanyBrowseResponse(company, districtRepository, provinceRepository, companyCategoryRepository));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getCompanyById(@PathVariable UUID id) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getCompany() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User is not part of any company"));
            }

            // Find the requested company
            Optional<Company> optCompany = companyRepository.findById(id);
            if (optCompany.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Company company = optCompany.get();

            // Check if user can view this company:
            // 1. It's their own company, OR
            // 2. It was created by their company
            boolean isOwnCompany = company.getId().equals(currentUser.getCompany().getId());
            boolean isCreatedByMyCompany = company.getCreatedByCompany() != null && 
                company.getCreatedByCompany().getId().equals(currentUser.getCompany().getId());
            
            if (!isOwnCompany && !isCreatedByMyCompany) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only view your own company or companies created by your company"));
            }

            return ResponseEntity.ok(new CompanyBrowseResponse(company, districtRepository, provinceRepository, companyCategoryRepository));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/my")
    @Transactional
    public ResponseEntity<Object> updateMyCompany(@RequestBody @Valid UpdateCompanyRequest req) {
        // Get current user
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = optUser.get();

        // Check if user has a company
        if (user.getCompany() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "User is not part of any company"));
        }

        // Check if user is OWNER of the company
        if (user.getUserRole() != UserRole.OWNER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Only company owners can update company information"));
        }

        Company company = user.getCompany();

        // Update company fields
        company.setName(req.name.trim());
        company.setAddress(req.address != null ? req.address.trim() : null);
        company.setPhoneNumber(req.phoneNumber != null ? req.phoneNumber.trim() : null);
        company.setDistrictId(req.districtId);
        company.setProvinceId(req.provinceId);
        company.setCategoryId(req.categoryId);
        company.setUpdatedByUser(user);

        companyRepository.save(company);

        return ResponseEntity.ok(new CompanyBrowseResponse(company, districtRepository, provinceRepository, companyCategoryRepository));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Object> updateDeliveryCompany(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateDeliveryCompanyRequest req) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getCompany() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User is not part of any company"));
            }

            // Find company to update
            Optional<Company> optCompany = companyRepository.findById(id);
            if (optCompany.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Company company = optCompany.get();

            // Check if user can edit this company:
            // 1. It's their own company (company.id == user's company.id), OR
            // 2. It was created by their company (created_by_company_id == user's company.id)
            boolean isOwnCompany = company.getId().equals(currentUser.getCompany().getId());
            boolean isCreatedByMyCompany = company.getCreatedByCompany() != null && 
                company.getCreatedByCompany().getId().equals(currentUser.getCompany().getId());
            
            if (!isOwnCompany && !isCreatedByMyCompany) {
                // Build detailed error message for debugging
                String debugInfo = String.format(
                    "Access denied. Company ID: %s, Your Company ID: %s, Created By Company ID: %s, Is Own: %s, Is Created By You: %s",
                    company.getId(),
                    currentUser.getCompany().getId(),
                    company.getCreatedByCompany() != null ? company.getCreatedByCompany().getId() : "null",
                    isOwnCompany,
                    isCreatedByMyCompany
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "error", "You can only edit your own company or companies created by your company",
                        "debug", debugInfo
                    ));
            }
            
            // For own company, also check if user is OWNER
            if (isOwnCompany && currentUser.getUserRole() != UserRole.OWNER) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only company owners can update their company information"));
            }

            // Update fields
            if (req.name() != null && !req.name().trim().isEmpty()) {
                company.setName(req.name().trim());
            }
            if (req.phoneNumber() != null) {
                company.setPhoneNumber(req.phoneNumber().trim());
            }
            if (req.address() != null) {
                company.setAddress(req.address().trim());
            }

            company.setDistrictId(req.districtId());
            company.setProvinceId(req.provinceId());
            company.setCategoryId(req.categoryId());
            company.setUpdatedByUser(currentUser);
            companyRepository.save(company);

            return ResponseEntity.ok(new CompanyBrowseResponse(company, districtRepository, provinceRepository, companyCategoryRepository));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/browse")
    public ResponseEntity<List<CompanyBrowseResponse>> browseCompanies(
            @RequestParam(required = false) String search) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getCompany() == null) {
                return ResponseEntity.badRequest().build();
            }

            List<Company> companies;
            if (search != null && !search.trim().isEmpty()) {
                companies = companyRepository.searchByCreatorCompanyAndName(
                    currentUser.getCompany().getId(), 
                    search.trim()
                );
            } else {
                companies = companyRepository.findByCreatorCompanyId(
                    currentUser.getCompany().getId()
                );
            }

            var response = companies.stream()
                .map(c -> new CompanyBrowseResponse(c, districtRepository, provinceRepository, companyCategoryRepository))
                .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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

    public record UpdateDeliveryCompanyRequest(
        String name,
        String phoneNumber,
        String address,
        UUID districtId,
        UUID provinceId,
        UUID categoryId
    ) {}

    public record CompanyBrowseResponse(
        String id,
        String name,
        String phoneNumber,
        String address,
        UUID categoryId,
        UUID provinceId,
        UUID districtId
    ) {
        public CompanyBrowseResponse(Company company, DistrictRepository districtRepository, 
                                    ProvinceRepository provinceRepository, CompanyCategoryRepository categorRepository) {
            this(
                company.getId().toString(),
                company.getName(),
                company.getPhoneNumber(),
                company.getAddress(),
                company.getCategoryId(),
                company.getProvinceId(),
                company.getDistrictId()
            );
        }
    }
}