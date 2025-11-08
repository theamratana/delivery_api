package com.delivery.deliveryapi.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    // Province endpoints
    @GetMapping("/provinces")
    public ResponseEntity<List<GeographicService.ProvinceSummary>> getAllProvinces(
            @RequestParam(required = false) String search) {
        try {
            List<Province> provinces = geographicService.searchProvinces(search);
            List<GeographicService.ProvinceSummary> summaries = provinces.stream()
                    .map(GeographicService.ProvinceSummary::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
}