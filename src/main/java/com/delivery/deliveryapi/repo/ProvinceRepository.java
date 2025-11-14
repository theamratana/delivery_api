package com.delivery.deliveryapi.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delivery.deliveryapi.model.Province;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, UUID> {

    Optional<Province> findByCode(String code);

    Optional<Province> findByName(String name);

    List<Province> findByActiveTrue();

    @Cacheable("provinces")
    List<Province> findByActiveTrueOrderByName();

    @Cacheable("provinces")
    @Query("SELECT p FROM Province p WHERE p.active = true ORDER BY p.name")
    List<Province> findAllActiveProvinces();

    @Query("SELECT p FROM Province p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.nameKh) LIKE LOWER(CONCAT('%', :search, '%')) AND p.active = true")
    List<Province> searchByName(@Param("search") String search);

    boolean existsByCode(String code);

    boolean existsByName(String name);
}