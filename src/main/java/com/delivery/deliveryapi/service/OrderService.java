package com.delivery.deliveryapi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.deliveryapi.model.DeliveryFee;
import com.delivery.deliveryapi.model.District;
import com.delivery.deliveryapi.model.Order;
import com.delivery.deliveryapi.model.OrderDelivery;
import com.delivery.deliveryapi.model.OrderItem;
import com.delivery.deliveryapi.model.Product;
import com.delivery.deliveryapi.model.Province;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserType;
import com.delivery.deliveryapi.model.enums.DeliveryStatus;
import com.delivery.deliveryapi.model.enums.DiscountType;
import com.delivery.deliveryapi.model.enums.OrderStatus;
import com.delivery.deliveryapi.model.enums.OrderType;
import com.delivery.deliveryapi.model.enums.PaymentStatus;
import com.delivery.deliveryapi.model.enums.PaymentType;
import com.delivery.deliveryapi.repo.DeliveryFeeRepository;
import com.delivery.deliveryapi.repo.DistrictRepository;
import com.delivery.deliveryapi.repo.OrderDeliveryRepository;
import com.delivery.deliveryapi.repo.OrderItemRepository;
import com.delivery.deliveryapi.repo.OrderRepository;
import com.delivery.deliveryapi.repo.ProductRepository;
import com.delivery.deliveryapi.repo.ProvinceRepository;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.service.base.BaseServiceImpl;

@Service
public class OrderService extends BaseServiceImpl<Order, OrderRepository> {

