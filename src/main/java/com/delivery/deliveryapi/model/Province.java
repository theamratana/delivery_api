package com.delivery.deliveryapi.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "provinces",
    indexes = {
        @Index(name = "idx_provinces_name", columnList = "name"),
        @Index(name = "idx_provinces_code", columnList = "code"),
        @Index(name = "idx_provinces_name_kh", columnList = "name_kh")
    }
)
public class Province extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "code", nullable = false, length = 2, unique = true)
    private String code; // Numeric code from government, e.g., "01", "02"

    @Column(name = "name", nullable = false, length = 100)
    private String name; // English name

    @Column(name = "name_kh", nullable = false, length = 100)
    private String nameKh; // Khmer name

    @Column(name = "capital", length = 100)
    private String capital;

    @Column(name = "area_km2")
    private Integer areaKm2;

    @Column(name = "population")
    private Integer population;

    // Administrative divisions
    @Column(name = "districts_krong")
    private Integer districtsKrong = 0; // Number of Krong districts

    @Column(name = "districts_srok")
    private Integer districtsSrok = 0; // Number of Srok districts

    @Column(name = "districts_khan")
    private Integer districtsKhan = 0; // Number of Khan districts (for Phnom Penh)

    @Column(name = "total_districts")
    private Integer totalDistricts = 0; // Total number of districts

    @Column(name = "communes_commune")
    private Integer communesCommune = 0; // Number of Commune communes

    @Column(name = "communes_sangkat")
    private Integer communesSangkat = 0; // Number of Sangkat communes

    @Column(name = "total_communes")
    private Integer totalCommunes = 0; // Total number of communes

    @Column(name = "total_villages")
    private Integer totalVillages = 0; // Total number of villages

    // Reference information
    @Column(name = "reference_number", length = 50)
    private String referenceNumber; // e.g., "ប្រកាសលេខ ៤៩៣ ប្រ.ក"

    @Column(name = "reference_year")
    private Integer referenceYear;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // Constructors
    public Province() {}

    public Province(String code, String name, String nameKh) {
        this.code = code;
        this.name = name;
        this.nameKh = nameKh;
    }

    public Province(String code, String name, String nameKh, String capital) {
        this.code = code;
        this.name = name;
        this.nameKh = nameKh;
        this.capital = capital;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameKh() { return nameKh; }
    public void setNameKh(String nameKh) { this.nameKh = nameKh; }

    public String getCapital() { return capital; }
    public void setCapital(String capital) { this.capital = capital; }

    public Integer getAreaKm2() { return areaKm2; }
    public void setAreaKm2(Integer areaKm2) { this.areaKm2 = areaKm2; }

    public Integer getPopulation() { return population; }
    public void setPopulation(Integer population) { this.population = population; }

    public Integer getDistrictsKrong() { return districtsKrong; }
    public void setDistrictsKrong(Integer districtsKrong) { this.districtsKrong = districtsKrong; }

    public Integer getDistrictsSrok() { return districtsSrok; }
    public void setDistrictsSrok(Integer districtsSrok) { this.districtsSrok = districtsSrok; }

    public Integer getDistrictsKhan() { return districtsKhan; }
    public void setDistrictsKhan(Integer districtsKhan) { this.districtsKhan = districtsKhan; }

    public Integer getTotalDistricts() { return totalDistricts; }
    public void setTotalDistricts(Integer totalDistricts) { this.totalDistricts = totalDistricts; }

    public Integer getCommunesCommune() { return communesCommune; }
    public void setCommunesCommune(Integer communesCommune) { this.communesCommune = communesCommune; }

    public Integer getCommunesSangkat() { return communesSangkat; }
    public void setCommunesSangkat(Integer communesSangkat) { this.communesSangkat = communesSangkat; }

    public Integer getTotalCommunes() { return totalCommunes; }
    public void setTotalCommunes(Integer totalCommunes) { this.totalCommunes = totalCommunes; }

    public Integer getTotalVillages() { return totalVillages; }
    public void setTotalVillages(Integer totalVillages) { this.totalVillages = totalVillages; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public Integer getReferenceYear() { return referenceYear; }
    public void setReferenceYear(Integer referenceYear) { this.referenceYear = referenceYear; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // Utility methods
    public void updateTotalDistricts() {
        this.totalDistricts = (districtsKrong != null ? districtsKrong : 0) +
                             (districtsSrok != null ? districtsSrok : 0) +
                             (districtsKhan != null ? districtsKhan : 0);
    }

    public void updateTotalCommunes() {
        this.totalCommunes = (communesCommune != null ? communesCommune : 0) +
                            (communesSangkat != null ? communesSangkat : 0);
    }
}