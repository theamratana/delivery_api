package com.delivery.deliveryapi.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delivery.deliveryapi.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderIdOrderByCreatedAtAsc(UUID orderId);

    @Query("""
        SELECT i.product.id, i.productName, i.productSku,
               COALESCE(SUM(i.quantity), 0), COALESCE(SUM(i.lineTotal), 0)
        FROM OrderItem i
        JOIN i.order o
        WHERE o.companyId = :cid AND o.deleted = false
          AND o.orderDate >= :from
          AND o.orderDate <= :to
        GROUP BY i.product.id, i.productName, i.productSku
        ORDER BY SUM(i.quantity) DESC
        """)
    List<Object[]> statsBestSellingProducts(@Param("cid") UUID companyId,
                                             @Param("from") OffsetDateTime from,
                                             @Param("to") OffsetDateTime to,
                                             Pageable pageable);
}
