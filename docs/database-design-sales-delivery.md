# Order & Delivery Database Design (POS System)

## Overview
Normalized database schema for POS order management with delivery tracking. Simple, focused on walk-in, pickup, and delivery sales.

---

## Entity Relationship Diagram (ERD)

```
Order (1) ----< (M) OrderItem >---- (1) Product
  |
  |---- (1) Customer
  |
  |---- (0..1) DeliveryDetail ---- (M) PackagePhoto
                  |
                  └---- (1) DeliveryCompany
```

---

## Table Definitions

### 1. **Order** (Main Transaction)
Primary transaction record. **Extends TenantAuditableEntity** (inherits: company_id, created_at, updated_at, created_by, updated_by).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique order ID |
| customer_id | UUID | FK, NOT NULL | Customer who placed order |
| order_number | VARCHAR(50) | UNIQUE, NOT NULL | Human-readable order number (e.g., "ORD-2026-0001") |
| order_type | VARCHAR(20) | NOT NULL | WALK_IN, PICKUP, DELIVERY |
| order_date | TIMESTAMP | NOT NULL | Date/time of order |
| order_subtotal | DECIMAL(12,2) | NOT NULL | Sum of all line_totals (after item discounts) |
| delivery_fee | DECIMAL(12,2) | DEFAULT 0 | Delivery charge (if applicable) |
| delivery_fee_discount | DECIMAL(12,2) | DEFAULT 0 | Discount on delivery fee |
| grand_total | DECIMAL(12,2) | NOT NULL | Final amount to pay |
| payment_type | VARCHAR(20) | NOT NULL | PAID, COD, PAID_DELIVERY_COD, PARTIAL |
| payment_status | VARCHAR(20) | NOT NULL | PENDING, COMPLETED, PARTIAL, REFUNDED |
| order_status | VARCHAR(20) | NOT NULL | PENDING, CONFIRMED, PROCESSING, READY, COMPLETED, CANCELLED |
| notes | TEXT | | Additional order notes |

**Indexes:**
- `idx_order_company_date` (company_id, order_date) - inherited company_id
- `idx_order_customer` (customer_id)
- `idx_order_number` (order_number)
- `idx_order_status` (order_status)
- `idx_order_source` (order_source)

**Constraints:**
```sql
CHECK (order_type IN ('WALK_IN', 'PICKUP', 'DELIVERY'))
CHECK (order_source IN ('POS', 'WEB', 'MOBILE_APP', 'MARKETPLACE'))
CHECK (payment_type IN ('PAID', 'COD', 'PAID_DELIVERY_COD', 'PARTIAL'))

**Constraints:**
```sql
CHECK (order_type IN ('WALK_IN', 'PICKUP', 'DELIVERY'))
CHECK (payment_type IN ('PAID', 'COD', 'PAID_DELIVERY_COD', 'PARTIAL'))
CHECK (payment_status IN ('PENDING', 'COMPLETED', 'PARTIAL', 'REFUNDED'))
CHECK (order_status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'READY', 'COMPLETED', 'CANCELLED'))
CHECK (grand_total >= 0)
CHECK (delivery_fee_discount <= delivery_fee)
```

**Calculation Formula:**
```
grand_total = order_subtotal + delivery_fee - delivery_fee
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique line item ID |
| order_id | UUID | FK, NOT NULL | Parent order |
| product_id | UUID | FK, NOT NULL | Product being sold |
| product_name | VARCHAR(255) | NOT NULL | Product name (snapshot) |
| product_sku | VARCHAR(100) | | Product SKU code (snapshot) - e.g., "SHIRT-RED-L" |
| product_variant_id | UUID | FK | Product variant (size, color, etc.) - optional |
| variant_attributes | JSONB | | Snapshot of variant attribut|
| quantity | DECIMAL(10,2) | NOT NULL | Quantity ordered |
| unit_price | DECIMAL(12,2) | NOT NULL | Selling price per unit |
| discount_type | VARCHAR(20) | | PERCENTAGE, AMOUNT (NULL = no discount) |
| discount_value | DECIMAL(12,2) | DEFAULT 0 | Discount amount or percentage |
| sub_total | DECIMAL(12,2) | NOT NULL | quantity × unit_price (BEFORE discount) |
| line_discount | DECIMAL(12,2) | DEFAULT 0 | Calculated discount amount |
| line_total | DECIMAL(12,2) | NOT NULL | sub_total - line_discount (AFTER discount) |
| notes | TEXT | | Item-specific notes |

