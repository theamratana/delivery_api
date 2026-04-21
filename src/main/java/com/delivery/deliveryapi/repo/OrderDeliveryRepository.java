package com.delivery.deliveryapi.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delivery.deliveryapi.model.OrderDelivery;

public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, UUID> {

    Optional<OrderDelivery> findByOrderId(UUID orderId);

    @Query("""
        SELECT d.deliveryCompanyId, COUNT(d), COALESCE(SUM(d.deliveryFeeCharged), 0)
        FROM OrderDelivery d
        JOIN d.order o
        WHERE o.companyId = :cid AND o.deleted = false
          AND d.deliveryCompanyId IS NOT NULL
          AND o.orderDate >= :from
          AND o.orderDate <= :to
        GROUP BY d.deliveryCompanyId
        ORDER BY COUNT(d) DESC
        """)
    List<Object[]> statsByDeliveryCompany(@Param("cid") UUID companyId,
                                           @Param("from") OffsetDateTime from,
                                           @Param("to") OffsetDateTime to);
}
