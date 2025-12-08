# Customer Management System - Testing Guide

## Overview
The customer management system provides company-scoped customer records with verification workflow to prevent accidental data overwrites.

## Key Features

### 1. Company-Scoped Customers
- Each company maintains its own customer database
- Phone number "012345678" for Company A ≠ "012345678" for Company B
- Unique constraint: `(phone_e164, company_id)` for CUSTOMER type

### 2. Customer Verification Before Delivery
- Detects if customer exists with different information
- Frontend shows popup with old vs new values
- User chooses: Cancel, Use Existing Info, or Update Customer

### 3. No Auto-Updates
- Customer information is **immutable** through delivery creation
- Manual updates only via `PUT /customers/{id}` with ownership validation

### 4. Default Address Storage
- Customers store: `default_address`, `default_province_id`, `default_district_id`
- Used for autocomplete/suggestions in delivery forms

## API Endpoints

### Customer Management

#### 1. List/Search Customers
```bash
GET /api/customers?search=john

Authorization: Bearer {token}
```

Response:
```json
[
  {
    "id": "uuid",
    "phone": "012345678",
    "name": "John Doe",
    "address": "Street 123",
    "provinceName": "Phnom Penh",
    "districtName": "Chamkar Mon",
    "totalDeliveries": 5,
    "lastDeliveryDate": "2024-01-15T10:30:00Z"
  }
]
```

#### 2. Get Customer Details
```bash
GET /api/customers/{id}

Authorization: Bearer {token}
```

#### 3. Create Customer Manually
```bash
POST /api/customers
Content-Type: application/json
Authorization: Bearer {token}

{
  "phone": "012345678",
  "name": "John Doe",
  "address": "Street 123",
  "provinceId": "province-uuid",
  "districtId": "district-uuid"
}
```

#### 4. Update Customer
```bash
PUT /api/customers/{id}
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Johnny Doe",
  "address": "Street 456",
  "provinceId": "province-uuid",
  "districtId": "district-uuid"
}
```

### Delivery Workflow

#### 5. Verify Customer Before Delivery
```bash
POST /api/deliveries/verify-customer
Content-Type: application/json
Authorization: Bearer {token}

{
  "receiverPhone": "012345678",
  "receiverName": "Johnny D",
  "deliveryAddress": "Street 789",
  "deliveryProvinceId": "province-uuid",
  "deliveryDistrictId": "district-uuid"
}
```

Response:
```json
{
  "exists": true,
  "customerId": "customer-uuid",
  "hasChanges": true,
  "currentInfo": {
    "name": "John Doe",
    "phone": "012345678",
    "address": "Street 123",
    "provinceId": "province-uuid",
    "provinceName": "Phnom Penh",
    "districtId": "district-uuid",
    "districtName": "Chamkar Mon"
  },
  "changes": {
    "name": {
      "oldValue": "John Doe",
      "newValue": "Johnny D"
    },
    "address": {
      "oldValue": "Street 123",
      "newValue": "Street 789"
    }
  }
}
```

#### 6. Get Receiver Suggestions (Autocomplete)
```bash
GET /api/deliveries/receiver-suggestions/{phone}

Authorization: Bearer {token}
```

Response shows **only your company's delivery history** for the phone number.

## Testing Scenarios

### Scenario 1: New Customer via Delivery

1. **Create First Delivery**
```bash
POST /api/deliveries
{
  "receiverPhone": "012999888",
  "receiverName": "Alice Chan",
  "deliveryAddress": "Street 100",
  "deliveryProvinceId": "province-uuid",
  "deliveryDistrictId": "district-uuid",
  // ... other delivery fields
}
```

Expected: New customer created with company_id = sender's company

2. **Verify Customer Created**
```bash
GET /api/customers?search=alice
```

Expected: Shows "Alice Chan" with 1 delivery

### Scenario 2: Customer Verification (Same Info)

