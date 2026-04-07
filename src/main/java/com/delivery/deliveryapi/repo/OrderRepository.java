package com.delivery.deliveryapi.repo;

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
}