**Indexes:**
- `idx_order_item_order` (order_id)
- `idx_order_item_product` (product_id)

**SKU (Stock Keeping Unit):**
- Unique inventory code for tracking (e.g., "PHONE-128GB-BLK", "SHIRT-RED-L")
```sql
CHECK (discount_type IN ('PERCENTAGE', 'AMOUNT') OR discount_type IS NULL)
CHECK (quantity > 0)
CHECK (unit_price >= 0)
CHECK (discount_value >= 0)
-- If PERCENTAGE, discount_value should be 0-100
CHECK (discount_type != 'PERCENTAGE' OR (discount_value >= 0 AND discount_value <= 100))
CHECK (line_total = sub_total - line_discount)
```

**Calculation Logic (Accounting Standard):**
1. `sub_total = quantity × unit_price` (extended price BEFORE discount)
2. Calculate discount:
   - If `discount_type = 'PERCENTAGE'`: `line_discount = sub_total × (discount_value / 100)`
   - If `discount_type = 'AMOUNT'`: `line_discount = discount_value`
   - If `discount_type = 'NONE'`: `line_discount = 0`
3. `line_total = sub_total - line_discount` (net price AFTER discount)

**Example:**
- Quantity: 3, Unit Price: $10.00
- sub_total = 3 × $10.00 = **$30.00**
- discount_type = PERCENTAGE, discount_value = 10
- line_discount = $30.00 × 0.10 = **$3.00**
- line_total = $30.00 - $3.00 = **$27.00** ✅

---

### 3. **Customer**
Customer master data with contact and location information.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique customer ID |
| company_id | UUID | FK, NOT NULL | Tenant/company reference |
| customer_code | VARCHAR(50) | UNIQUE | Auto-generated code (e.g., "CUS-0001") |
| name | VARCHAR(255) | NOT NULL | Customer name |
| phone | VARCHAR(20) | | Primary phone number |
| phone2 | VARCHAR(20) | | Secondary phone number |
| email | VARCHAR(255) | | Email address |
| address | TEXT | | Street address |
| province_id | UUID | FK | Province reference |
| district_id | UUID | FK | District reference |
| commune_id | UUID | FK | Commune reference (if applicable) |
| village_id | UUID | FK | Village reference (if applicable) |
| customer_type | VARCHAR(20) | DEFAULT 'REGULAR' | REGULAR, WALK_IN, VIP |
| is_active | BOOLEAN | DEFAULT TRUE | |
| notes | TEXT | | Additional notes |
| created_at | TIMESTAMP | NOT NULL | |
| updated_at | TIMESTAMP | NOT NULL | |orders). **Extends AuditableEntity** (created_at, updated_at).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique delivery ID |
| order_id | UUID | FK, UNIQUE, NOT NULL | One-to-one with Order |
| delivery_company_id | UUID | FK | Delivery service provider |
| delivery_fee | DECIMAL(12,2) | NOT NULL | Original delivery fee |
| fee_discount_amount | DECIMAL(12,2) | DEFAULT 0 | Discount on delivery fee |
| final_delivery_fee | DECIMAL(12,2) | NOT NULL | delivery_fee - fee_discount_amount |
| is_free_delivery | BOOLEAN | COMPUTED | final_delivery_fee = 0 |
| qr_code | VARCHAR(100) | UNIQUE | Internal QR code for quick scanning |
| tracking_number | VARCHAR(100) | | External delivery company tracking # |
| pickup_date | TIMESTAMP | | When picked up from warehouse/store |
| estimated_delivery_date | DATE | | Expected delivery date |
| actual_delivery_date | TIMESTAMP | | Actual delivery completion |
| delivery_status | VARCHAR(20) | NOT NULL | PENDING, PICKED_UP, IN_TRANSIT, DELIVERED, FAILED, RETURNED |
| delivery_notes | TEXT | | Delivery instructions |
| recipient_name | VARCHAR(255) | | Recipient name (if different from customer) |
| recipient_phone | VARCHAR(20) | | Recipient phone |
| delivery_address | TEXT | NOT NULL | Full delivery address |
| latitude | DECIMAL(10,8) | | GPS latitude for delivery location |
| longitude | DECIMAL(11,8) | | GPS longitude for delivery location |

