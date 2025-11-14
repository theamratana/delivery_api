package com.delivery.deliveryapi.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery.deliveryapi.model.DeliveryPhoto;

public interface DeliveryPhotoRepository extends JpaRepository<DeliveryPhoto, UUID> {

    List<DeliveryPhoto> findByDeliveryItemIdOrderBySequenceOrderAsc(UUID deliveryItemId);

    List<DeliveryPhoto> findByDeliveryItemIdAndDeletedFalseOrderBySequenceOrderAsc(UUID deliveryItemId);

    void deleteByDeliveryItemId(UUID deliveryItemId);
}