package com.delivery.deliveryapi.repo;

import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhoneE164(String phoneE164);
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    long countByCompanyId(UUID companyId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company WHERE u.id = :id")
    Optional<User> findByIdWithCompany(UUID id);
    
    // Customer management queries
    Optional<User> findByPhoneE164AndCompanyAndUserType(String phoneE164, Company company, UserType userType);
    
    @Query("SELECT u FROM User u WHERE u.company.id = :companyId AND u.userType = :userType ORDER BY u.createdAt DESC")
    List<User> findByCompanyIdAndUserType(@Param("companyId") UUID companyId, @Param("userType") UserType userType);
    
    @Query("SELECT u FROM User u WHERE u.company.id = :companyId AND u.userType = :userType AND (LOWER(u.displayName) LIKE LOWER(CONCAT('%', :search, '%')) OR u.phoneE164 LIKE CONCAT('%', :search, '%')) ORDER BY u.createdAt DESC")
    List<User> searchCustomersByCompany(@Param("companyId") UUID companyId, @Param("userType") UserType userType, @Param("search") String search);
}
