package com.delivery.deliveryapi.model.enums;

import lombok.Getter;

/**
 * Address type enum with multi-language support.
 * Defines the purpose of an address.
 */
@Getter
public enum AddressType {
    SHIPPING("Shipping Address", "អាសយដ្ឋានដឹកជញ្ជូន"),
    BILLING("Billing Address", "អាសយដ្ឋានវិក័យប័ត្រ");

    private final String name;
    private final String khmerName;

    AddressType(String name, String khmerName) {
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
