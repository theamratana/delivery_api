package com.delivery.deliveryapi.model.enums;

import lombok.Getter;

/**
 * Order source enum with multi-language support.
 * Defines where the order originated from.
 */
@Getter
public enum OrderSource {
    POS("POS System", "ប្រព័ន្ធលក់"),
    WEB("Website", "គេហទំព័រ"),
    MOBILE_APP("Mobile App", "កម្មវិធីទូរស័ព្ទ"),
    MARKETPLACE("Marketplace", "ទីផ្សារអនឡាញ");

    private final String name;
    private final String khmerName;

    OrderSource(String name, String khmerName) {
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
