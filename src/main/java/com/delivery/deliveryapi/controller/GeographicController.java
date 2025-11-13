package com.delivery.deliveryapi.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.delivery.deliveryapi.service.GeographicService;

@RestController
@RequestMapping("/geographic")
public class GeographicController {

    private final GeographicService geographicService;

    public GeographicController(GeographicService geographicService) {
        this.geographicService = geographicService;
    }

    @PostMapping("/provinces")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<Province> createProvince(@RequestBody CreateProvinceRequest request) {
        try {
            Province province = geographicService.createProvince(
                    request.getCode(),
                    request.getName(),
                    request.getKhmerName(),
                    request.getCapital(),
                    request.getPopulation(),
                    request.getAreaKm2(),
                    request.getDistrictsKhan(),
                    request.getCommunesSangkat(),
                    request.getTotalVillages(),
                    request.getReferenceNumber(),
                    request.getReferenceYear()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(province);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PutMapping("/provinces/{provinceId}")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<Province> updateProvince(@PathVariable UUID provinceId,
                                                  @RequestBody UpdateProvinceRequest request) {
        try {
            Province province = geographicService.updateProvince(
                    provinceId,
                    request.getName(),
                    request.getKhmerName(),
                    request.getCapital(),
                    request.getPopulation(),
                    request.getAreaKm2(),
                    request.getDistrictsKhan(),
                    request.getCommunesSangkat(),
                    request.getTotalVillages(),
                    request.getReferenceNumber(),
                    request.getReferenceYear()
            );
            return ResponseEntity.ok(province);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/provinces/{id}")
    public ResponseEntity<GeographicService.ProvinceSummary> getProvinceById(@PathVariable UUID id) {
        try {
            Optional<Province> province = geographicService.getProvinceById(id);
            if (province.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(new GeographicService.ProvinceSummary(province.get()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/provinces/code/{code}")
    public ResponseEntity<GeographicService.ProvinceSummary> getProvinceByCode(@PathVariable String code) {
        try {
            Optional<Province> province = geographicService.getProvinceByCode(code);
            if (province.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(new GeographicService.ProvinceSummary(province.get()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // District endpoints
    @GetMapping("/districts")
    public ResponseEntity<List<GeographicService.DistrictSummary>> getAllDistricts(
            @RequestParam(required = false) String search) {
        try {
            List<District> districts = geographicService.searchDistricts(search);
            List<GeographicService.DistrictSummary> summaries = districts.stream()
                    .map(GeographicService.DistrictSummary::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/districts/{id}")
    public ResponseEntity<GeographicService.DistrictSummary> getDistrictById(@PathVariable UUID id) {
        try {
            Optional<District> district = geographicService.getDistrictById(id);
            if (district.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(new GeographicService.DistrictSummary(district.get()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/districts/code/{code}")
    public ResponseEntity<GeographicService.DistrictSummary> getDistrictByCode(@PathVariable String code) {
        try {
            Optional<District> district = geographicService.getDistrictByCode(code);
            if (district.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(new GeographicService.DistrictSummary(district.get()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Districts by province endpoints
    @GetMapping("/provinces/{provinceId}/districts")
    public ResponseEntity<List<GeographicService.DistrictSummary>> getDistrictsByProvinceId(
            @PathVariable UUID provinceId,
            @RequestParam(required = false) String search) {
        try {
            Optional<Province> province = geographicService.getProvinceById(provinceId);
            if (province.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<District> districts = geographicService.searchDistrictsInProvince(province.get(), search);
            List<GeographicService.DistrictSummary> summaries = districts.stream()
                    .map(GeographicService.DistrictSummary::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/provinces/code/{provinceCode}/districts")
    public ResponseEntity<List<GeographicService.DistrictSummary>> getDistrictsByProvinceCode(
            @PathVariable String provinceCode,
            @RequestParam(required = false) String search) {
        try {
            Optional<Province> province = geographicService.getProvinceByCode(provinceCode);
            if (province.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<District> districts = geographicService.searchDistrictsInProvince(province.get(), search);
            List<GeographicService.DistrictSummary> summaries = districts.stream()
                    .map(GeographicService.DistrictSummary::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Combined endpoint for hierarchical data
    @GetMapping("/hierarchy")
    public ResponseEntity<List<GeographicService.ProvinceWithDistricts>> getGeographicHierarchy() {
        try {
            List<GeographicService.ProvinceWithDistricts> hierarchy = geographicService.getAllProvincesWithDistricts();
            return ResponseEntity.ok(hierarchy);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Request/Response DTOs
    public static class CreateProvinceRequest {
        private String code;
        private String name;
        private String khmerName;
        private String capital;
        private Integer population;
        private Integer areaKm2;
        private Integer districtsKhan;
        private Integer communesSangkat;
        private Integer totalVillages;
        private String referenceNumber;
        private Integer referenceYear;

        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getKhmerName() { return khmerName; }
        public void setKhmerName(String khmerName) { this.khmerName = khmerName; }

        public String getCapital() { return capital; }
        public void setCapital(String capital) { this.capital = capital; }

        public Integer getPopulation() { return population; }
        public void setPopulation(Integer population) { this.population = population; }

        public Integer getAreaKm2() { return areaKm2; }
        public void setAreaKm2(Integer areaKm2) { this.areaKm2 = areaKm2; }

        public Integer getDistrictsKhan() { return districtsKhan; }
        public void setDistrictsKhan(Integer districtsKhan) { this.districtsKhan = districtsKhan; }

        public Integer getCommunesSangkat() { return communesSangkat; }
        public void setCommunesSangkat(Integer communesSangkat) { this.communesSangkat = communesSangkat; }

        public Integer getTotalVillages() { return totalVillages; }
        public void setTotalVillages(Integer totalVillages) { this.totalVillages = totalVillages; }

        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

        public Integer getReferenceYear() { return referenceYear; }
        public void setReferenceYear(Integer referenceYear) { this.referenceYear = referenceYear; }
    }

    public static class UpdateProvinceRequest {
        private String name;
        private String khmerName;
        private String capital;
        private Integer population;
        private Integer areaKm2;
        private Integer districtsKhan;
        private Integer communesSangkat;
        private Integer totalVillages;
        private String referenceNumber;
        private Integer referenceYear;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getKhmerName() { return khmerName; }
        public void setKhmerName(String khmerName) { this.khmerName = khmerName; }

        public String getCapital() { return capital; }
        public void setCapital(String capital) { this.capital = capital; }

        public Integer getPopulation() { return population; }
        public void setPopulation(Integer population) { this.population = population; }

        public Integer getAreaKm2() { return areaKm2; }
        public void setAreaKm2(Integer areaKm2) { this.areaKm2 = areaKm2; }

        public Integer getDistrictsKhan() { return districtsKhan; }
        public void setDistrictsKhan(Integer districtsKhan) { this.districtsKhan = districtsKhan; }

        public Integer getCommunesSangkat() { return communesSangkat; }
        public void setCommunesSangkat(Integer communesSangkat) { this.communesSangkat = communesSangkat; }

        public Integer getTotalVillages() { return totalVillages; }
        public void setTotalVillages(Integer totalVillages) { this.totalVillages = totalVillages; }

        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

        public Integer getReferenceYear() { return referenceYear; }
        public void setReferenceYear(Integer referenceYear) { this.referenceYear = referenceYear; }
    }
}