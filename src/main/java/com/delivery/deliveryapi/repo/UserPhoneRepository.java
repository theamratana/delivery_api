package com.delivery.deliveryapi.repo;

import com.delivery.deliveryapi.model.UserPhone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPhoneRepository extends JpaRepository<UserPhone, UUID> {
    Optional<UserPhone> findByPhoneE164(String phoneE164);
    boolean existsByPhoneE164(String phoneE164);
    List<UserPhone> findByUserId(UUID userId);
    Optional<UserPhone> findByUserIdAndPrimaryTrue(UUID userId);
}
