package com.delivery.deliveryapi.repo;

import com.delivery.deliveryapi.model.PendingEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PendingEmployeeRepository extends JpaRepository<PendingEmployee, UUID> {

    Optional<PendingEmployee> findByPhoneE164AndCompanyId(String phoneE164, UUID companyId);

    List<PendingEmployee> findByCompanyIdAndExpiresAtAfter(UUID companyId, Instant now);

    @Query("SELECT pe FROM PendingEmployee pe WHERE pe.phoneE164 = :phone AND pe.expiresAt > :now")
    Optional<PendingEmployee> findActiveByPhone(@Param("phone") String phoneE164, @Param("now") Instant now);

    boolean existsByPhoneE164AndCompanyId(String phoneE164, UUID companyId);
}