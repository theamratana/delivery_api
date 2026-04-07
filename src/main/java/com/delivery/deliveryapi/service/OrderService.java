package com.delivery.deliveryapi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
}
