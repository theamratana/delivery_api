package com.delivery.deliveryapi.model;

public enum OtpStatus {
    PENDING,     // request created, waiting for Telegram link
    WAITING_FOR_CONTACT, // waiting for user to share contact
    SENT,        // OTP sent to Telegram
    VERIFIED,    // Code verified successfully
    EXPIRED,     // Timed out
    BLOCKED      // Too many attempts
}