    private final OrderItemRepository orderItemRepository;
    private final OrderDeliveryRepository orderDeliveryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final DeliveryFeeRepository deliveryFeeRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderDeliveryRepository orderDeliveryRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            ProvinceRepository provinceRepository,
            DistrictRepository districtRepository,
            DeliveryFeeRepository deliveryFeeRepository) {
        super(orderRepository);
        this.orderItemRepository = orderItemRepository;
        this.orderDeliveryRepository = orderDeliveryRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.deliveryFeeRepository = deliveryFeeRepository;
    }

    /**
     * Create a complete order with items and optional delivery in one transaction.
     * Handles WALK_IN, PICKUP, and DELIVERY order types.
     */
    @Transactional
    public Order createOrder(UUID companyId, CreateOrderRequest req) {
        // 1. Validate customer belongs to this company and is a CUSTOMER
        User customer = userRepository.findById(req.customerId())
            .filter(u -> u.getUserType() == UserType.CUSTOMER)
            .filter(u -> u.getCompany() != null && u.getCompany().getId().equals(companyId))
            .orElseThrow(() -> new IllegalArgumentException("Customer not found or does not belong to this company"));

        // 2. Validate items are provided
        if (req.items() == null || req.items().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        // 3. Validate delivery is provided when order type is DELIVERY
        if (req.orderType() == OrderType.DELIVERY && req.delivery() == null) {
            throw new IllegalArgumentException("Delivery information is required for DELIVERY orders");
        }

        // 4. Build the Order
        Order order = new Order();
        order.setCompanyId(companyId);
        order.setCustomer(customer);
        order.setOrderNumber(generateOrderNumber());
        order.setOrderType(req.orderType());
        if (req.orderType() == OrderType.WALK_IN) {
            order.setPaymentType(PaymentType.PAID);
            order.setPaymentStatus(PaymentStatus.COMPLETED);
            order.setOrderStatus(OrderStatus.COMPLETED);
        } else {
            order.setPaymentType(req.paymentType());
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setOrderStatus(OrderStatus.PENDING);
        }
        order.setDiscountType(req.discountType());
        order.setDiscountValue(req.discountValue() != null ? req.discountValue() : BigDecimal.ZERO);
        order.setNotes(req.notes());
        // Placeholder totals — updated after items are saved
        order.setSubtotal(BigDecimal.ZERO);
        order.setOrderDiscount(BigDecimal.ZERO);
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setGrandTotal(BigDecimal.ZERO);
        Order savedOrder = repository.save(order);

        // 5. Save items and accumulate subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CreateOrderRequest.OrderItemRequest itemReq : req.items()) {
            Product product = productRepository.findById(itemReq.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemReq.productId()));

            OrderItem item = new OrderItem();
            item.setOrder(savedOrder);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setProductSku(null); // Product has no dedicated SKU field
            item.setUnitPrice(itemReq.unitPrice() != null ? itemReq.unitPrice() : product.getSellingPrice());
            item.setQuantity(itemReq.quantity());
            item.setDiscountType(itemReq.discountType());
            item.setDiscountValue(itemReq.discountValue() != null ? itemReq.discountValue() : BigDecimal.ZERO);
            item.setNotes(itemReq.notes());
            item.recalculate();

            orderItemRepository.save(item);
            subtotal = subtotal.add(item.getLineTotal());
        }

        // 6. Calculate order-level discount
        BigDecimal orderDiscount = calculateDiscount(subtotal, req.discountType(), savedOrder.getDiscountValue());

        // 7. Handle delivery if DELIVERY type
        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (req.orderType() == OrderType.DELIVERY) {
            CreateOrderRequest.OrderDeliveryRequest delivReq = req.delivery();

            // Look up standard fee from delivery_fees table (hierarchical: district > province > default)
            BigDecimal standardFee = lookupStandardFee(delivReq.deliveryCompanyId(), delivReq.provinceId(), delivReq.districtId());

            // What the customer actually pays — seller can override to 0 for free delivery
            BigDecimal charged = delivReq.deliveryFeeCharged() != null ? delivReq.deliveryFeeCharged() : standardFee;

            Province province = delivReq.provinceId() != null
                ? provinceRepository.findById(delivReq.provinceId()).orElse(null) : null;
            District district = delivReq.districtId() != null
                ? districtRepository.findById(delivReq.districtId()).orElse(null) : null;

            OrderDelivery delivery = new OrderDelivery();
            delivery.setOrder(savedOrder);
            delivery.setDeliveryCompanyId(delivReq.deliveryCompanyId());
            delivery.setRecipientName(delivReq.recipientName());
            delivery.setRecipientPhone(delivReq.recipientPhone());
            delivery.setDeliveryAddress(delivReq.deliveryAddress());
            delivery.setProvince(province);
            delivery.setDistrict(district);
            delivery.setDeliveryFeeStandard(standardFee);
            delivery.setDeliveryFeeCharged(charged);
            delivery.setDeliveryNotes(delivReq.deliveryNotes());
            delivery.setDeliveryStatus(DeliveryStatus.PENDING);
            orderDeliveryRepository.save(delivery);

            deliveryFee = charged;
        }

        // 8. Finalize and update order totals
        savedOrder.setSubtotal(subtotal);
        savedOrder.setOrderDiscount(orderDiscount);
        savedOrder.setDeliveryFee(deliveryFee);
        savedOrder.setGrandTotal(subtotal.subtract(orderDiscount).add(deliveryFee));
        return repository.save(savedOrder);
    }

    // ── Helper methods ─────────────────────────────────────────────────────────

    private BigDecimal calculateDiscount(BigDecimal base, DiscountType type, BigDecimal value) {
        if (type == null || value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (type == DiscountType.PERCENTAGE) {
            return base.multiply(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        return value; // AMOUNT
    }

    /**
     * Look up delivery fee with hierarchical fallback:
     * district-specific → province-wide → global default → 0
     */
    private BigDecimal lookupStandardFee(UUID targetCompanyId, UUID provinceId, UUID districtId) {
        if (targetCompanyId == null) return BigDecimal.ZERO;
        if (provinceId != null && districtId != null) {
            var fee = deliveryFeeRepository.findFeeByDistrict(targetCompanyId, provinceId, districtId);
            if (fee.isPresent()) return fee.get().getFee();
        }
        if (provinceId != null) {
            var fee = deliveryFeeRepository.findFeeByProvince(targetCompanyId, provinceId);
            if (fee.isPresent()) return fee.get().getFee();
        }
        return deliveryFeeRepository.findDefaultFee(targetCompanyId)
            .map(DeliveryFee::getFee)
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Generate a unique order number: ORD-YYYYMMDD-XXXXXXXX
     * XXXXXXXX = first 8 chars of a random UUID (uppercase)
     */
    private String generateOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD-" + date + "-" + suffix;
    }

    /**
     * Update orderStatus, paymentStatus, deliveryStatus, and trackingNumber
     * for an order whose orderType is DELIVERY.
     */
    @Transactional
    public Order updateDeliveryStatus(UUID companyId, UUID orderId, UpdateDeliveryStatusRequest req) {
        Order order = repository.findById(orderId)
            .filter(o -> o.getCompanyId().equals(companyId))
            .filter(o -> !o.isDeleted())
            .filter(o -> o.getOrderType() == OrderType.DELIVERY)
            .orElseThrow(() -> new IllegalArgumentException("Order not found or is not a DELIVERY order"));

        if (req.orderStatus() != null) order.setOrderStatus(req.orderStatus());
        if (req.paymentStatus() != null) order.setPaymentStatus(req.paymentStatus());

        if (req.deliveryStatus() != null || req.trackingNumber() != null) {
            OrderDelivery delivery = orderDeliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("Delivery record not found for this order"));
            if (req.deliveryStatus() != null) {
                delivery.setDeliveryStatus(req.deliveryStatus());
                if (req.deliveryStatus() == DeliveryStatus.DELIVERED) {
                    delivery.setDeliveredAt(OffsetDateTime.now());
                }
            }
            if (req.trackingNumber() != null) delivery.setTrackingNumber(req.trackingNumber());
            orderDeliveryRepository.save(delivery);
        }

        return repository.save(order);
    }

    /** Update order status and payment status (billing side). */
    @Transactional
    public Order updateBillingStatus(UUID companyId, UUID orderId, UpdateBillingStatusRequest req) {
        Order order = repository.findById(orderId)
            .filter(o -> o.getCompanyId().equals(companyId))
            .filter(o -> !o.isDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (req.orderStatus() != null) order.setOrderStatus(req.orderStatus());
        if (req.paymentStatus() != null) order.setPaymentStatus(req.paymentStatus());

        return repository.save(order);
    }

    /** Update delivery status and tracking number (shipping side). DELIVERY orders only. */
    @Transactional
    public Order updateShippingStatus(UUID companyId, UUID orderId, UpdateShippingStatusRequest req) {
        Order order = repository.findById(orderId)
            .filter(o -> o.getCompanyId().equals(companyId))
            .filter(o -> !o.isDeleted())
            .filter(o -> o.getOrderType() == OrderType.DELIVERY)
            .orElseThrow(() -> new IllegalArgumentException("Order not found or is not a DELIVERY order"));

        OrderDelivery delivery = orderDeliveryRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalStateException("Delivery record not found for this order"));

        if (req.deliveryStatus() != null) {
            delivery.setDeliveryStatus(req.deliveryStatus());
            if (req.deliveryStatus() == DeliveryStatus.DELIVERED) {
                delivery.setDeliveredAt(OffsetDateTime.now());
            }
        }
        if (req.trackingNumber() != null) delivery.setTrackingNumber(req.trackingNumber());
        orderDeliveryRepository.save(delivery);

        return repository.save(order);
    }

    // ── Inner request DTOs (used by controller and service) ───────────────────

    public record CreateOrderRequest(
        UUID customerId,
        OrderType orderType,
        com.delivery.deliveryapi.model.enums.PaymentType paymentType,
        String notes,
        DiscountType discountType,
        BigDecimal discountValue,
        java.util.List<OrderItemRequest> items,
        OrderDeliveryRequest delivery
    ) {
        public record OrderItemRequest(
            UUID productId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            DiscountType discountType,
            BigDecimal discountValue,
            String notes
        ) {}

        public record OrderDeliveryRequest(
            UUID deliveryCompanyId,
            String recipientName,
            String recipientPhone,
            String deliveryAddress,
            UUID provinceId,
            UUID districtId,
            BigDecimal deliveryFeeCharged,
            String deliveryNotes
        ) {}
    }

    public record UpdateDeliveryStatusRequest(
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        DeliveryStatus deliveryStatus,
        String trackingNumber
    ) {}

    /** PATCH /orders/{id}/billing-status — order status + payment status */
    public record UpdateBillingStatusRequest(
        OrderStatus orderStatus,
        PaymentStatus paymentStatus
    ) {}

    /** PATCH /orders/{id}/shipping-status — delivery status + tracking number */
    public record UpdateShippingStatusRequest(
        DeliveryStatus deliveryStatus,
        String trackingNumber
    ) {}

    // ── Edit request DTOs ──────────────────────────────────────────────────────

    /** PUT /orders/{id}/info — customer, payment type, notes, discount, order date */
    public record UpdateOrderInfoRequest(
        UUID customerId,
        PaymentType paymentType,
        String notes,
        DiscountType discountType,
        BigDecimal discountValue,
        OffsetDateTime orderDate
    ) {}

    /** PUT /orders/{id}/items — full item reconciliation */
    public record UpdateOrderItemsRequest(
        List<OrderItemEditRequest> items
    ) {
        /**
         * id = null  → new item (add)
         * id present → update that existing item
         * existing items not in this list → removed
         */
        public record OrderItemEditRequest(
            UUID id,
            UUID productId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            DiscountType discountType,
            BigDecimal discountValue,
            String notes
        ) {}
    }

    /** PUT /orders/{id}/delivery — recipient and fee, DELIVERY orders only */
    public record UpdateOrderDeliveryRequest(
        UUID deliveryCompanyId,
        String recipientName,
        String recipientPhone,
        String deliveryAddress,
        UUID provinceId,
        UUID districtId,
        BigDecimal deliveryFeeCharged,
        String deliveryNotes
    ) {}

    // ── Edit service methods ───────────────────────────────────────────────────

    /** Update order header info: customer, payment type, notes, discount, order date. */
    @Transactional
    public Order updateOrderInfo(UUID companyId, UUID orderId, UpdateOrderInfoRequest req) {
        Order order = repository.findById(orderId)
            .filter(o -> o.getCompanyId().equals(companyId))
            .filter(o -> !o.isDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (req.customerId() != null) {
            User customer = userRepository.findById(req.customerId())
                .filter(u -> u.getUserType() == UserType.CUSTOMER)
                .filter(u -> u.getCompany() != null && u.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found or does not belong to this company"));
            order.setCustomer(customer);
        }
        if (req.paymentType() != null) order.setPaymentType(req.paymentType());
        if (req.notes() != null) order.setNotes(req.notes());
        if (req.orderDate() != null) order.setOrderDate(req.orderDate());

        boolean discountChanged = req.discountType() != null || req.discountValue() != null;
        if (req.discountType() != null) order.setDiscountType(req.discountType());
        if (req.discountValue() != null) order.setDiscountValue(req.discountValue());

        if (discountChanged) {
            BigDecimal orderDiscount = calculateDiscount(order.getSubtotal(), order.getDiscountType(), order.getDiscountValue());
            order.setOrderDiscount(orderDiscount);
            order.setGrandTotal(order.getSubtotal().subtract(orderDiscount).add(order.getDeliveryFee()));
        }

        return repository.save(order);
    }

    /** Replace order items. Existing items not in the list are removed. */
    @Transactional
    public Order updateOrderItems(UUID companyId, UUID orderId, UpdateOrderItemsRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        Order order = repository.findById(orderId)
            .filter(o -> o.getCompanyId().equals(companyId))
            .filter(o -> !o.isDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        List<OrderItem> existingItems = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        Map<UUID, OrderItem> existingById = existingItems.stream()
            .collect(Collectors.toMap(OrderItem::getId, i -> i));

        Set<UUID> keptIds = req.items().stream()
            .filter(r -> r.id() != null)
            .map(UpdateOrderItemsRequest.OrderItemEditRequest::id)
            .collect(Collectors.toSet());

        existingItems.stream()
            .filter(i -> !keptIds.contains(i.getId()))
            .forEach(orderItemRepository::delete);
        orderItemRepository.flush();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (UpdateOrderItemsRequest.OrderItemEditRequest itemReq : req.items()) {
            OrderItem item;
            if (itemReq.id() != null) {
                item = existingById.get(itemReq.id());
                if (item == null) {
                    throw new IllegalArgumentException("Order item not found: " + itemReq.id());
                }
            } else {
                item = new OrderItem();
                item.setOrder(order);
            }

            if (itemReq.productId() != null) {
                Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemReq.productId()));
                item.setProduct(product);
                item.setProductName(product.getName());
                item.setProductSku(null);
                if (itemReq.unitPrice() != null) {
                    item.setUnitPrice(itemReq.unitPrice());
                } else if (item.getUnitPrice() == null) {
                    item.setUnitPrice(product.getSellingPrice());
                }
            }
            if (itemReq.quantity() != null) item.setQuantity(itemReq.quantity());
            if (itemReq.discountType() != null) item.setDiscountType(itemReq.discountType());
            if (itemReq.discountValue() != null) item.setDiscountValue(itemReq.discountValue());
            if (itemReq.notes() != null) item.setNotes(itemReq.notes());
            item.recalculate();

            orderItemRepository.save(item);
            subtotal = subtotal.add(item.getLineTotal());
        }

        BigDecimal orderDiscount = calculateDiscount(subtotal, order.getDiscountType(), order.getDiscountValue());
        order.setSubtotal(subtotal);
        order.setOrderDiscount(orderDiscount);
        order.setGrandTotal(subtotal.subtract(orderDiscount).add(order.getDeliveryFee()));
        return repository.save(order);
    }

    /** Update delivery recipient and fee. Order must be of type DELIVERY. */
    @Transactional
    public Order updateOrderDelivery(UUID companyId, UUID orderId, UpdateOrderDeliveryRequest req) {
        Order order = repository.findById(orderId)
            .filter(o -> o.getCompanyId().equals(companyId))
            .filter(o -> !o.isDeleted())
            .filter(o -> o.getOrderType() == OrderType.DELIVERY)
            .orElseThrow(() -> new IllegalArgumentException("Order not found or is not a DELIVERY order"));

        OrderDelivery delivery = orderDeliveryRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalStateException("Delivery record not found for this order"));

        if (req.deliveryCompanyId() != null) delivery.setDeliveryCompanyId(req.deliveryCompanyId());
        if (req.recipientName() != null) delivery.setRecipientName(req.recipientName());
        if (req.recipientPhone() != null) delivery.setRecipientPhone(req.recipientPhone());
        if (req.deliveryAddress() != null) delivery.setDeliveryAddress(req.deliveryAddress());
        if (req.deliveryNotes() != null) delivery.setDeliveryNotes(req.deliveryNotes());
        if (req.provinceId() != null) {
            delivery.setProvince(provinceRepository.findById(req.provinceId()).orElse(null));
        }
        if (req.districtId() != null) {
            delivery.setDistrict(districtRepository.findById(req.districtId()).orElse(null));
        }
        if (req.deliveryFeeCharged() != null) {
            delivery.setDeliveryFeeCharged(req.deliveryFeeCharged());
            order.setDeliveryFee(req.deliveryFeeCharged());
            order.setGrandTotal(order.getSubtotal().subtract(order.getOrderDiscount()).add(req.deliveryFeeCharged()));
            repository.save(order);
        }
        orderDeliveryRepository.save(delivery);

        return repository.findById(orderId).orElseThrow();
    }
}
