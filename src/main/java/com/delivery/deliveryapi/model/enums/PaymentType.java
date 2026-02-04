package com.delivery.deliveryapi.model.enums;

import lombok.Getter;

/**
 * Payment type enum with multi-language support.
 * Defines how payment is handled.
 */
@Getter
public enum PaymentType {
    PAID("Paid", "បង់ប្រាក់រួច"),
    COD("Cash on Delivery", "បង់ប្រាក់ពេលទទួល"),
    PAID_DELIVERY_COD("Paid, Delivery Fee COD", "បង់ប្រាក់ផលិតផល ថ្លៃដឹកពេលទទួល"),
    PARTIAL("Partial Payment", "បង់មួយផ្នែក");

    private final String name;
    private final String khmerName;

    PaymentType(String name, String khmerName) {
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
