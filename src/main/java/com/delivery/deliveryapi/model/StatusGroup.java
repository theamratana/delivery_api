package com.delivery.deliveryapi.model;

public enum StatusGroup {
    READY_FOR_PICKUP("Ready for Pickup", "រួចរាល់សម្រាប់ដឹក"),
    IN_DELIVERY("In Delivery", "កំពុងដឹកជញ្ជូន"),
    COMPLETED("Completed", "បានបញ្ចប់"),
    FAILED("Failed", "បរាជ័យ");

    private final String englishName;
    private final String khmerName;

    StatusGroup(String englishName, String khmerName) {
        this.englishName = englishName;
        this.khmerName = khmerName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getKhmerName() {
        return khmerName;
    }
}