**Indexes:**
- `idx_delivery_order` (order_id)
- `idx_delivery_company` (delivery_company_id)
- `idx_delivery_status` (delivery_status)
- `idx_delivery_qr` (qr_codeivery tracking number |
| pickup_date | TIMESTAMP | | When picked up from seller |
| estimated_delivery_date | DATE | | Expected delivery date |
| actual_delivery_date | TIMESTAMP | | Actual delivery completion |
| delivery_status | VARCHAR(20) | NOT NULL | PENDING, PICKED_UP, IN_TRANSIT, DELIVERED, FAILED, RETURNED |
| delivery_notes | TEXT | | Delivery instructions |
| recipient_name | VARCHAR(255) | | Recipient name (if different from customer) |
| recipient_phone | VARCHAR(20) | | Recipient phone |
| delivery_address | TEXT | NOT NULL | Full delivery address |
| created_at | TIMESTAMP | NOT NULL | |
| updated_at | TIMESTAMP | NOT NULL | |

**Indexes:**
- `idx_delivery_sale` (sale_id)
- `idx_delivery_company` (delivery_company_id)
- `idx_delivery_status` (delivery_status)
- `idx_delivery_tracking` (tracking_number)

**Constraints:**
```sql
CHECK (delivery_status IN ('PENDING', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'FAILED', 'RETURNED'))
CHECK (delivery_fee >= 0)
CHECK (fee_discount_amount >= 0)
CHECK (final_delivery_fee >= 0)
CHECK (fee_discount_amount <= delivery_fee)
```
QR Code vs Tracking Number:**
- **qr_code**: YOUR internal QR for warehouse/driver scanning (generated by your system)
  - Example: `ORD202600001-DEL` encoded as QR
  - Fast pickup/delivery verification
  - Works offline with mobile scanner
  
- **tracking_number**: External delivery company's tracking number (J&T, Kerry Express, etc.)
  - Example: `JT123456789KH`
  - Customers can track on delivery company website
  - Integration with 3rd party delivery APIs

**Free Delivery Tracking:**
- `is_free_delivery = (final_delivery_fee = 0)`
- Report total promotional delivery costs: 
  ```sql
  SUM(delivery_fee - final_delivery_fee) WHERE final_delivery_fee = 0
  ``
- Can generate reports on free delivery costs: `SUM(delivery_fee - final_delivery_fee) WHERE final_delivery_fee = 0`

---

### 5. **DeliveryPhoto**
Package photos for deliveries.

| Column | Type | Constraints | Description |
|--------|------|---------- **Extends TenantAuditableEntity**.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique delivery company ID |
| name | VARCHAR(255) | NOT NULL | Delivery company name (J&T, Kerry, etc.) |
| code | VARCHAR(50) | UNIQUE | Short code (JT, KERRY, NINJAVAN) |
| phone | VARCHAR(20) | | Contact phone |
| email | VARCHAR(255) | | Contact email |
| api_endpoint | VARCHAR(500) | | API URL for tracking integration |
| api_key | VARCHAR(255) | | API key (encrypted) |
| suggested_base_fee | DECIMAL(12,2) | | Suggested starting fee (reference only) |
| pricing_type | VARCHAR(20) | | FLAT, WEIGHT_BASED, DISTANCE_BASED, CUSTOM |
| is_active | BOOLEAN | DEFAULT TRUE | |
| notes | TEXT | | Additional notes |

**Indexes:**
- `idx_delivery_company_tenant` (company_id) - inherited
- `idx_delivery_company_code` (code)

**Note on suggested_base_fee:**
- Used as UI helper/reference only
- Actual fees come from `delivery_pricing` table
- Helps sellers estimate costs quickly
- Optional - can be NULL if using dynamic pricing only
|--------|------|-------------|-------------|
| id | UUID | PK | Unique company ID |
| company_id | UUID | FK, NOT NULL | Tenant/company reference |
| name | VARCHAR(255) | NOT NULL | Delivery company name |
| phone | VARCHAR(20) | | Contact phone |
| email | VARCHAR(255) | | Contact email |
| default_fee | DECIMAL(12,2) | | Default delivery fee |
| is_active | BOOLEAN | DEFAULT TRUE | |
| notes | TEXT | | Additional notes |
| created_at | TIMESTAMP | NOT NULL | |
| updated_at | TIMESTAMP | NOT NULL | |

**Indexes:**
- `idx_delivery_company_tenant` (company_id)

---

### 8. **OrderAddress** (Ecommerce Ready)
Separate shipping and billing addresses for orders. **Extends AuditableEntity**.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique address ID |
| order_id | UUID | FK, NOT NULL | Parent order |
| address_type | VARCHAR(20) | NOT NULL | SHIPPING, BILLING |
| recipient_name | VARCHAR(255) | NOT NULL | Recipient name |
| phone | VARCHAR(20) | NOT NULL | Contact phone |
| email | VARCHAR(255) | | Contact email |
| address_line1 | VARCHAR(255) | NOT NULL | Street address line 1 |
| address_line2 | VARCHAR(255) | | Street address line 2 |
| province_id | UUID | FK | Province reference |
| district_id | UUID | FK | District reference |
| commune_id | UUID | FK | Commune reference |
| village_id | UUID | FK | Village reference |
| postal_code | VARCHAR(20) | | Postal/ZIP code |
| country | VARCHAR(2) | DEFAULT 'KH' | ISO country code |
| latitude | DECIMAL(10,8) | | GPS latitude |
| longitude | DECIMAL(11,8) | | GPS longitude |

**Indexes:**
- `idx_order_address_order_type` (order_id, address_type) - Unique together
- `idx_order_address_order` (order_id)

**Constraints:**
```sql
CHECK (address_type IN ('SHIPPING', 'BILLING'))
UNIQUE (order_id, address_type)  -- Only one shipping and one billing per order
```

**How to Get Addresses:**

**Option 1: Query by Type**
```sql
-- Get shipping address
SELECT * FROM order_address 
WHERE order_id = :orderId AND address_type = 'SHIPPING';

-- Get billing address
SELECT * FROM order_address 
WHERE order_id = :orderId AND address_type = 'BILLING';

-- Get both addresses at once
SELECT * FROM order_address 
WHERE order_id = :orderId;
```

**Option 2: JPA Entity Mapping**
```java
@Entity
public class Order {
    @OneToMany(mappedBy = "order")
    private List<OrderAddress> addresses;
    
    // Helper methods
    public OrderAddress getShippingAddress() {
        return addresses.stream()
            .filter(a -> a.getAddressType() == AddressType.SHIPPING)
            .findFirst()
            .orElse(null);
    }
    
    public OrderAddress getBillingAddress() {
        return addresses.stream()
            .filter(a -> a.getAddressType() == AddressType.BILLING)
            .findFirst()
            .orElse(null);
    }
}
```

**Why This Design?**
- ✅ Flexible - can extend to multiple addresses per type later
- ✅ Clean - addresses naturally belong to order
- ✅ Historical accuracy - addresses preserved with order
- ✅ No NULL foreign keys in Order table
- ✅ Easy to query all addresses for an order

---

### 9. **OrderStatusHistory** (NEW - Audit Trail)
Track all status changes for orders. **Extends AuditableEntity**.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique history record ID |
| order_id | UUID | FK, NOT NULL | Parent order |
| from_status | VARCHAR(20) | | Previous status (NULL for initial) |
| to_status | VARCHAR(20) | NOT NULL | New status |
| changed_by | UUID | FK | User who made the change |
| changed_at | TIMESTAMP | NOT NULL | When status changed |
| notes | TEXT | | Reason for status change |

**Indexes:**
- `idx_order_history_order` (order_id, changed_at)

**Benefits:**
- Full audit trail of order lifecycle
- Track who changed what and when
- Useful for analytics (time in each status)
- Dispute resolution

---

## Status Flow Diagrams

### Order Status Flow
```
PENDING → CONFIRMED → PROCESSING → READY → COMPLETED
    ↓          ↓            ↓          ↓
    └──────────┴────────────┴──────────┴──→ CANCELLED
```

**Status Definitions:**
- **PENDING**: Order received, awaiting confirmation
- **CONFIRMED**: Order confirmed, awaiting processing
- **PROCESSING**: Order being prepared/packed
- **READY**: Order ready for pickup/delivery
- **COMPLETED**: Order fulfilled
- **CANCELLED**: Order cancelled

### Delivery Status Flow
```
PENDING → PICKED_UP → IN_TRANSIT → DELIVERED
    ↓          ↓            ↓
    └──────────┴────────────┴──→ FAILED → RETURNED
```

---

## Key Business Rules

### 1. **Order Type Logic**
- **WALK_IN**: Immediate sale at physical store, customer present
- **PICKUP**: Customer orders now, collects later (store/warehouse)
- **DELIVERY**: Ship to customer address via delivery company

### 2. **Customer Handling**
- **Walk-In**: Use default "Walk-In Customer" or quick create with minimal info
- **Pickup/Delivery**: Full customer info required for contact/tracking
- **Ecommerce**: Customer must register/login first

### 3. **Discount Calculations (Accounting Standard)**
Per Item:
```
sub_total = quantity × unit_price (before discount)
line_discount = calculated discount amount
line_total = sub_total - line_discount (after discount)
```

Order Level:
```, translate in application:
```java
// Database Storage (English enums)
payment_type = "PAID" | "COD" | "PAID_DELIVERY_COD" | "PARTIAL"
order_type = "WALK_IN" | "PICKUP" | "DELIVERY"

// Application i18n Layer
{
  "en": {
    "PAID": "Paid",
    "COD": "Cash on Delivery",
    "PAID_DELIVERY_COD": "Paid, Delivery Fee COD",
    "PARTIAL": "Partial Payment",
    "WALK_IN": "Walk-In",
    "PICKUP": "Pickup",
    "DELIVERY": "Delivery"
  },
  "km": {
    "PAID": "បង់រួច",
    "COD": "សងប្រាក់ពេលទទួល",
    "PAID_DELIVERY_COD": "បង់ប្រាក់ផលិតផល តម្លៃដឹកជញ្ជូនសងពេលទទួល",
    "PARTIAL": "បង់មួយផ្នែក",
    "WALK_IN": "អតិថិជនមកដោយផ្ទាល់",
    "PICKUP": "មករបស់ដោយខ្លួនឯង",
    "DELIVERY": "ផ្ញើរហូត"
  }
}
```

**Benefits:**
- Database remains language-neutral
- Easy to add new languages without DB changes
- Consistent enum values for code logic
- Frontend handles display translationen": {
    "PAID": "Paid",
    "COD": "Cash on Delivery",
    "WALK_IN": "Walk-In"
  },Promotional Cost
