package com.delivery.deliveryapi.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import com.delivery.deliveryapi.model.enums.DiscountType;
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
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "order_items",
    indexes = {
        @Index(name = "idx_order_items_order_id", columnList = "order_id"),
        @Index(name = "idx_order_items_product_id", columnList = "product_id")
    }
)
public class OrderItem extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Snapshots at time of sale — product data may change later
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_sku", length = 100)
    private String productSku;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    // Item-level discount (NULL = no discount on this line)
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    // sub_total = quantity × unit_price  (BEFORE discount)
    @Column(name = "sub_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal subTotal = BigDecimal.ZERO;

    // line_discount = calculated discount amount
    @Column(name = "line_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineDiscount = BigDecimal.ZERO;

    // line_total = sub_total - line_discount  (AFTER discount)
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Recalculate sub_total, line_discount, line_total.
     * Call this whenever quantity, unit_price, or discount changes.
     */
    public void recalculate() {
        this.subTotal = this.quantity.multiply(this.unitPrice);

        if (this.discountType == null || this.discountValue.compareTo(BigDecimal.ZERO) == 0) {
            this.lineDiscount = BigDecimal.ZERO;
        } else if (this.discountType == DiscountType.PERCENTAGE) {
            this.lineDiscount = this.subTotal
                .multiply(this.discountValue)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            // AMOUNT
            this.lineDiscount = this.discountValue;
        }

        this.lineTotal = this.subTotal.subtract(this.lineDiscount);
    }
}
