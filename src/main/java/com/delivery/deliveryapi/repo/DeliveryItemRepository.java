package com.delivery.deliveryapi.repo;

import java.time.OffsetDateTime;
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

    // Find all items belonging to a specific batch
    List<DeliveryItem> findByBatchId(java.util.UUID batchId);

    List<DeliveryItem> findByStatusAndDeletedFalse(DeliveryStatus status);

    @Query("SELECT d FROM DeliveryItem d WHERE d.sender = :user OR d.receiver = :user OR d.deliveryDriver = :user AND d.deleted = false ORDER BY d.createdAt DESC")
    List<DeliveryItem> findByUserInvolved(@Param("user") User user);

    @Query("SELECT d FROM DeliveryItem d WHERE d.receiver IS NOT NULL AND d.receiver.phoneE164 = :phone AND d.deleted = false ORDER BY d.updatedAt DESC")
    List<DeliveryItem> findByReceiverPhone(@Param("phone") String phone);

    long countBySenderIdAndDeletedFalse(UUID senderId);

    long countByStatusAndDeletedFalse(DeliveryStatus status);

    @Query("SELECT d.status, COUNT(d) FROM DeliveryItem d WHERE (d.sender.id = :userId OR d.receiver.id = :userId OR d.deliveryDriver.id = :userId) AND d.deleted = false AND d.createdAt >= :start AND d.createdAt <= :end GROUP BY d.status")
    List<Object[]> countStatusByUserInRange(@Param("userId") UUID userId, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}