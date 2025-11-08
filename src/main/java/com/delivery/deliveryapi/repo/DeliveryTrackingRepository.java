package com.delivery.deliveryapi.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery.deliveryapi.model.DeliveryTracking;

public interface DeliveryTrackingRepository extends JpaRepository<DeliveryTracking, UUID> {

    List<DeliveryTracking> findByDeliveryItemIdOrderByTimestampDesc(UUID deliveryItemId);

    List<DeliveryTracking> findByDeliveryItemIdAndDeletedFalseOrderByTimestampDesc(UUID deliveryItemId);

    List<DeliveryTracking> findByStatusUpdatedByIdAndDeletedFalseOrderByTimestampDesc(UUID userId);

    DeliveryTracking findFirstByDeliveryItemIdAndDeletedFalseOrderByTimestampDesc(UUID deliveryItemId);
}