1. **Verify with Matching Info**
```bash
POST /api/deliveries/verify-customer
{
  "receiverPhone": "012999888",
  "receiverName": "Alice Chan",
  "deliveryAddress": "Street 100",
  "deliveryProvinceId": "province-uuid",
  "deliveryDistrictId": "district-uuid"
}
```

Expected:
```json
{
  "exists": true,
  "customerId": "uuid",
  "hasChanges": false,
  "currentInfo": { ... },
  "changes": {}
}
```

2. **Create Delivery Normally**
```bash
POST /api/deliveries
# Same receiver info as above
```

Expected: Uses existing customer, no popup needed

### Scenario 3: Customer Verification (Different Info)

1. **Verify with Different Name**
```bash
POST /api/deliveries/verify-customer
{
  "receiverPhone": "012999888",
  "receiverName": "Alice C",  // DIFFERENT
  "deliveryAddress": "Street 100",
  "deliveryProvinceId": "province-uuid",
  "deliveryDistrictId": "district-uuid"
}
```

Expected:
```json
{
  "exists": true,
  "hasChanges": true,
  "changes": {
    "name": {
      "oldValue": "Alice Chan",
      "newValue": "Alice C"
    }
  }
}
```

2. **Frontend Shows Popup**
Options:
- **Cancel**: Don't create delivery
- **Use Existing**: Auto-fill form with "Alice Chan" + "Street 100"
- **Update Customer**: Proceed to step 3

3. **Update Customer (if user chose Update)**
```bash
PUT /api/customers/{customerId}
{
  "name": "Alice C",
  "address": "Street 100",
  "provinceId": "province-uuid",
  "districtId": "district-uuid"
}
```

4. **Create Delivery**
```bash
POST /api/deliveries
# With updated info
```

### Scenario 4: Company Isolation

**Company A (Bling)**
```bash
# Login as Company A user
POST /api/deliveries
{
  "receiverPhone": "012111222",
  "receiverName": "Bob Bling"
}
```

**Company B (Vizeak)**
```bash
# Login as Company B user
POST /api/deliveries
{
  "receiverPhone": "012111222",  // SAME PHONE
  "receiverName": "Bob Vizeak"   // DIFFERENT NAME
}
```

Expected:
- Two separate customer records created
- Company A sees "Bob Bling" when searching "012111222"
- Company B sees "Bob Vizeak" when searching "012111222"
- No conflicts, no overwrites

### Scenario 5: Receiver Suggestions

1. **Create Multiple Deliveries**
```bash
POST /api/deliveries
# Delivery 1: Phone "012333444", Address "Street 10"
# Delivery 2: Phone "012333444", Address "Street 20"
# Delivery 3: Phone "012333444", Address "Street 30"
```

2. **Get Suggestions**
```bash
GET /api/deliveries/receiver-suggestions/012333444
```

Expected: Shows 1 suggestion with most recent delivery info (filtered by company)

### Scenario 6: Ownership Validation

**As Company A User**
```bash
GET /api/customers
# Lists only Company A's customers
```

**Try to Update Company B's Customer**
```bash
PUT /api/customers/{company-b-customer-id}
{
  "name": "Hacked"
}
```

Expected: **403 Forbidden** - ownership check prevents unauthorized edit

## Frontend Implementation Guide

### Delivery Creation Flow

