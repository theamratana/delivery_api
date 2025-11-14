package com.delivery.deliveryapi.controller;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.District;
import com.delivery.deliveryapi.model.Province;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserRole;
import com.delivery.deliveryapi.repo.CompanyRepository;
import com.delivery.deliveryapi.repo.DistrictRepository;
import com.delivery.deliveryapi.repo.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;

    public CompanyController(CompanyRepository companyRepository, UserRepository userRepository, DistrictRepository districtRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.districtRepository = districtRepository;
    }

    public record UpdateCompanyRequest(
        @NotBlank(message = "Company name is required") String name,
        String address,
        UUID districtId
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
        District district = company.getDistrict();
        ProvinceInfo provinceInfo = district != null && district.getProvince() != null ?
            new ProvinceInfo(district.getProvince().getId(), district.getProvince().getName(), district.getProvince().getNameKh()) : null;
        DistrictInfo districtInfo = district != null ?
            new DistrictInfo(district.getId(), district.getName(), district.getNameKh(), provinceInfo) : null;

        return ResponseEntity.ok(new CompanyResponse(
            company.getId(),
            company.getName(),
            company.getAddress(),
            districtInfo,
            company.isActive()
        ));
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

        // Set district if provided
        if (req.districtId != null) {
            Optional<District> optDistrict = districtRepository.findById(req.districtId);
            if (optDistrict.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid district ID"));
            }
            company.setDistrict(optDistrict.get());
        } else {
            company.setDistrict(null);
        }

        companyRepository.save(company);

        // Build response
        District district = company.getDistrict();
        ProvinceInfo provinceInfo = district != null && district.getProvince() != null ?
            new ProvinceInfo(district.getProvince().getId(), district.getProvince().getName(), district.getProvince().getNameKh()) : null;
        DistrictInfo districtInfo = district != null ?
            new DistrictInfo(district.getId(), district.getName(), district.getNameKh(), provinceInfo) : null;

        return ResponseEntity.ok(Map.of(
            "message", "Company information updated successfully",
            "company", new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getAddress(),
                districtInfo,
                company.isActive()
            )
        ));
    }
}