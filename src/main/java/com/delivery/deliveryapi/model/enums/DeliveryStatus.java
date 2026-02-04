package com.delivery.deliveryapi.model.enums;

import lombok.Getter;

/**
 * Delivery status enum with multi-language support.
 * Tracks the delivery lifecycle from handover to completion.
 */
@Getter
public enum DeliveryStatus {
    PENDING("Pending", "កំពុងរង់ចាំ"),
    WITH_CARRIER("With Carrier", "នៅជាមួយក្រុមហ៊ុនដឹកជញ្ជូន"),
    IN_TRANSIT("In Transit", "កំពុងដឹកជញ្ជូន"),
    DELIVERED("Delivered", "បានដល់"),
    FAILED("Failed", "បរាជ័យ"),
    RETURNED("Returned", "បានត្រឡប់មកវិញ");

    private final String name;
    private final String khmerName;

    DeliveryStatus(String name, String khmerName) {
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
     * Check if this is a terminal status (cannot transition further).
     * @return true if terminal
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == RETURNED;
    }

    /**
     * Check if this status can transition to the target status.
     * @param target Target status
     * @return true if transition is valid
     */
    public boolean canTransitionTo(DeliveryStatus target) {
        if (isTerminal()) {
            return false; // Terminal states
        }

        switch (this) {
            case PENDING:
                return target == WITH_CARRIER || target == IN_TRANSIT || target == DELIVERED || target == FAILED;
            case WITH_CARRIER:
                return target == IN_TRANSIT || target == FAILED;
            case IN_TRANSIT:
                return target == DELIVERED || target == FAILED;
            case FAILED:
                return target == RETURNED || target == PENDING;
            default:
                return false;
        }
    }
}
