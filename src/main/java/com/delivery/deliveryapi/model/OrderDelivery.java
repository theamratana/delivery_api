package com.delivery.deliveryapi.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.delivery.deliveryapi.model.enums.DeliveryStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "order_deliveries",
    indexes = {
        @Index(name = "idx_order_deliveries_order_id", columnList = "order_id"),
        @Index(name = "idx_order_deliveries_status", columnList = "delivery_status"),
        @Index(name = "idx_order_deliveries_province", columnList = "province_id"),
        @Index(name = "idx_order_deliveries_district", columnList = "district_id")
    }
)
public class OrderDelivery extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // Recipient info — can differ from customer (e.g. customer buys for someone else)
    @Column(name = "recipient_name", nullable = false, length = 255)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    // Delivery address — override from customer's default_address
    @Column(name = "delivery_address", nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id")
    private Province province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    // What the delivery_fees table says it should cost (looked up at order time)
    @Column(name = "delivery_fee_standard", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryFeeStandard = BigDecimal.ZERO;

    // What the customer actually pays — 0 means seller made it free
    // This value is mirrored to orders.delivery_fee for billing convenience
    @Column(name = "delivery_fee_charged", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryFeeCharged = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    // The delivery company chosen for this order (references companies table)
    @Column(name = "delivery_company_id")
    private UUID deliveryCompanyId;

    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;
}
