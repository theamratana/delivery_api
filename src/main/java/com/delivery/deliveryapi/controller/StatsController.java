package com.delivery.deliveryapi.controller;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.service.OrderReportService;

@RestController
@RequestMapping("/stats")
public class StatsController {

    private final OrderReportService orderReportService;
    private final UserRepository userRepository;

    public StatsController(OrderReportService orderReportService, UserRepository userRepository) {
        this.orderReportService = orderReportService;
        this.userRepository = userRepository;
    }

    // ── GET /stats/orders ─────────────────────────────────────────────────────
    // Overall order totals: count, revenue, average order value

    @GetMapping("/orders")
    public ResponseEntity<?> orderSummary(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        return ResponseEntity.ok(orderReportService.getSummary(companyId(), from, to));
    }

    // ── GET /stats/orders/by-status ───────────────────────────────────────────
    // Order count + revenue grouped by status (PENDING, CONFIRMED, CANCELLED, …)

    @GetMapping("/orders/by-status")
    public ResponseEntity<?> ordersByStatus(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        return ResponseEntity.ok(orderReportService.getByStatus(companyId(), from, to));
    }

    // ── GET /stats/orders/by-type ─────────────────────────────────────────────
    // Order count + revenue grouped by type (WALK_IN vs DELIVERY)

    @GetMapping("/orders/by-type")
    public ResponseEntity<?> ordersByType(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        return ResponseEntity.ok(orderReportService.getByType(companyId(), from, to));
    }

    // ── GET /stats/orders/by-payment-status ──────────────────────────────────
    // Order count + revenue grouped by payment status (PENDING, PAID, …)

    @GetMapping("/orders/by-payment-status")
    public ResponseEntity<?> ordersByPaymentStatus(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        return ResponseEntity.ok(orderReportService.getByPaymentStatus(companyId(), from, to));
    }

    // ── GET /stats/delivery-companies ────────────────────────────────────────
    // Shipment count + total delivery fees grouped by delivery company (name resolved)

    @GetMapping("/delivery-companies")
    public ResponseEntity<?> byDeliveryCompany(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        return ResponseEntity.ok(orderReportService.getByDeliveryCompany(companyId(), from, to));
    }

    // ── GET /stats/best-sellers ───────────────────────────────────────────────
    // Top N products ranked by total quantity sold

    @GetMapping("/best-sellers")
    public ResponseEntity<?> bestSellers(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return ResponseEntity.ok(orderReportService.getBestSellingProducts(companyId(), from, to, safeLimit));
    }

    // ── GET /stats/best-customers ─────────────────────────────────────────────
    // Top N repeat buyers ranked by order count

    @GetMapping("/best-customers")
    public ResponseEntity<?> bestCustomers(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return ResponseEntity.ok(orderReportService.getLoyalCustomers(companyId(), from, to, safeLimit));
    }

    // ── Private helper ────────────────────────────────────────────────────────

    private UUID companyId() {
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
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getCompany() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not part of any company");
        }
        return user.getCompany().getId();
    }
}
