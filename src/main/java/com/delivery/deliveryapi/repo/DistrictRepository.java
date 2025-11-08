package com.delivery.deliveryapi.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delivery.deliveryapi.model.District;
import com.delivery.deliveryapi.model.Province;

@Repository
public interface DistrictRepository extends JpaRepository<District, UUID> {

    Optional<District> findByCode(String code);

    Optional<District> findByName(String name);

    List<District> findByProvince(Province province);

    List<District> findByProvinceAndActiveTrue(Province province);

    List<District> findByProvinceAndActiveTrueOrderByName(Province province);

    List<District> findByActiveTrue();

    List<District> findByActiveTrueOrderByName();

    @Query("SELECT d FROM District d WHERE d.province = :province AND d.active = true ORDER BY d.name")
    List<District> findActiveDistrictsByProvince(@Param("province") Province province);

    @Query("SELECT d FROM District d WHERE d.province.id = :provinceId AND d.active = true ORDER BY d.name")
    List<District> findActiveDistrictsByProvinceId(@Param("provinceId") UUID provinceId);

    @Query("SELECT d FROM District d WHERE d.province.code = :provinceCode AND d.active = true ORDER BY d.name")
    List<District> findActiveDistrictsByProvinceCode(@Param("provinceCode") String provinceCode);

    @Query("SELECT d FROM District d WHERE d.active = true ORDER BY d.province.name, d.name")
    List<District> findAllActiveDistrictsGroupedByProvince();

    @Query("SELECT d FROM District d WHERE (LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(d.nameKh) LIKE LOWER(CONCAT('%', :search, '%'))) AND d.active = true")
    List<District> searchByName(@Param("search") String search);

    @Query("SELECT d FROM District d WHERE d.province = :province AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(d.nameKh) LIKE LOWER(CONCAT('%', :search, '%'))) AND d.active = true")
    List<District> searchByNameInProvince(@Param("province") Province province, @Param("search") String search);

    boolean existsByCode(String code);

    boolean existsByNameAndProvince(String name, Province province);
}