package com.delivery.deliveryapi.model;

public enum DeliveryStatus {
    // Creation phase
    CREATED("Created", "បង្កើតថ្មី"),
    AWAITING_PICKUP("Awaiting Pickup", "រង់ចាំដឹកទំនិញ"),
    
    // Pickup phase
    PICKED_UP("Picked Up", "បានយកទំនិញ"),
    AT_WAREHOUSE("At Warehouse", "នៅឃ្លាំងទំនិញ"),
    
    // Transit phase
    IN_TRANSIT("In Transit", "កំពុងដឹកជញ្ជូន"),
    OUT_FOR_DELIVERY("Out for Delivery", "កំពុងដឹកទៅផ្ដល់"),
    
    // Delivery attempts
    DELIVERY_ATTEMPTED("Delivery Attempted", "ព្យាយាមដឹកជញ្ជូន"),
    RECEIVER_NOT_AVAILABLE("Receiver Not Available", "អ្នកទទួលមិនមាន"),
    
    // Completion phase
    DELIVERED("Delivered", "បានដឹកជញ្ជូន"),
    COMPLETED("Completed", "បានបញ្ចប់"),
    
    // Failed/Problem phase
    FAILED("Failed", "បរាជ័យ"),
    CANCELLED("Cancelled", "បានបោះបង់"),
    RETURNED_TO_SENDER("Returned to Sender", "ត្រឡប់មកអ្នកផ្ញើ"),
    LOST("Lost", "បាត់បង់"),
    DAMAGED("Damaged", "ខូច");

    private final String englishName;
    private final String khmerName;

    DeliveryStatus(String englishName, String khmerName) {
        this.englishName = englishName;
        this.khmerName = khmerName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getKhmerName() {
        return khmerName;
    }

    public String getDisplayName() {
        return englishName + " / " + khmerName;
    }
}
