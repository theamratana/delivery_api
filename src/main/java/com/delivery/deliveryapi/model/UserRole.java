package com.delivery.deliveryapi.model;

public enum UserRole {
    OWNER,      // Company creator, full access
    MANAGER,    // Can manage team, orders
    STAFF,      // Customer service, basic operations
    DRIVER      // Delivery personnel
}