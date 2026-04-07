package com.delivery.deliveryapi.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery.deliveryapi.model.OrderDelivery;

public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, UUID> {

    Optional<OrderDelivery> findByOrderId(UUID orderId);
}
