package com.delivery.deliveryapi.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery.deliveryapi.model.DeliveryPackage;

public interface DeliveryPackageRepository extends JpaRepository<DeliveryPackage, UUID> {
    Optional<DeliveryPackage> findById(UUID id);
}
