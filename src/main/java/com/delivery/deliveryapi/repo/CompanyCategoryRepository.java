package com.delivery.deliveryapi.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.delivery.deliveryapi.model.CompanyCategory;

@Repository
public interface CompanyCategoryRepository extends JpaRepository<CompanyCategory, UUID> {
    Optional<CompanyCategory> findByCode(String code);
}
