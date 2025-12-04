#!/bin/bash

# Test script for PUT /deliveries/batch/{batchId} endpoint
# Step 1: Create a new delivery
# Step 2: Edit the delivery that was just created

TOKEN=$(cat /tmp/token.txt)

echo "========================================="
echo "Step 1: Creating a new delivery..."
echo "========================================="

CREATE_RESPONSE=$(curl -s -X POST "http://localhost:8081/api/deliveries" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "senderName": "Custom Sender Name",
    "senderPhone": "+85512345678",
    "receiverPhone": "+85599999999",
    "receiverName": "Test Edit Receiver",
    "deliveryType": "COMPANY",
    "companyName": "Test Edit Company",
    "companyPhone": "+85577777777",
    "deliveryAddress": "Test Edit Address 123",
    "deliveryProvince": "Phnom Penh Capital",
    "deliveryDistrict": "Chamkar Mon Khan",
    "deliveryFee": 2.50,
    "deliveryDiscount": 0.00,
    "orderDiscount": 0.00,
    "actualDeliveryCost": 2.50,
    "paymentMethod": "cod",
    "items": [
      {
        "itemDescription": "Original Item 1",
        "quantity": 1,
        "price": 10.00,
        "itemPhotos": ["https://example.com/original1.jpg"]
      },
      {
        "itemDescription": "Original Item 2",
        "quantity": 2,
        "price": 5.00,
        "itemPhotos": ["https://example.com/original2.jpg"]
      }
    ]
  }')

echo "$CREATE_RESPONSE"
echo ""

# Extract batchId from response
BATCH_ID=$(echo "$CREATE_RESPONSE" | grep -o '"batchId":"[^"]*' | cut -d'"' -f4)

if [ -z "$BATCH_ID" ]; then
  echo "ERROR: Failed to create delivery or extract batchId"
  exit 1
fi

echo "Created delivery with batchId: $BATCH_ID"
echo ""
echo "========================================="
echo "Step 2: Editing the delivery..."
echo "========================================="

sleep 2

EDIT_RESPONSE=$(curl -s -X PUT "http://localhost:8081/api/deliveries/batch/$BATCH_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "senderName": "EDITED Sender Override",
    "senderPhone": "+85587654321",
    "receiverPhone": "+85588888888",
    "receiverName": "UPDATED Receiver Name",
    "deliveryType": "COMPANY",
    "companyName": "UPDATED Delivery Company",
    "deliveryAddress": "UPDATED Address 999",
    "deliveryProvince": "Phnom Penh Capital",
    "deliveryDistrict": "Chamkar Mon Khan",
    "deliveryFee": 5.00,
    "deliveryDiscount": 1.00,
    "orderDiscount": 0.50,
    "actualDeliveryCost": 5.00,
    "paymentMethod": "cod",
    "items": [
      {
        "itemDescription": "COMPLETELY NEW Item A",
        "quantity": 3,
        "price": 15.00,
        "itemPhotos": ["https://example.com/updated-a.jpg"]
      },
      {
        "itemDescription": "COMPLETELY NEW Item B",
        "quantity": 1,
        "price": 25.00,
        "itemPhotos": ["https://example.com/updated-b.jpg"]
      },
      {
        "itemDescription": "COMPLETELY NEW Item C",
        "quantity": 5,
        "price": 8.00,
        "itemPhotos": ["https://example.com/updated-c.jpg"]
      }
    ]
  }')

echo "$EDIT_RESPONSE"
echo ""

# Verify the edit
echo "========================================="
echo "Verification: Fetching updated delivery..."
echo "========================================="

sleep 1

VERIFY_RESPONSE=$(curl -s -X GET "http://localhost:8081/api/deliveries/batch/$BATCH_ID" \
  -H "Authorization: Bearer $TOKEN")

echo "$VERIFY_RESPONSE"
echo ""

# Check if edit was successful
if echo "$VERIFY_RESPONSE" | grep -q "UPDATED"; then
  echo "✅ SUCCESS: Delivery was updated correctly!"
  echo "   - Found 'UPDATED' in response"
  
  # Check item count
  ITEM_COUNT=$(echo "$VERIFY_RESPONSE" | grep -o '"itemCount":[0-9]*' | cut -d':' -f2)
  echo "   - Item count: $ITEM_COUNT (expected: 3)"
  
  # Check delivery fee
  DELIVERY_FEE=$(echo "$VERIFY_RESPONSE" | grep -o '"deliveryFee":[0-9.]*' | cut -d':' -f2)
  echo "   - Delivery fee: $DELIVERY_FEE (expected: 5.00)"
  
  # Check sender fields are preserved
  SENDER_NAME=$(echo "$VERIFY_RESPONSE" | grep -o '"senderName":"[^"]*' | cut -d'"' -f4)
  SENDER_PHONE=$(echo "$VERIFY_RESPONSE" | grep -o '"senderPhone":"[^"]*' | cut -d'"' -f4)
  echo "   - Sender name: $SENDER_NAME (expected: 'EDITED Sender Override')"
  echo "   - Sender phone: $SENDER_PHONE (expected: '+85587654321')"
  
  if [ "$SENDER_NAME" = "EDITED Sender Override" ] && [ "$SENDER_PHONE" = "+85587654321" ]; then
    echo "   ✅ Sender override worked correctly!"
  else
    echo "   ❌ ERROR: Sender override failed! Got: '$SENDER_NAME' / '$SENDER_PHONE'"
    exit 1
  fi
else
  echo "❌ FAILED: Delivery edit verification failed"
  exit 1
fi