```sql
-- How much are we absorbing in free delivery?
SELECT 
    DATE_TRUNC('month', o.order_date) as month,
    COUNT(*) as free_delivery_count,
    SUM(d.delivery_fee - d.final_delivery_fee) as total_promotional_cost
FROM "order" o
JOIN delivery_detail d ON o.id = d.order_id
WHERE d.final_delivery_fee = 0
  AND d.delivery_fee > 0
  AND o.company_id = :companyId
GROUP BY month
ORDER BY month DESC;
```

### 2. Complete Order Details with Items and Delivery
```sql
SELECT 
    o.*,
    c.name as customer_name,
    c.phone as customer_phone,
    json_agg(json_build_object(
        'product_name', oi.product_name,
        'sku', oi.product_sku,
        'quantity', oi.quantity,
        'unit_price', oi.unit_price,
        'sub_total', oi.sub_total,
        'discount', oi.line_discount,
        'line_total', oi.line_total
    )) as items,
    dc.name as delivery_company,
    d.qr_code,
    d.tracking_number,
    d.delivery_status,
    d.final_delivery_fee
FROM "order" o
JOIN customer c ON o.customer_id = c.id
JOIN order_item oi ON o.id = oi.order_id
LEFT JOIN delivery_detail d ON o.id = d.order_id
LEFT JOIN delivery_company dc ON d.delivery_company_id = dc.id
WHERE o.id = :orderId
GROUP BY o.id, c.id, d.id, dc.id;
```

