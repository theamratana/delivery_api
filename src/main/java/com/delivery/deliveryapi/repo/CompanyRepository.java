package com.delivery.deliveryapi.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delivery.deliveryapi.model.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByName(String name);
    
    @Query("SELECT c FROM Company c WHERE c.name = :name AND c.createdByCompany = :createdByCompany")
    Optional<Company> findByNameAndCreatedByCompany(@Param("name") String name, @Param("createdByCompany") Company createdByCompany);
    
    @Query("SELECT c FROM Company c WHERE c.createdByCompany.id = :creatorCompanyId ORDER BY c.name ASC")
    List<Company> findByCreatorCompanyId(@Param("creatorCompanyId") UUID creatorCompanyId);
    
    @Query("SELECT c FROM Company c WHERE c.createdByCompany.id = :creatorCompanyId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY c.name ASC")
    List<Company> searchByCreatorCompanyAndName(@Param("creatorCompanyId") UUID creatorCompanyId, @Param("searchTerm") String searchTerm);
}