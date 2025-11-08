package com.delivery.deliveryapi.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.deliveryapi.model.District;
import com.delivery.deliveryapi.model.Province;
import com.delivery.deliveryapi.repo.DistrictRepository;
import com.delivery.deliveryapi.repo.ProvinceRepository;

@Service
public class GeographicService {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;

    public GeographicService(ProvinceRepository provinceRepository, DistrictRepository districtRepository) {
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
    }

    // Province operations
    @Transactional(readOnly = true)
    public List<Province> getAllActiveProvinces() {
        return provinceRepository.findByActiveTrueOrderByName();
    }

    @Transactional(readOnly = true)
    public Optional<Province> getProvinceById(UUID id) {
        return provinceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Province> getProvinceByCode(String code) {
        return provinceRepository.findByCode(code);
    }

    @Transactional(readOnly = true)
    public Optional<Province> getProvinceByName(String name) {
        return provinceRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public List<Province> searchProvinces(String search) {
        if (search == null || search.trim().isEmpty()) {
            return provinceRepository.findByActiveTrueOrderByName();
        }
        return provinceRepository.searchByName(search.trim());
    }

    // District operations
    @Transactional(readOnly = true)
    public List<District> getAllActiveDistricts() {
        return districtRepository.findByActiveTrueOrderByName();
    }

    @Transactional(readOnly = true)
    public Optional<District> getDistrictById(UUID id) {
        return districtRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<District> getDistrictByCode(String code) {
        return districtRepository.findByCode(code);
    }

    @Transactional(readOnly = true)
    public List<District> getDistrictsByProvince(Province province) {
        return districtRepository.findByProvinceAndActiveTrueOrderByName(province);
    }

    @Transactional(readOnly = true)
    public List<District> getDistrictsByProvinceId(UUID provinceId) {
        return districtRepository.findActiveDistrictsByProvinceId(provinceId);
    }

    @Transactional(readOnly = true)
    public List<District> getDistrictsByProvinceCode(String provinceCode) {
        return districtRepository.findActiveDistrictsByProvinceCode(provinceCode);
    }

    @Transactional(readOnly = true)
    public List<District> searchDistricts(String search) {
        if (search == null || search.trim().isEmpty()) {
            return districtRepository.findByActiveTrueOrderByName();
        }
        return districtRepository.searchByName(search.trim());
    }

    @Transactional(readOnly = true)
    public List<District> searchDistrictsInProvince(Province province, String search) {
        if (search == null || search.trim().isEmpty()) {
            return districtRepository.findByProvinceAndActiveTrueOrderByName(province);
        }
        return districtRepository.searchByNameInProvince(province, search.trim());
    }

    // Combined operations for API responses
    @Transactional(readOnly = true)
    public List<ProvinceWithDistricts> getAllProvincesWithDistricts() {
        List<Province> provinces = provinceRepository.findByActiveTrueOrderByName();
        return provinces.stream()
                .map(province -> {
                    List<District> districts = districtRepository.findByProvinceAndActiveTrueOrderByName(province);
                    return new ProvinceWithDistricts(province, districts);
                })
                .toList();
    }

    // DTOs for API responses
    public static class ProvinceSummary {
        private String id;
        private String code;
        private String name;
        private String nameKh;
        private String capital;
        private Integer areaKm2;
        private Integer population;
        private Integer districtsKrong;
        private Integer districtsSrok;
        private Integer districtsKhan;
        private Integer totalDistricts;
        private Integer communesCommune;
        private Integer communesSangkat;
        private Integer totalCommunes;
        private Integer totalVillages;
        private String referenceNumber;
        private Integer referenceYear;

        public ProvinceSummary(Province province) {
            this.id = province.getId().toString();
            this.code = province.getCode();
            this.name = province.getName();
            this.nameKh = province.getNameKh();
            this.capital = province.getCapital();
            this.areaKm2 = province.getAreaKm2();
            this.population = province.getPopulation();
            this.districtsKrong = province.getDistrictsKrong();
            this.districtsSrok = province.getDistrictsSrok();
            this.districtsKhan = province.getDistrictsKhan();
            this.totalDistricts = province.getTotalDistricts();
            this.communesCommune = province.getCommunesCommune();
            this.communesSangkat = province.getCommunesSangkat();
            this.totalCommunes = province.getTotalCommunes();
            this.totalVillages = province.getTotalVillages();
            this.referenceNumber = province.getReferenceNumber();
            this.referenceYear = province.getReferenceYear();
        }

        // Getters
        public String getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getNameKh() { return nameKh; }
        public String getCapital() { return capital; }
        public Integer getAreaKm2() { return areaKm2; }
        public Integer getPopulation() { return population; }
        public Integer getDistrictsKrong() { return districtsKrong; }
        public Integer getDistrictsSrok() { return districtsSrok; }
        public Integer getDistrictsKhan() { return districtsKhan; }
        public Integer getTotalDistricts() { return totalDistricts; }
        public Integer getCommunesCommune() { return communesCommune; }
        public Integer getCommunesSangkat() { return communesSangkat; }
        public Integer getTotalCommunes() { return totalCommunes; }
        public Integer getTotalVillages() { return totalVillages; }
        public String getReferenceNumber() { return referenceNumber; }
        public Integer getReferenceYear() { return referenceYear; }
    }

    public static class DistrictSummary {
        private String id;
        private String name;
        private String nameKh;
        private String code;
        private String type;
        private Integer areaKm2;
        private Integer population;
        private String postalCode;
        private String provinceName;
        private String provinceCode;
        private Integer communesCommune;
        private Integer communesSangkat;
        private Integer totalCommunes;
        private Integer totalVillages;
        private String referenceNumber;
        private Integer referenceYear;

        public DistrictSummary(District district) {
            this.id = district.getId().toString();
            this.name = district.getName();
            this.nameKh = district.getNameKh();
            this.code = district.getCode();
            this.type = district.getType();
            this.areaKm2 = district.getAreaKm2();
            this.population = district.getPopulation();
            this.postalCode = district.getPostalCode();
            this.communesCommune = district.getCommunesCommune();
            this.communesSangkat = district.getCommunesSangkat();
            this.totalCommunes = (district.getCommunesCommune() != null ? district.getCommunesCommune() : 0) +
                                (district.getCommunesSangkat() != null ? district.getCommunesSangkat() : 0);
            this.totalVillages = district.getTotalVillages();
            this.referenceNumber = district.getReferenceNumber();
            this.referenceYear = district.getReferenceYear();
            if (district.getProvince() != null) {
                this.provinceName = district.getProvince().getName();
                this.provinceCode = district.getProvince().getCode();
            }
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getNameKh() { return nameKh; }
        public String getCode() { return code; }
        public String getType() { return type; }
        public Integer getAreaKm2() { return areaKm2; }
        public Integer getPopulation() { return population; }
        public String getPostalCode() { return postalCode; }
        public String getProvinceName() { return provinceName; }
        public String getProvinceCode() { return provinceCode; }
        public Integer getCommunesCommune() { return communesCommune; }
        public Integer getCommunesSangkat() { return communesSangkat; }
        public Integer getTotalCommunes() { return totalCommunes; }
        public Integer getTotalVillages() { return totalVillages; }
        public String getReferenceNumber() { return referenceNumber; }
        public Integer getReferenceYear() { return referenceYear; }
    }

    public static class ProvinceWithDistricts {
        private ProvinceSummary province;
        private List<DistrictSummary> districts;

        public ProvinceWithDistricts(Province province, List<District> districts) {
            this.province = new ProvinceSummary(province);
            this.districts = districts.stream()
                    .map(DistrictSummary::new)
                    .toList();
        }

        public ProvinceSummary getProvince() { return province; }
        public List<DistrictSummary> getDistricts() { return districts; }
    }
}