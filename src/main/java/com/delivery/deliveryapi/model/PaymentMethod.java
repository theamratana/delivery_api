package com.delivery.deliveryapi.model;

/**
 * Payment method for delivery items.
 * PAID: Payment has been collected or prepaid
 * COD: Cash on Delivery - payment to be collected upon delivery
 */
public enum PaymentMethod {
    PAID("paid", "Payment already received"),
    COD("cod", "Cash on Delivery");

    private final String code;
    private final String description;

    PaymentMethod(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentMethod fromCode(String code) {
        if (code == null) return COD; // Default to COD
        for (PaymentMethod method : PaymentMethod.values()) {
            if (method.code.equalsIgnoreCase(code)) {
                return method;
            }
        }
        return COD; // Default to COD
    }
}
