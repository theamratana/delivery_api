# Pricing and Exchange Rate Feature Summary

## Overview
Added comprehensive pricing breakdown and multi-currency support to the delivery system.

## Database Changes

### New Table: `exchange_rates`
Tracks currency exchange rates over time with audit trail.

**Columns:**
- `id` (UUID) - Primary key
- `from_currency` (VARCHAR) - Default "USD"
- `to_currency` (VARCHAR) - Default "KHR"
- `rate` (NUMERIC 10,4) - Default 4000.0000
- `effective_date` (TIMESTAMP WITH TIME ZONE) - When rate becomes active
- `is_active` (BOOLEAN) - Whether rate is currently active
- `notes` (TEXT) - Optional notes about the rate
- `created_at`, `updated_at`, `deleted_at` - Audit fields

**Indexes:**
- `idx_exchange_rates_effective_date` - For date-based queries
- `idx_exchange_rates_currencies` - For currency pair lookups

**Default Data:**
- 1 USD = 4000 KHR (inserted automatically)

### Updated Table: `delivery_items`
Added pricing breakdown fields to track discounts and currency conversion.

**New Columns:**
- `delivery_discount` (NUMERIC 10,2) - Discount on delivery fee (default 0.00)
- `order_discount` (NUMERIC 10,2) - Order-wide discount (default 0.00)
- `sub_total` (NUMERIC 10,2) - item_value + delivery_fee - delivery_discount
- `grand_total` (NUMERIC 10,2) - sub_total - order_discount
- `actual_delivery_cost` (NUMERIC 10,2) - Real delivery cost (even when free)
- `khr_amount` (NUMERIC 15,2) - Grand total converted to KHR
- `exchange_rate_used` (NUMERIC 10,4) - Snapshot of exchange rate at transaction time

## Code Changes

### 1. DeliveryItem Entity (`model/DeliveryItem.java`)
Added 7 new fields with getters/setters:
```java
private BigDecimal deliveryDiscount = BigDecimal.ZERO;
private BigDecimal orderDiscount = BigDecimal.ZERO;
private BigDecimal subTotal;
private BigDecimal grandTotal;
private BigDecimal actualDeliveryCost;
private BigDecimal khrAmount;
private BigDecimal exchangeRateUsed;
```

### 2. ExchangeRate Entity (`model/ExchangeRate.java`)
New entity for tracking exchange rates:
```java
@Entity
@Table(name = "exchange_rates")
public class ExchangeRate extends AuditableEntity {
    private UUID id;
    private String fromCurrency = "USD";
    private String toCurrency = "KHR";
    private BigDecimal rate = new BigDecimal("4000.0000");
    private OffsetDateTime effectiveDate;
    private Boolean isActive = true;
    private String notes;
}
```

### 3. ExchangeRateRepository (`repo/ExchangeRateRepository.java`)
Repository with smart rate lookup:
```java
@Query("SELECT er FROM ExchangeRate er WHERE er.fromCurrency = :fromCurrency " +
       "AND er.toCurrency = :toCurrency AND er.isActive = true " +
       "AND er.effectiveDate <= :asOfDate " +
       "ORDER BY er.effectiveDate DESC LIMIT 1")
Optional<ExchangeRate> findLatestRate(...);

Optional<ExchangeRate> findCurrentRate(String fromCurrency, String toCurrency);
```

### 4. ExchangeRateService (`service/ExchangeRateService.java`)
Service for exchange rate operations:
```java
// Get current rate (defaults to 4000 for USD->KHR)
BigDecimal getExchangeRate(String fromCurrency, String toCurrency);

// Get rate as of specific date
BigDecimal getExchangeRateAsOf(String fromCurrency, String toCurrency, OffsetDateTime asOfDate);

// Convert amount between currencies
BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency);
```

## Pricing Calculation Logic

### Formula:
```
item_total = item_value * quantity
sub_total = item_total + delivery_fee - delivery_discount
grand_total = sub_total - order_discount
khr_amount = grand_total * exchange_rate_used
```

### Key Points:
- `actual_delivery_cost` tracks real delivery cost even when `delivery_discount` makes it free
- `exchange_rate_used` snapshots the rate at transaction time (audit trail)
- Discounts default to 0.00 (no discount)
- USD is primary currency, KHR is for display/record

## Migration File
`migration-pricing-and-exchange-rate.sql`
- Creates `exchange_rates` table with indexes
- Inserts default USD->KHR rate (4000)
- Adds 7 pricing columns to `delivery_items`
- Adds documentation comments

## Usage Examples

### Setting Pricing on Delivery:
```java
DeliveryItem item = new DeliveryItem();
item.setItemValue(new BigDecimal("50.00")); // Product value
item.setDeliveryFee(new BigDecimal("5.00")); // Delivery cost
item.setActualDeliveryCost(new BigDecimal("5.00")); // Real cost
item.setDeliveryDiscount(new BigDecimal("5.00")); // Free delivery promo!
item.setOrderDiscount(new BigDecimal("2.00")); // Order discount

// Calculate totals
BigDecimal subTotal = item.getItemValue()
    .add(item.getDeliveryFee())
    .subtract(item.getDeliveryDiscount());
item.setSubTotal(subTotal); // 50.00 + 5.00 - 5.00 = 50.00

BigDecimal grandTotal = subTotal.subtract(item.getOrderDiscount());
item.setGrandTotal(grandTotal); // 50.00 - 2.00 = 48.00

// Get exchange rate and convert
BigDecimal rate = exchangeRateService.getExchangeRate("USD", "KHR");
item.setExchangeRateUsed(rate); // 4000.0000
item.setKhrAmount(grandTotal.multiply(rate)); // 48 * 4000 = 192,000 KHR
```

### Getting Exchange Rates:
```java
// Current rate
BigDecimal currentRate = exchangeRateService.getExchangeRate("USD", "KHR");

// Historical rate
BigDecimal historicalRate = exchangeRateService.getExchangeRateAsOf(
    "USD", "KHR", OffsetDateTime.parse("2024-01-01T00:00:00Z")
);

// Convert amount
BigDecimal khrAmount = exchangeRateService.convert(
    new BigDecimal("100.00"), "USD", "KHR"
); // Returns 400,000 KHR
```

## Testing Checklist
- [x] Database migration applied successfully
- [x] exchange_rates table created with default rate
- [x] delivery_items table has new pricing columns
- [x] Code compiles without errors
- [ ] Test creating delivery with pricing fields
- [ ] Test exchange rate lookup
- [ ] Test currency conversion
- [ ] Verify free delivery tracking (actual_delivery_cost vs delivery_discount)

## Next Steps
1. Add pricing calculation logic to DeliveryItem save operations
2. Create admin API endpoints to manage exchange rates
3. Add validation for discount amounts (can't exceed totals)
4. Implement automatic calculation of sub_total and grand_total in service layer
5. Add historical exchange rate reporting
