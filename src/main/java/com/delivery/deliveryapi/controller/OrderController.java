package com.delivery.deliveryapi.controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.model.Order;
import com.delivery.deliveryapi.model.OrderDelivery;
import com.delivery.deliveryapi.model.OrderItem;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.enums.DeliveryStatus;
import com.delivery.deliveryapi.model.enums.DiscountType;
import com.delivery.deliveryapi.model.enums.OrderStatus;
import com.delivery.deliveryapi.model.enums.OrderType;
import com.delivery.deliveryapi.model.enums.PaymentStatus;
import com.delivery.deliveryapi.model.enums.PaymentType;
import com.delivery.deliveryapi.repo.OrderRepository;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.service.OrderService;
import com.delivery.deliveryapi.service.OrderService.CreateOrderRequest;
import com.delivery.deliveryapi.service.OrderService.UpdateDeliveryStatusRequest;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, OrderRepository orderRepository, UserRepository userRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // ── POST /orders ─────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest req) {
        User current = getCurrentUser();
        if (current.getCompany() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "User is not part of any company"));
        }
        try {
            Order order = orderService.createOrder(current.getCompany().getId(), req);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDetail(order));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // ── PATCH /orders/{id}/status ────────────────────────────────────

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateDeliveryStatus(
            @PathVariable UUID id,
            @RequestBody UpdateDeliveryStatusRequest req) {
        User current = getCurrentUser();
        if (current.getCompany() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "User is not part of any company"));
        }
        try {
            Order order = orderService.updateDeliveryStatus(current.getCompany().getId(), id, req);
            return ResponseEntity.ok(toDetail(order));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /orders ───────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> listOrders(Pageable pageable) {
        User current = getCurrentUser();
        if (current.getCompany() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "User is not part of any company"));
        }
        try {
            Page<OrderSummary> page = orderRepository
                .findByCompanyId(current.getCompany().getId(), pageable)
                .map(OrderController::toSummary);
            return ResponseEntity.ok(page);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /orders/{id} ─────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable UUID id) {
        User current = getCurrentUser();
        if (current.getCompany() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try {
            return orderRepository.findById(id)
                .filter(o -> o.getCompanyId().equals(current.getCompany().getId()))
                .filter(o -> !o.isDeleted())
                .map(o -> ResponseEntity.ok(toDetail(o)))
                .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Response mapping ──────────────────────────────────────────────────────

    private static OrderSummary toSummary(Order o) {
        return new OrderSummary(
            o.getId(),
            o.getOrderNumber(),
            o.getOrderType(),
            o.getOrderStatus(),
            o.getPaymentType(),
            o.getPaymentStatus(),
            o.getSubtotal(),
            o.getOrderDiscount(),
            o.getDeliveryFee(),
            o.getGrandTotal(),
            o.getCustomer() != null ? o.getCustomer().getDisplayName() : null,
            o.getCustomer() != null ? o.getCustomer().getPhoneE164() : null,
            o.getOrderDate(),
            o.getCreatedAt()
        );
    }

    private static OrderDetail toDetail(Order o) {
        List<OrderItemDetail> items = o.getItems().stream()
            .map(OrderController::toItemDetail)
            .collect(Collectors.toList());

        OrderDeliveryDetail delivery = o.getDelivery() != null ? toDeliveryDetail(o.getDelivery()) : null;

        return new OrderDetail(
            o.getId(),
            o.getOrderNumber(),
            o.getOrderType(),
            o.getOrderStatus(),
            o.getPaymentType(),
            o.getPaymentStatus(),
            o.getSubtotal(),
            o.getDiscountType(),
            o.getDiscountValue(),
            o.getOrderDiscount(),
            o.getDeliveryFee(),
            o.getGrandTotal(),
            o.getNotes(),
            o.getCustomer() != null ? o.getCustomer().getId() : null,
            o.getCustomer() != null ? o.getCustomer().getDisplayName() : null,
            o.getCustomer() != null ? o.getCustomer().getPhoneE164() : null,
            o.getOrderDate(),
            o.getCreatedAt(),
            items,
            delivery
        );
    }

    private static OrderItemDetail toItemDetail(OrderItem i) {
        return new OrderItemDetail(
            i.getId(),
            i.getProduct() != null ? i.getProduct().getId() : null,
            i.getProductName(),
            i.getProductSku(),
            i.getUnitPrice(),
            i.getQuantity(),
            i.getDiscountType(),
            i.getDiscountValue(),
            i.getSubTotal(),
            i.getLineDiscount(),
            i.getLineTotal(),
            i.getNotes()
        );
    }

    private static OrderDeliveryDetail toDeliveryDetail(OrderDelivery d) {
        return new OrderDeliveryDetail(
            d.getId(),
            d.getDeliveryCompanyId(),
            d.getRecipientName(),
            d.getRecipientPhone(),
            d.getDeliveryAddress(),
            d.getProvince() != null ? d.getProvince().getName() : null,
            d.getDistrict() != null ? d.getDistrict().getName() : null,
            d.getDeliveryFeeStandard(),
            d.getDeliveryFeeCharged(),
            d.getDeliveryStatus(),
            d.getTrackingNumber(),
            d.getDeliveryNotes(),
            d.getDeliveredAt()
        );
    }

    // ── Private helper ────────────────────────────────────────────────────────

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String userIdStr)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    // ── Response record types ─────────────────────────────────────────────────

    record OrderSummary(
        UUID id,
        String orderNumber,
        OrderType orderType,
        OrderStatus orderStatus,
        PaymentType paymentType,
        PaymentStatus paymentStatus,
        BigDecimal subtotal,
        BigDecimal orderDiscount,
        BigDecimal deliveryFee,
        BigDecimal grandTotal,
        String customerName,
        String customerPhone,
        OffsetDateTime orderDate,
        OffsetDateTime createdAt
    ) {}

    record OrderDetail(
        UUID id,
        String orderNumber,
        OrderType orderType,
        OrderStatus orderStatus,
        PaymentType paymentType,
        PaymentStatus paymentStatus,
        BigDecimal subtotal,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal orderDiscount,
        BigDecimal deliveryFee,
        BigDecimal grandTotal,
        String notes,
        UUID customerId,
        String customerName,
        String customerPhone,
        OffsetDateTime orderDate,
        OffsetDateTime createdAt,
        List<OrderItemDetail> items,
        OrderDeliveryDetail delivery
    ) {}

    record OrderItemDetail(
        UUID id,
        UUID productId,
        String productName,
        String productSku,
        BigDecimal unitPrice,
        BigDecimal quantity,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal subTotal,
        BigDecimal lineDiscount,
        BigDecimal lineTotal,
        String notes
    ) {}

    record OrderDeliveryDetail(
        UUID id,
        UUID deliveryCompanyId,
        String recipientName,
        String recipientPhone,
        String deliveryAddress,
        String province,
        String district,
        BigDecimal deliveryFeeStandard,
        BigDecimal deliveryFeeCharged,
        DeliveryStatus deliveryStatus,
        String trackingNumber,
        String deliveryNotes,
        OffsetDateTime deliveredAt
    ) {}
}