### 3. Sales Report by Order Source & Type
```sql
SELECT 
   Ecommerce Evolution Roadmap

### Phase 1: Current (POS + Basic Delivery)
✅ Order management with multiple items
✅ Customer tracking
✅ Delivery with QR code tracking
✅ Discount management
✅ Multi-language support

### Phase 2: Enhanced Features (Q2 2026)
- **PaymentTransaction** table: Multiple payment methods per order
- **Coupon** table: Promotional codes with rules
- **ProductVariant**: Size, color, options
- **Inventory** integration: Real-time stock checking
- **OrderTag**: Custom tags for filtering/organization

### Phase 3: Full Ecommerce (Q3-Q4 2026)
- **ShippingMethod**: Standard, express, same-day options
- **Refund**: Return/refund management
- **Review**: Customer reviews and ratings
- **Wishlist**: Save for later functionality
- **Cart**: Shopping cart management
- **Subscription**: Recurring orders

### Phase 4: Marketplace (2027)
- **Vendor**: Multi-vendor support
- **Commission**: Vendor payout management
- **Rating**: Vendor ratings
- **Dispute**: Order dispute resolution

---

## Normalization Verification

✅ **1NF**: All attributes are atomic
✅ **2NF**: No partial dependencies (all non-key attributes fully dependent on PK)
✅ **3NF**: No transitive dependencies (no non-key attributes depend on other non-key attributes)

**Denormalized Fields (Justified):**
- `product_name`, `product_sku` in OrderItem: Historical snapshot (product may change/be deleted)
- Calculated fields in Order: Performance optimization (pre-computed for queries)
- Geographic data in OrderAddress: Snapshot of delivery location

**Extends TenantAuditableEntity:**
- Order, DeliveryCompany: Full multi-tenant audit trail
- OrderItem: Part of order, uses order's tenant context
- DeliveryDetail, OrderAddress, OrderStatusHistory: Audit metadata only

---

## Implementation Order

### Step 1: Core Tables
1. ✅ Customer (already exists)
2. ✅ Company (already exists)
3. **Order** (main transaction)
4. **OrderItem** (line items)

### Step 2: Delivery Module
5. **DeliveryCompany** (providers)
6. **DeliveryDetail** (delivery info)
7. **DeliveryPhoto** (package photos)

### Step 3: Ecommerce Enhancements
8. **OrderAddress** (shipping/billing)
9. **OrderStatusHistory** (audit trail)

### Migration Steps:
1. Create entity classes extending proper base classes
2. Run Liquibase/Flyway migrations
3. Seed default "Walk-In Customer" per company
4. Create indexes
5. Add foreign key constraints
6. Test with sample data

---

## Design Decisions Summary

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Table Naming | Order (not Sale) | Ecommerce-standard, scalable |
| Base Class | TenantAuditableEntity | Multi-tenant with audit trail |
| Line Amounts | sub_total → line_total | Accounting standard (before → after discount) |
| Delivery Company | FK only (no snapshot) | Companies rarely rename |
| QR + Tracking | Both fields | Internal QR + external tracking |
| Address Storage | Separate table | Multiple addresses per order |
| Status History | Separate table | Full audit trail |
| Enums | English in DB | I18n in application layer |
| Currency | Per order | Multi-currency support |
    o.order_number,
    o.order_date,
    osh.from_status,
    osh.to_status,
    osh.changed_at,
    EXTRACT(EPOCH FROM (osh.changed_at - LAG(osh.changed_at) OVER (PARTITION BY o.id ORDER BY osh.changed_at))) / 3600 as hours_in_status
FROM "order" o
JOIN order_status_history osh ON o.id = osh.order_id
WHERE o.id = :orderId
ORDER BY osh.changed_at;
```

