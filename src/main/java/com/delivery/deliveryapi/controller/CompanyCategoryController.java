package com.delivery.deliveryapi.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.model.CompanyCategory;
import com.delivery.deliveryapi.repo.CompanyCategoryRepository;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/company-categories")
public class CompanyCategoryController {

    private final CompanyCategoryRepository companyCategoryRepository;

    public CompanyCategoryController(CompanyCategoryRepository companyCategoryRepository) {
        this.companyCategoryRepository = companyCategoryRepository;
    }

    @GetMapping
    public ResponseEntity<List<CompanyCategoryResponse>> getAllCategories() {
        var categories = companyCategoryRepository.findAll();
        var response = categories.stream()
            .map(CompanyCategoryResponse::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<CompanyCategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        try {
            CompanyCategory category = new CompanyCategory(
                request.getCode(),
                request.getName(),
                request.getNameKm()
            );
            category = companyCategoryRepository.save(category);
            return ResponseEntity.status(HttpStatus.CREATED).body(new CompanyCategoryResponse(category));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<CompanyCategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        try {
            var categoryOpt = companyCategoryRepository.findById(id);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            CompanyCategory category = categoryOpt.get();
            if (request.getName() != null) {
                category.setName(request.getName());
            }
            if (request.getNameKm() != null) {
                category.setNameKm(request.getNameKm());
            }

            category = companyCategoryRepository.save(category);
            return ResponseEntity.ok(new CompanyCategoryResponse(category));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        try {
            if (!companyCategoryRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            companyCategoryRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // DTOs
    public static class CreateCategoryRequest {
        @JsonProperty("code")
        @NotBlank(message = "Code is required")
        private String code;

        @JsonProperty("name")
        @NotBlank(message = "Name is required")
        private String name;

        @JsonProperty("nameKm")
        private String nameKm;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getNameKm() { return nameKm; }
        public void setNameKm(String nameKm) { this.nameKm = nameKm; }
    }

    public static class UpdateCategoryRequest {
        @JsonProperty("name")
        private String name;

        @JsonProperty("nameKm")
        private String nameKm;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getNameKm() { return nameKm; }
        public void setNameKm(String nameKm) { this.nameKm = nameKm; }
    }

    public static class CompanyCategoryResponse {
        @JsonProperty("id")
        private String id;

        @JsonProperty("code")
        private String code;

        @JsonProperty("name")
        private String name;

        @JsonProperty("nameKm")
        private String nameKm;

        public CompanyCategoryResponse(CompanyCategory category) {
            this.id = category.getId().toString();
            this.code = category.getCode();
            this.name = category.getName();
            this.nameKm = category.getNameKm();
        }

        public String getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getNameKm() { return nameKm; }
    }
}
