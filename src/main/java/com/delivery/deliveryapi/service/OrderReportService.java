package com.delivery.deliveryapi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.delivery.deliveryapi.model.enums.OrderStatus;
import com.delivery.deliveryapi.model.enums.OrderType;
import com.delivery.deliveryapi.model.enums.PaymentStatus;
import com.delivery.deliveryapi.repo.CompanyRepository;
import com.delivery.deliveryapi.repo.OrderDeliveryRepository;
import com.delivery.deliveryapi.repo.OrderItemRepository;
import com.delivery.deliveryapi.repo.OrderRepository;

@Service
public class OrderReportService {

    private static final OffsetDateTime MIN_DATE = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime MAX_DATE = OffsetDateTime.of(2099, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderDeliveryRepository orderDeliveryRepository;
    private final CompanyRepository companyRepository;

    public OrderReportService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderDeliveryRepository orderDeliveryRepository,
            CompanyRepository companyRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderDeliveryRepository = orderDeliveryRepository;
        this.companyRepository = companyRepository;
    }

    // ── Summary ───────────────────────────────────────────────────────────────

    public Summary getSummary(UUID companyId, OffsetDateTime from, OffsetDateTime to) {
        Object[] row = orderRepository.statsSummary(companyId, ef(from), et(to)).get(0);
        return new Summary(
            ((Number) row[0]).longValue(),
            row[1] instanceof BigDecimal bd ? bd : BigDecimal.valueOf(((Number) row[1]).doubleValue()),
            row[2] instanceof BigDecimal bd ? bd : BigDecimal.valueOf(((Number) row[2]).doubleValue())
        );
    }

    // ── By order status ───────────────────────────────────────────────────────

    public List<StatusBreakdown> getByStatus(UUID companyId, OffsetDateTime from, OffsetDateTime to) {
        return orderRepository.statsByStatus(companyId, ef(from), et(to)).stream()
            .map(r -> new StatusBreakdown(
                (OrderStatus) r[0],
                ((Number) r[1]).longValue(),
                (BigDecimal) r[2]))
            .collect(Collectors.toList());
    }

    // ── By order type ─────────────────────────────────────────────────────────

    public List<TypeBreakdown> getByType(UUID companyId, OffsetDateTime from, OffsetDateTime to) {
        return orderRepository.statsByType(companyId, ef(from), et(to)).stream()
            .map(r -> new TypeBreakdown(
                (OrderType) r[0],
                ((Number) r[1]).longValue(),
                (BigDecimal) r[2]))
            .collect(Collectors.toList());
    }

    // ── By payment status ─────────────────────────────────────────────────────

    public List<PaymentStatusBreakdown> getByPaymentStatus(UUID companyId, OffsetDateTime from, OffsetDateTime to) {
        return orderRepository.statsByPaymentStatus(companyId, ef(from), et(to)).stream()
            .map(r -> new PaymentStatusBreakdown(
                (PaymentStatus) r[0],
                ((Number) r[1]).longValue(),
                (BigDecimal) r[2]))
            .collect(Collectors.toList());
    }

    // ── By delivery company ───────────────────────────────────────────────────

    /** Returns order count and total delivery fees grouped by delivery company.
     *  Company names are resolved in a single batch lookup. */
    public List<DeliveryCompanyBreakdown> getByDeliveryCompany(UUID companyId, OffsetDateTime from, OffsetDateTime to) {
        List<Object[]> rows = orderDeliveryRepository.statsByDeliveryCompany(companyId, ef(from), et(to));
        List<UUID> companyIds = rows.stream().map(r -> (UUID) r[0]).collect(Collectors.toList());
        Map<UUID, String> nameMap = companyRepository.findAllById(companyIds).stream()
            .collect(Collectors.toMap(c -> c.getId(), c -> c.getName()));
        return rows.stream()
            .map(r -> {
                UUID dcId = (UUID) r[0];
                return new DeliveryCompanyBreakdown(
                    dcId,
                    nameMap.getOrDefault(dcId, "Unknown"),
                    ((Number) r[1]).longValue(),
                    (BigDecimal) r[2]);
            })
            .collect(Collectors.toList());
    }

    // ── Best-selling products ─────────────────────────────────────────────────

    public List<BestSellingProduct> getBestSellingProducts(
            UUID companyId, OffsetDateTime from, OffsetDateTime to, int limit) {
        return orderItemRepository
            .statsBestSellingProducts(companyId, ef(from), et(to), PageRequest.of(0, limit))
            .stream()
            .map(r -> new BestSellingProduct(
                (UUID) r[0],
                (String) r[1],
                (String) r[2],
                (BigDecimal) r[3],
                (BigDecimal) r[4]))
            .collect(Collectors.toList());
    }

    // ── Most loyal customers ──────────────────────────────────────────────────

    /** Returns customers ranked by number of orders placed (repeat buyers first). */
    public List<LoyalCustomer> getLoyalCustomers(
            UUID companyId, OffsetDateTime from, OffsetDateTime to, int limit) {
        return orderRepository
            .statsLoyalCustomers(companyId, ef(from), et(to), PageRequest.of(0, limit))
            .stream()
            .map(r -> new LoyalCustomer(
                (UUID) r[0],
                (String) r[1],
                (String) r[2],
                ((Number) r[3]).longValue(),
                (BigDecimal) r[4]))
            .collect(Collectors.toList());
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private OffsetDateTime ef(OffsetDateTime from) { return from != null ? from : MIN_DATE; }
    private OffsetDateTime et(OffsetDateTime to)   { return to   != null ? to   : MAX_DATE; }

    // ── Result records ────────────────────────────────────────────────────────

    public record Summary(
        long totalOrders,
        BigDecimal totalRevenue,
        BigDecimal avgOrderValue
    ) {}

    public record StatusBreakdown(
        OrderStatus status,
        long count,
        BigDecimal revenue
    ) {}

    public record TypeBreakdown(
        OrderType type,
        long count,
        BigDecimal revenue
    ) {}

    public record PaymentStatusBreakdown(
        PaymentStatus paymentStatus,
        long count,
        BigDecimal revenue
    ) {}

    public record DeliveryCompanyBreakdown(
        UUID deliveryCompanyId,
        String deliveryCompanyName,
        long count,
        BigDecimal totalFee
    ) {}

    public record BestSellingProduct(
        UUID productId,
        String productName,
        String productSku,
        BigDecimal totalQty,
        BigDecimal totalRevenue
    ) {}

    public record LoyalCustomer(
        UUID customerId,
        String customerName,
        String customerPhone,
        long orderCount,
        BigDecimal totalRevenue
    ) {}
}
