package com.delivery.deliveryapi.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
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