### 5. QR Code Delivery Tracking
```sql
-- Scan QR code, get delivery info
SELECT 
    o.order_number,
    o.order_date,
    c.name as customer_name,
    c.phone as customer_phone,
    d.delivery_status,
    d.delivery_address,
    d.recipient_name,
    d.recipient_phone,
    oa.address_line1 as shipping_address,
    dc.name as delivery_company,
    d.tracking_number
FROM delivery_detail d
JOIN "order" o ON d.order_id = o.id
JOIN customer c ON o.customer_id = c.id
LEFT JOIN order_address oa ON o.id = oa.order_id AND oa.address_type = 'SHIPPING'
LEFT JOIN delivery_company dc ON d.delivery_company_id = dc.id
WHERE d.qr_code = :scannedQRCodid, d.id;
```

### 3. Sales Report by Type
```sql
SELECT 
    sale_type,
    COUNT(*) as total_sales,
    SUM(final_amount) as total_revenue,
    AVG(final_amount) as avg_sale_value
FROM sale
WHERE company_id = :companyId
  AND sale_date BETWEEN :startDate AND :endDate
GROUP BY sale_type;
```

---

## Normalization Verification

✅ **1NF**: All attributes are atomic
✅ **2NF**: No partial dependencies (all non-key attributes fully dependent on PK)
✅ **3NF**: No transitive dependencies (no non-key attributes depend on other non-key attributes)

**Denormalized Fields (Justified):**
- `product_name` in SaleItem: Historical snapshot (product name may change)
- `delivery_company_name` in DeliveryDetail: Historical snapshot
- Calculated fields in Sale: Performance optimization

---

## Migration Notes

1. Create tables in order: Customer → Sale → SaleItem, DeliveryCompany → DeliveryDetail → DeliveryPhoto
2. Seed default "Walk-In Customer" per company
3. Create indexes after data migration
4. Add foreign key constraints last
