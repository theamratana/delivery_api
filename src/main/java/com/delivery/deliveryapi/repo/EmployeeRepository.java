package com.delivery.deliveryapi.repo;

import com.delivery.deliveryapi.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByCompanyId(UUID companyId);

    List<Employee> findByUserId(UUID userId);

    Optional<Employee> findByUserIdAndCompanyId(UUID userId, UUID companyId);

    List<Employee> findByCompanyIdAndActive(UUID companyId, boolean active);

    @Query("SELECT e FROM Employee e WHERE e.user.id = :userId AND e.active = true ORDER BY e.createdAt DESC")
    List<Employee> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.company.id = :companyId AND e.active = true")
    long countActiveByCompanyId(@Param("companyId") UUID companyId);
}