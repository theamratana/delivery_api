package com.delivery.deliveryapi.model.enums;

import lombok.Getter;

/**
 * Discount type enum with multi-language support.
 * Defines how discount is calculated.
 * Use NONE to indicate no discount is applied.
 */
@Getter
public enum DiscountType {
    NONE("None", "គ្មាន"),
    AMOUNT("Fixed Amount", "ចំនួនថេរ"),
    PERCENTAGE("Percentage", "ភាគរយ");

    private final String name;
    private final String khmerName;

    DiscountType(String name, String khmerName) {
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
