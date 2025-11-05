package com.delivery.deliveryapi.repo;

import com.delivery.deliveryapi.model.AuthIdentity;
import com.delivery.deliveryapi.model.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthIdentityRepository extends JpaRepository<AuthIdentity, UUID> {
    Optional<AuthIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
