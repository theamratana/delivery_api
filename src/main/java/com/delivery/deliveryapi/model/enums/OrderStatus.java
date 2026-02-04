package com.delivery.deliveryapi.model.enums;

import lombok.Getter;

/**
 * Order status enum with multi-language support.
 * Tracks the order lifecycle from creation to completion.
 */
@Getter
public enum OrderStatus {
    PENDING("Pending", "កំពុងរង់ចាំ"),
    CONFIRMED("Confirmed", "បានបញ្ជាក់"),
    PROCESSING("Processing", "កំពុងដំណើរការ"),
    READY("Ready", "រួចរាល់"),
    COMPLETED("Completed", "បានបញ្ចប់"),
    CANCELLED("Cancelled", "បានលុបចោល");

    private final String name;
    private final String khmerName;

    OrderStatus(String name, String khmerName) {
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

    /**
     * Check if this status can transition to the target status.
     * @param target Target status
     * @return true if transition is valid
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (this == CANCELLED || this == COMPLETED) {
            return false; // Terminal states
        }
        
        switch (this) {
            case PENDING:
                return target == CONFIRMED || target == CANCELLED;
            case CONFIRMED:
                return target == PROCESSING || target == CANCELLED;
            case PROCESSING:
                return target == READY || target == CANCELLED;
            case READY:
                return target == COMPLETED || target == CANCELLED;
            default:
                return false;
        }
    }
}
