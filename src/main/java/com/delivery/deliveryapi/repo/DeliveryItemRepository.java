package com.delivery.deliveryapi.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delivery.deliveryapi.model.DeliveryItem;
import com.delivery.deliveryapi.model.DeliveryStatus;
import com.delivery.deliveryapi.model.User;

public interface DeliveryItemRepository extends JpaRepository<DeliveryItem, UUID> {

    List<DeliveryItem> findBySenderIdAndDeletedFalseOrderByCreatedAtDesc(UUID senderId);

    List<DeliveryItem> findByReceiverIdAndDeletedFalseOrderByCreatedAtDesc(UUID receiverId);

    List<DeliveryItem> findByDeliveryCompanyIdAndDeletedFalseOrderByCreatedAtDesc(UUID companyId);

    List<DeliveryItem> findByDeliveryDriverIdAndDeletedFalseOrderByCreatedAtDesc(UUID driverId);

    List<DeliveryItem> findByStatusAndDeletedFalse(DeliveryStatus status);

    @Query("SELECT d FROM DeliveryItem d WHERE d.sender = :user OR d.receiver = :user OR d.deliveryDriver = :user AND d.deleted = false ORDER BY d.createdAt DESC")
    List<DeliveryItem> findByUserInvolved(@Param("user") User user);

    long countBySenderIdAndDeletedFalse(UUID senderId);

    long countByStatusAndDeletedFalse(DeliveryStatus status);
}