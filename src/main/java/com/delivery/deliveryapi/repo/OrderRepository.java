package com.delivery.deliveryapi.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delivery.deliveryapi.model.Order;
import com.delivery.deliveryapi.model.enums.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    boolean existsByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o WHERE o.companyId = :companyId AND o.deleted = false ORDER BY o.orderDate DESC")
    Page<Order> findByCompanyId(@Param("companyId") UUID companyId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.companyId = :companyId AND o.orderStatus = :status AND o.deleted = false ORDER BY o.orderDate DESC")
    List<Order> findByCompanyIdAndStatus(@Param("companyId") UUID companyId, @Param("status") OrderStatus status);

    // ── Stats queries ─────────────────────────────────────────────────────────

    @Query("""
        SELECT COUNT(o), COALESCE(SUM(o.grandTotal), 0), COALESCE(AVG(o.grandTotal), 0)
        FROM Order o
        WHERE o.companyId = :cid AND o.deleted = false
          AND o.orderDate >= :from
          AND o.orderDate <= :to
        """)
    List<Object[]> statsSummary(@Param("cid") UUID companyId,
                          @Param("from") OffsetDateTime from,
                          @Param("to") OffsetDateTime to);

    @Query("""
        SELECT o.orderStatus, COUNT(o), COALESCE(SUM(o.grandTotal), 0)
        FROM Order o
        WHERE o.companyId = :cid AND o.deleted = false
          AND o.orderDate >= :from
          AND o.orderDate <= :to
        GROUP BY o.orderStatus
        ORDER BY COUNT(o) DESC
        """)
    List<Object[]> statsByStatus(@Param("cid") UUID companyId,
                                  @Param("from") OffsetDateTime from,
                                  @Param("to") OffsetDateTime to);

    @Query("""
        SELECT o.orderType, COUNT(o), COALESCE(SUM(o.grandTotal), 0)
        FROM Order o
        WHERE o.companyId = :cid AND o.deleted = false
          AND o.orderDate >= :from
          AND o.orderDate <= :to
        GROUP BY o.orderType
        ORDER BY COUNT(o) DESC
        """)
    List<Object[]> statsByType(@Param("cid") UUID companyId,
                                @Param("from") OffsetDateTime from,
                                @Param("to") OffsetDateTime to);

    @Query("""
        SELECT o.paymentStatus, COUNT(o), COALESCE(SUM(o.grandTotal), 0)
        FROM Order o
        WHERE o.companyId = :cid AND o.deleted = false
          AND o.orderDate >= :from
          AND o.orderDate <= :to
        GROUP BY o.paymentStatus
        ORDER BY COUNT(o) DESC
        """)
    List<Object[]> statsByPaymentStatus(@Param("cid") UUID companyId,
                                         @Param("from") OffsetDateTime from,
                                         @Param("to") OffsetDateTime to);

    @Query("""
        SELECT o.customer.id, o.customer.displayName, o.customer.phoneE164,
               COUNT(o), COALESCE(SUM(o.grandTotal), 0)
        FROM Order o
        WHERE o.companyId = :cid AND o.deleted = false
          AND o.orderDate >= :from
          AND o.orderDate <= :to
        GROUP BY o.customer.id, o.customer.displayName, o.customer.phoneE164
        ORDER BY COUNT(o) DESC
        """)
    List<Object[]> statsLoyalCustomers(@Param("cid") UUID companyId,
                                        @Param("from") OffsetDateTime from,
                                        @Param("to") OffsetDateTime to,
                                        Pageable pageable);
}
