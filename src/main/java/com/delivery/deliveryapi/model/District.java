package com.delivery.deliveryapi.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "districts",
    indexes = {
        @Index(name = "idx_districts_name", columnList = "name"),
        @Index(name = "idx_districts_code", columnList = "code"),
        @Index(name = "idx_districts_province_id", columnList = "province_id"),
        @Index(name = "idx_districts_province_name", columnList = "province_id, name")
    }
)
public class District extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "name_kh", length = 100)
    private String nameKh; // Khmer name

    @Column(name = "code", nullable = false, length = 20, unique = true)
    private String code; // Province code + district code, e.g., "PP-001" for Chamkarmon in Phnom Penh

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;

    @Column(name = "type", length = 50)
    private String type; // "district", "municipality", "city", etc.

    @Column(name = "area_km2")
    private Integer areaKm2;

    @Column(name = "population")
    private Integer population;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    // Administrative divisions
    @Column(name = "communes_commune")
    private Integer communesCommune = 0; // Number of rural communes

    @Column(name = "communes_sangkat")
    private Integer communesSangkat = 0; // Number of urban sangkats

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
    public District() {}

    public District(String name, String code, Province province) {
        this.name = name;
        this.code = code;
        this.province = province;
    }

    public District(String name, String nameKh, String code, Province province) {
        this.name = name;
        this.nameKh = nameKh;
        this.code = code;
        this.province = province;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameKh() { return nameKh; }
    public void setNameKh(String nameKh) { this.nameKh = nameKh; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Province getProvince() { return province; }
    public void setProvince(Province province) { this.province = province; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getAreaKm2() { return areaKm2; }
    public void setAreaKm2(Integer areaKm2) { this.areaKm2 = areaKm2; }

    public Integer getPopulation() { return population; }
    public void setPopulation(Integer population) { this.population = population; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public Integer getCommunesCommune() { return communesCommune; }
    public void setCommunesCommune(Integer communesCommune) { this.communesCommune = communesCommune; }

    public Integer getCommunesSangkat() { return communesSangkat; }
    public void setCommunesSangkat(Integer communesSangkat) { this.communesSangkat = communesSangkat; }

    public Integer getTotalVillages() { return totalVillages; }
    public void setTotalVillages(Integer totalVillages) { this.totalVillages = totalVillages; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public Integer getReferenceYear() { return referenceYear; }
    public void setReferenceYear(Integer referenceYear) { this.referenceYear = referenceYear; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}