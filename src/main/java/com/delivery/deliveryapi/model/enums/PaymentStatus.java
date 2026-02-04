package com.delivery.deliveryapi.model.enums;

import lombok.Getter;

/**
 * Payment status enum with multi-language support.
 * Tracks the payment state of an order.
 */
@Getter
public enum PaymentStatus {
    PENDING("Pending", "កំពុងរង់ចាំ"),
    COMPLETED("Completed", "បានបញ្ចប់"),
    PARTIAL("Partial", "បង់មួយផ្នែក"),
    REFUNDED("Refunded", "បានសងប្រាក់វិញ");

    private final String name;
    private final String khmerName;

    PaymentStatus(String name, String khmerName) {
        this.name = name;
        this.khmerName = khmerName;
    }

    /**
     * Get label by language code.
     * @param lang "en" or "km"
     * @return Localized label
     */
    public String getLabel(String lang) {
        return "km".equalsIgnoreCase(lang) ? khmerName : name;
    }
}
