package com.delivery.deliveryapi.model;

public enum UserRole {
    SYSTEM_ADMINISTRATOR,  // System-wide administrator with full access
    OWNER,      // Company creator, full access
    MANAGER,    // Can manage team, orders
    STAFF,      // Customer service, basic operations
    DRIVER      // Delivery personnel
}