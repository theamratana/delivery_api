package com.delivery.deliveryapi.model.enums;

import lombok.Getter;

/**
 * Order type enum with multi-language support.
 * Defines how customer interacts with the order.
 */
@Getter
public enum OrderType {
    WALK_IN("Walk-In", "អតិថិជនមកដោយផ្ទាល់"),
    PICKUP("Pickup", "មកយកដល់ហាង"),
    DELIVERY("Delivery", "ដាក់ផ្ញើរ");

    private final String name;
    private final String khmerName;

    OrderType(String name, String khmerName) {
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
