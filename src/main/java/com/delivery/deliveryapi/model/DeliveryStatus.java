package com.delivery.deliveryapi.model;

public enum DeliveryStatus {
    // READY FOR PICKUP GROUP - រួចរាល់សម្រាប់ដឹក
    CREATED("Created", "បានបង្កើត", StatusGroup.READY_FOR_PICKUP),
    DROPPED_OFF("Dropped Off", "បានដាក់ផ្ញើរ", StatusGroup.READY_FOR_PICKUP),
    
    // IN DELIVERY GROUP - កំពុងដឹកជញ្ជូន
    PICKED_UP("Picked Up", "បានយកទំនិញ", StatusGroup.IN_DELIVERY),
    IN_TRANSIT("In Transit", "កំពុងដឹក", StatusGroup.IN_DELIVERY),
    OUT_FOR_DELIVERY("Out for Delivery", "កំពុងដឹកទៅផ្ទះ", StatusGroup.IN_DELIVERY),
    
    // COMPLETED GROUP - បានបញ្ចប់
    DELIVERED("Delivered", "បានដឹកជញ្ជូន", StatusGroup.COMPLETED),
    
    // FAILED GROUP - បរាជ័យ
    CANCELLED("Cancelled", "បានលុបចោល", StatusGroup.FAILED),
    FAILED_DELIVERY("Failed Delivery", "ដឹកមិនបាន", StatusGroup.FAILED),
    RETURNED("Returned", "បានត្រឡប់", StatusGroup.FAILED),
    
    // Deprecated - kept for backward compatibility with existing data
    @Deprecated
    ASSIGNED("Assigned", "បានចាត់តាំង", StatusGroup.READY_FOR_PICKUP),
    
    @Deprecated
    FAILED("Failed", "បរាជ័យ", StatusGroup.FAILED);

    private final String englishName;
    private final String khmerName;
    private final StatusGroup group;

    DeliveryStatus(String englishName, String khmerName, StatusGroup group) {
        this.englishName = englishName;
        this.khmerName = khmerName;
        this.group = group;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getKhmerName() {
        return khmerName;
    }

    public StatusGroup getGroup() {
        return group;
    }

    /**
     * Check if this status belongs to a specific group
     */
    public boolean isInGroup(StatusGroup statusGroup) {
        return this.group == statusGroup;
    }

    /**
     * Check if the delivery is still in progress (not completed or failed)
     */
    public boolean isInProgress() {
        return this.group == StatusGroup.READY_FOR_PICKUP || this.group == StatusGroup.IN_DELIVERY;
    }

    /**
     * Check if the delivery has reached a final state
     */
    public boolean isFinal() {
        return this.group == StatusGroup.COMPLETED || this.group == StatusGroup.FAILED;
    }
}