#!/bin/bash

# Get a valid auth token (you'll need to replace with actual token)
echo "Ì≥ù Testing delivery creation with item photos..."
echo ""

# Sample delivery creation with item photos
curl -X POST http://localhost:8081/api/deliveries \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "receiverPhone": "+85512345678",
    "receiverName": "Test Receiver",
    "pickupAddress": "123 Pickup St",
    "pickupProvinceId": "PROVINCE_UUID",
    "pickupDistrictId": "DISTRICT_UUID",
    "deliveryAddress": "456 Delivery Ave",
    "deliveryProvinceId": "PROVINCE_UUID",
    "deliveryDistrictId": "DISTRICT_UUID",
    "paymentMethod": "CASH",
    "items": [
      {
        "productName": "iPhone 15 Pro",
        "itemDescription": "Brand new iPhone 15 Pro 256GB",
        "price": 1200.00,
        "estimatedValue": 1200.00,
        "quantity": 1,
        "itemPhotos": [
          "https://example.com/photos/iphone-front.jpg",
          "https://example.com/photos/iphone-back.jpg"
        ]
      }
    ]
  }' | jq '.'

echo ""
echo "Now checking product search to verify photos are attached..."
echo ""

# Search for the created product
curl -X GET "http://localhost:8081/api/products/search?query=iPhone" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" | jq '.products[] | {id, name, productPhotos}'