```javascript
// 1. User fills form
const deliveryForm = {
  receiverPhone: "012345678",
  receiverName: "John Doe",
  deliveryAddress: "Street 123",
  deliveryProvinceId: "province-uuid",
  deliveryDistrictId: "district-uuid"
};

// 2. Before submit, verify customer
const verification = await fetch('/api/deliveries/verify-customer', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
  body: JSON.stringify(deliveryForm)
});

const result = await verification.json();

// 3. Check for changes
if (result.exists && result.hasChanges) {
  // Show popup modal
  const userChoice = await showCustomerChangeModal(result.changes, result.currentInfo);
  
  if (userChoice === 'cancel') {
    return; // Don't create delivery
  }
  
  if (userChoice === 'useExisting') {
    // Auto-fill form with existing data
    deliveryForm.receiverName = result.currentInfo.name;
    deliveryForm.deliveryAddress = result.currentInfo.address;
    deliveryForm.deliveryProvinceId = result.currentInfo.provinceId;
    deliveryForm.deliveryDistrictId = result.currentInfo.districtId;
  }
  
  if (userChoice === 'updateCustomer') {
    // Update customer first
    await fetch(`/api/customers/${result.customerId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      body: JSON.stringify({
        name: deliveryForm.receiverName,
        address: deliveryForm.deliveryAddress,
        provinceId: deliveryForm.deliveryProvinceId,
        districtId: deliveryForm.deliveryDistrictId
      })
    });
  }
}

// 4. Create delivery
await fetch('/api/deliveries', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
  body: JSON.stringify(deliveryForm)
});
```

### Modal Example

```javascript
function showCustomerChangeModal(changes, currentInfo) {
  const modal = `
    <div class="modal">
      <h3>Customer Information Changed</h3>
      <p>The customer information differs from our records:</p>
      
      ${Object.entries(changes).map(([field, change]) => `
        <div>
          <strong>${field}:</strong>
          <div>Current: ${change.oldValue}</div>
          <div>New: ${change.newValue}</div>
        </div>
      `).join('')}
      
      <div class="buttons">
        <button onclick="return 'cancel'">Cancel</button>
        <button onclick="return 'useExisting'">Use Existing Info</button>
        <button onclick="return 'updateCustomer'">Update Customer</button>
      </div>
    </div>
  `;
  
  // Show modal and return user's choice
  return showModal(modal);
}
```

## Database Verification

Check customer records:
```sql
-- View customers by company
SELECT 
  u.id,
  u.display_name,
  u.phone_e164,
  u.default_address,
  c.company_name,
  COUNT(DISTINCT d.id) as total_deliveries
FROM users u
LEFT JOIN companies c ON u.company_id = c.id
LEFT JOIN delivery_items d ON u.id = d.receiver_id
WHERE u.user_type = 'CUSTOMER'
GROUP BY u.id, c.company_name
ORDER BY c.company_name, u.display_name;

-- Check unique constraint works
SELECT phone_e164, company_id, COUNT(*)
FROM users
WHERE user_type = 'CUSTOMER'
GROUP BY phone_e164, company_id
HAVING COUNT(*) > 1;
-- Should return 0 rows
```

## Troubleshooting

### Issue: Verification shows hasChanges = false when info differs

**Cause**: Province/District comparison uses UUIDs, not names

**Solution**: Ensure `deliveryProvinceId` and `deliveryDistrictId` match exactly

### Issue: 403 Forbidden when accessing customer

**Cause**: Customer belongs to different company

**Solution**: Only company that created customer can view/edit it

### Issue: Duplicate phone error

**Cause**: Trying to create second customer with same phone in same company

**Solution**: Use verification endpoint first, update existing customer instead

## Migration Applied

```sql
-- Schema changes applied
ALTER TABLE users ADD COLUMN company_id UUID;
ALTER TABLE users ADD COLUMN default_address TEXT;
ALTER TABLE users ADD COLUMN default_province_id UUID;
ALTER TABLE users ADD COLUMN default_district_id UUID;

-- Unique constraint
CREATE UNIQUE INDEX uk_users_phone_company 
  ON users(phone_e164, company_id) 
  WHERE user_type = 'CUSTOMER' AND company_id IS NOT NULL;
```

## Summary

✅ **Company-scoped customers** - Data isolation per company  
✅ **Verification workflow** - Prevent accidental overwrites  
✅ **No auto-updates** - Explicit user confirmation required  
✅ **Ownership validation** - Security checks on all operations  
✅ **Clean testing** - All test data deleted, ready for fresh tests
