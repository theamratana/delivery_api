# Product Delivery Module - Brainstorm & Design

## Overview
The Product Delivery module enables users to create delivery items through a simple photo-based interface, automatically managing delivery companies, drivers, and receivers while providing end-to-end tracking capabilities.

## Core Use Cases

### 1. **Create Delivery Item**
**Primary Flow:**
- User takes photo or selects from gallery
- Adds recipient information (phone number, optional name)
- Specifies delivery method (company or direct driver)
- System creates delivery item with initial status

**Alternative Flows:**
- Bulk delivery creation
- Scheduled deliveries
- Recurring deliveries

### 2. **Auto-Create Entities**
**Company Creation:**
- When delivery targets a company, system creates unverified company
- Uses phone number + company name for identification
- Company can be claimed later via existing verification process

**Driver Creation:**
- Similar to company creation
- Driver can claim account via phone verification
- Maintains driver-specific attributes (vehicle, availability, etc.)

**Receiver Creation:**
- Receiver account auto-created when delivery is sent
- Receiver can claim via existing phone verification flow
- Enables future deliveries and tracking history

### 3. **Delivery Tracking**
**Status Lifecycle:**
```
CREATED → ASSIGNED → PICKED_UP → IN_TRANSIT → DELIVERED
    ↓         ↓          ↓            ↓           ↓
  PENDING   COMPANY/   DRIVER       RECEIVER    COMPLETED
            DRIVER     PICKUP      LOCATION
```

### 4. **Entity Claiming**
**Claim Process:**
- Unverified entities (companies/drivers/receivers) can be claimed
- Uses existing Telegram phone verification
- Transfers ownership and enables full account features

## Data Model Design

### Core Entities

#### DeliveryItem
```java
{
    UUID id;
    UUID senderId;           // From existing User
    UUID receiverId;         // From existing User (may be unverified)
    UUID deliveryCompanyId;  // From existing Company (may be unverified)
    UUID deliveryDriverId;   // From existing User (may be unverified)

    // Item Details
    String itemDescription;
    List<DeliveryPhoto> itemPhotos;  // Photos from gallery/camera
    BigDecimal itemValue;    // Price/value of the item
    String currency;

    // Location Information (Critical for Analytics)
    String pickupAddress;
    String pickupProvince;   // Province/State for reporting
    String pickupKhanDistrict; // District for detailed analytics
    Double pickupLat, pickupLng;

    String deliveryAddress;
    String deliveryProvince; // Province/State for reporting
    String deliveryKhanDistrict; // District for detailed analytics
    Double deliveryLat, deliveryLng;

    // Status & Tracking
    DeliveryStatus status;   // Auto-detected based on user activity
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    OffsetDateTime estimatedDeliveryTime;

    // Payment Information
    PaymentType paymentType; // PAID_TO_CUSTOMER, PAID_TO_DELIVERYMAN
    BigDecimal deliveryFee;
    BigDecimal totalAmount;  // itemValue + deliveryFee
    PaymentStatus paymentStatus;

    // Metadata
    Map<String, String> metadata;
    String specialInstructions;
}
```

#### DeliveryPhoto
```java
{
    UUID id;
    UUID deliveryItemId;
    String photoUrl;
    String thumbnailUrl;     // Optimized thumbnail
    PhotoSource source;      // GALLERY or CAMERA
    OffsetDateTime uploadedAt;
    Integer sequenceOrder;   // Display order
    String description;      // Optional photo description
}
```

#### PhotoSource Enum
```java
enum PhotoSource {
    GALLERY,    // Selected from device gallery
    CAMERA      // Taken with device camera
}
```

#### PaymentType Enum - COMMERCIAL PAYMENTS
```java
enum PaymentType {
    // Primary Commercial Payment Types
    ALREADY_PAID,              // Receiver already paid sender (online purchase)
    CASH_ON_DELIVERY,          // Receiver pays on delivery (COD)
    INSTALLMENT_PAYMENT,       // Pay in installments over time

    // Advanced Payment Arrangements
    PARTIAL_PREPAID,           // Partial payment upfront, rest on delivery
    DEPOSIT_BASED,             // Deposit + final payment on delivery
    MILESTONE_PAYMENT,         // Payment released at delivery milestones

    // Business-to-Business Arrangements
    NET_TERMS_30,              // Net 30 days payment terms
    NET_TERMS_60,              // Net 60 days payment terms
    LETTER_OF_CREDIT,          // Bank-guaranteed payment
    CONSIGNMENT,               // Pay only after item sells

    // Specialized Commercial Models
    REVENUE_SHARING,           // Percentage of sales revenue
    PERFORMANCE_BASED,         // Payment based on delivery performance
    VOLUME_DISCOUNT,           // Bulk purchase discounts
    LOYALTY_DISCOUNT,          // Repeat customer discounts

    // International Trade
    ADVANCE_PAYMENT,           // Full payment before shipment
    DOCUMENT_AGAINST_PAYMENT,  // Pay against shipping documents
    DOCUMENT_AGAINST_ACCEPTANCE, // Pay against accepted bill of exchange

    // Alternative Arrangements
    BARTER_EXCHANGE,           // Goods exchanged for goods/services
    DIGITAL_ASSET,             // Cryptocurrency or digital tokens
    GIFT_PURCHASE,             // Gift purchases (no payment tracking)
    CHARITY_DONATION           // Charitable donations
}
```

#### PaymentStatus Enum
```java
enum PaymentStatus {
    PENDING,        // Payment not yet processed
    COMPLETED,      // Payment successfully processed
    FAILED,         // Payment failed
    REFUNDED,       // Payment refunded
    CANCELLED       // Payment cancelled
}
```

#### DeliveryStatus Enum
```java
enum DeliveryStatus {
    CREATED,        // Item created, waiting assignment
    ASSIGNED,       // Assigned to company/driver
    PICKED_UP,      // Driver picked up item
    IN_TRANSIT,     // Item in transit to receiver
    DELIVERED,      // Successfully delivered
    CANCELLED,      // Cancelled by sender
    RETURNED,       // Returned to sender
    LOST            // Item lost/damaged
}
```

#### DeliveryTracking
```java
{
    UUID id;
    UUID deliveryItemId;
    DeliveryStatus status;
    String description;      // Human readable status update
    OffsetDateTime timestamp;
    UUID updatedBy;         // User who updated status
    String location;        // Optional location info
    Double latitude, longitude;
}
```

### Extended Existing Entities

#### Company Extensions
- Add `verified` boolean flag
- Add `claimedBy` UUID (user who claimed)
- Add `deliveryServices` list (types of delivery offered)

#### User Extensions
- Add `userType` enum extension for DRIVER role
- Add driver-specific fields when userType == DRIVER:
  - `vehicleType`, `licensePlate`, `availabilityStatus`
  - `rating`, `completedDeliveries`

## Payment Tracking Module Ideas

### Core Payment Concepts - COMMERCIAL TRANSACTIONS

The payment system tracks how **receivers pay senders** for the actual goods/items being delivered, separate from delivery service fees.

#### 1. **Commercial Payment Types**
**ALREADY_PAID**: Receiver has already paid sender (online purchase, advance payment)
```
Sender → Receiver → Delivery Service
   ↓         ↓              ↓
Already   Receives        Transports
Paid      Item           Item Only
```

**CASH_ON_DELIVERY**: Receiver pays sender when item is delivered
```
Sender → Delivery Service → Receiver → Sender
   ↓              ↓              ↓         ↓
Sends        Transports       Receives   Gets Paid
Item         Item            Item       for Item
```

**INSTALLMENT_PAYMENT**: Receiver pays in installments over time
```
Payment Schedule: 30% upfront, 70% on delivery
├── Month 1: $100
├── Month 2: $100
└── Delivery: $400
```

### Payment Module Components

#### PaymentTransaction Entity - COMMERCIAL PAYMENTS
```java
{
    UUID id;
    UUID deliveryItemId;
    UUID sellerId;             // Sender/Seller who should receive payment
    UUID buyerId;              // Receiver/Buyer who makes payment
    PaymentType paymentType;   // How buyer pays seller
    BigDecimal itemAmount;     // Amount buyer owes seller
    BigDecimal amountPaid;     // Amount actually paid
    BigDecimal outstandingAmount; // Remaining amount to be paid
    String currency;
    PaymentMethod paymentMethod;
    PaymentStatus status;
    String transactionReference;
    OffsetDateTime dueDate;    // When payment is due
    OffsetDateTime paidAt;     // When payment was completed
    String paymentTerms;       // e.g., "Net 30", "COD", "Installment"
    Map<String, String> metadata;
}
```

#### PaymentMethod Enum
```java
enum PaymentMethod {
    // Cash & Physical
    CASH,                   // Physical cash payment
    CASH_ON_DELIVERY,       // COD payment method

    // Digital Wallets & Mobile Money
    DIGITAL_WALLET,         // Generic digital wallet
    ABA_PAY,               // Cambodian payment gateway
    WING_MONEY,            // Cambodian mobile money
    TRUE_MONEY,            // Cambodian mobile money
    EMONEY,                // Cambodian mobile money
    ACLEDA_MOBILE,         // Bank mobile app

    // Bank Transfers
    BANK_TRANSFER,          // Bank wire transfer
    INTERNET_BANKING,      // Online banking
    MOBILE_BANKING,        // Bank mobile app

    // Cards
    CREDIT_CARD,           // Credit/debit cards
    VISA,                  // Visa cards
    MASTERCARD,            // Mastercard
    JCB,                   // JCB cards
    UNION_PAY,             // UnionPay

    // Cryptocurrency
    CRYPTOCURRENCY,        // Generic crypto
    BITCOIN,               // BTC payments
    ETHEREUM,              // ETH payments
    USDT,                  // Tether stablecoin
    KHR_CRYPTO,            // Cambodian crypto

    // Business & Corporate
    COMPANY_CREDIT,        // Company credit account
    CORPORATE_CARD,        // Corporate credit cards
    PURCHASE_ORDER,        // PO-based payments

    // Alternative Methods
    GIFT_CARD,             // Prepaid gift cards
    LOYALTY_POINTS,        // Reward points
    STORE_CREDIT,          // Store credit balance
    PROMOTIONAL_CREDIT,    // Marketing credits

    // International
    PAYPAL,                // PayPal payments
    STRIPE,                // Stripe integration
    ADYEN,                 // Adyen payment processor
    PAYMENT_WALL,          // Multi-gateway solution

    // Emerging Methods
    QR_CODE,               // QR code payments
    NFC_TAP,               // NFC/contactless
    BIOMETRIC,             // Biometric authentication
    VOICE_PAYMENT          // Voice-activated payments
}
```

### Payment Processing Strategies

#### 1. **Escrow System for Trust**
- **For PAID_TO_CUSTOMER**: System holds payment until delivery confirmation
- **For PAID_TO_DELIVERYMAN**: System facilitates payment collection at delivery
- **Dispute Resolution**: Funds held in escrow during disputes

#### 2. **Multi-Channel Payment Support**
- **Cash Payments**: Track collection status, driver wallet
- **Digital Payments**: Integration with payment gateways
- **Company Credits**: Internal accounting for business clients
- **Split Payments**: Commission distribution to platform/company/driver

#### 3. **Advanced Payment Types**
- **Subscription Models**: Recurring revenue from regular customers
- **Corporate Solutions**: B2B invoicing and credit terms
- **Value-Added Services**: Insurance, express delivery, special handling
- **Incentive Programs**: Tips, loyalty points, promotional discounts
- **Penalty Systems**: Late delivery compensation and cancellation fees

#### 4. **Automated Payment Triggers**
- **Status-Based**: Auto-trigger payments on status changes
- **Time-Based**: Scheduled payments for subscriptions
- **Event-Based**: Webhook triggers from external systems
- **Location-Based**: GPS-triggered payments for delivery zones

### Advanced Payment Features

#### 1. **Commission & Fee Structure**
```
Total Payment = Item Value + Delivery Fee + Platform Fee
                    ↓              ↓             ↓
              To Receiver    To Driver    To Platform
```

#### 2. **Payment Analytics**
- **Revenue Tracking**: By region, company, driver
- **Payment Success Rates**: Success/failure analytics
- **Cash Flow Management**: Predictable payment schedules

#### 3. **Fraud Prevention**
- **Payment Verification**: Multi-factor verification for high-value items
- **Duplicate Prevention**: Transaction deduplication
- **Anomaly Detection**: Unusual payment patterns

### Integration Points

#### External Payment Gateways
- **ABA Pay**: Cambodian payment gateway
- **Wing Money**: Mobile money integration
- **ACLEDA Bank**: Bank transfer integration
- **TrueMoney**: Mobile wallet popular in Cambodia
- **eMoney**: Government-backed digital currency
- **Crypto**: Bitcoin/Ethereum for international

#### Cambodian Market Considerations
- **Mobile Money Dominance**: Wing, TrueMoney, eMoney most popular
- **Cash Still King**: Physical cash payments common for COD
- **USD Preference**: Many transactions in USD despite KHR official currency
- **Rural Access**: Limited banking infrastructure outside Phnom Penh
- **Trust Issues**: Escrow systems crucial for building trust
- **Remittance Flows**: Cross-border payments important for diaspora

#### Internal System Integration
- **User Wallets**: Driver/company balance management
- **Accounting System**: Financial reporting integration
- **Notification System**: Payment status updates

### Payment Flow Examples - COMMERCIAL TRANSACTIONS

#### ALREADY_PAID Flow (Online Purchase):
```
1. Customer buys item online, pays seller immediately
2. Seller creates delivery with itemValue = 0 (already paid)
3. Delivery service only charges delivery fee
4. Item delivered, no additional payment required
5. Seller receives payment separately from online transaction
```

#### CASH_ON_DELIVERY Flow:
```
1. Seller creates delivery with itemValue = $50
2. Delivery service transports item
3. Driver arrives, receiver pays $50 cash to driver
4. Driver marks payment as received in app
5. System credits $50 to seller's wallet
6. Seller can withdraw funds or use for other deliveries
```

#### INSTALLMENT_PAYMENT Flow:
```
1. Seller creates delivery with itemValue = $500
2. Payment terms: 3 installments (30% upfront, 70% on delivery)
3. Receiver pays $150 upfront via app
4. Item delivered, receiver pays remaining $350
5. System tracks payment schedule and reminders
6. Seller receives funds as installments are paid
```

#### NET_TERMS_30 Flow (B2B):
```
1. Business seller creates delivery with itemValue = $1000
2. Payment terms: Net 30 days
3. Item delivered, invoice generated
4. Receiver has 30 days to pay
5. System sends payment reminders
6. Seller receives payment after 30 days
```

#### REVENUE_SHARING Flow:
```
1. Seller delivers product on consignment
2. Item value not fixed - depends on sales
3. Receiver sells item, reports revenue
4. Seller receives percentage of actual sales
5. System tracks sales data and calculates payments
```

## Delivery Fee Integration with Commercial Payments

### Core Concepts - Separating Service Fees from Item Costs

The system must distinguish between:
- **Commercial Payment**: Receiver pays sender for the item/goods
- **Delivery Fee**: Receiver/sender pays delivery service for transportation

#### 1. **Delivery Fee Payment Models**

**MODEL A: Receiver Pays Delivery Fee (Standard)**
```
Item Cost: $10 → Paid to Sender
Delivery Fee: $2 → Paid to Delivery Service
Total Receiver Pays: $12
```

**MODEL B: Sender Pays Delivery Fee (Free Delivery)**
```
Item Cost: $10 → Paid to Receiver
Delivery Fee: $2 → Paid by Sender to Delivery Service
Total Receiver Pays: $10 (Free Delivery)
```

**MODEL C: Bundled Payment (COD with Delivery Fee)**
```
Combined Payment: $12 → Receiver pays to Delivery Driver
├── Item Cost: $10 → Credited to Sender Wallet
└── Delivery Fee: $2 → Credited to Delivery Service
```

### Delivery Fee Scenarios

#### Scenario 1: Receiver Pays Both (Standard E-commerce)
```
E-commerce Flow:
1. Customer orders item ($10) + delivery ($2) = $12 total
2. Payment processed online (ALREADY_PAID)
3. Delivery service transports item
4. No additional payment needed
5. Analytics: "Paid delivery fee upfront"
```

#### Scenario 2: Sender Subsidizes Delivery (Marketing Strategy)
```
Free Delivery Promotion:
1. Item costs $10, delivery normally $2
2. Sender offers "Free Delivery" promotion
3. Receiver pays only $10
4. Sender pays $2 delivery fee to service
5. Analytics: "Subsidized delivery", "Marketing cost: $2"
```

#### Scenario 3: COD with Delivery Fee Collection
```
Cash on Delivery with Fee:
1. Item costs $10, delivery costs $2
2. Receiver pays $12 cash to delivery driver
3. Driver splits payment in app:
   - $10 credited to sender wallet
   - $2 credited to delivery service
4. Real-time balance updates
```

#### Scenario 4: Partial Subsidy (Discounted Delivery)
```
Discounted Delivery:
1. Normal delivery fee: $2
2. Sender offers 50% discount
3. Receiver pays item $10 + delivery $1 = $11
4. Sender pays remaining $1 to delivery service
5. Analytics: "Partial subsidy", "Effective fee: $1"
```

### Delivery Fee Analytics & Business Intelligence

#### 1. **Sender Behavior Analysis**
- **Free Delivery Frequency**: Which senders offer free delivery most often
- **Subsidy Costs**: Total marketing spend on delivery subsidies
- **ROI Tracking**: Revenue gained vs. delivery costs subsidized
- **Competitive Positioning**: How delivery pricing affects market share

#### 2. **Market Trends**
- **Average Delivery Fees**: By region, category, item value
- **Free Delivery Adoption**: Percentage of deliveries with subsidized fees
- **Price Sensitivity**: How delivery fees affect conversion rates
- **Seasonal Patterns**: Delivery fee variations during holidays

#### 3. **Cost Optimization**
- **Dynamic Pricing**: Adjust delivery fees based on demand/distance
- **Bulk Discounts**: Volume-based fee reductions
- **Route Optimization**: Minimize delivery costs through efficient routing
- **Fuel Surcharges**: Distance/time-based fee adjustments

### Technical Implementation - Delivery Fee Handling

#### Enhanced DeliveryItem Entity
```java
{
    // ... existing fields ...

    // Delivery Fee Structure
    BigDecimal deliveryFee;           // Base delivery fee
    BigDecimal subsidizedAmount;      // Amount sender subsidizes (0 = receiver pays full)
    BigDecimal effectiveDeliveryFee;  // Actual amount receiver pays
    DeliveryFeeModel feeModel;        // STANDARD, SUBSIDIZED, FREE, BUNDLED

    // Fee Payment Tracking
    boolean deliveryFeePaid;          // Has delivery fee been paid?
    OffsetDateTime deliveryFeePaidAt; // When delivery fee was paid
    UUID deliveryFeePaidBy;           // Who paid the delivery fee
    String deliveryFeePaymentMethod;  // How delivery fee was paid

    // Analytics Fields
    String feeCategory;               // "FREE", "DISCOUNTED", "STANDARD", "PREMIUM"
    BigDecimal marketingCost;         // Sender's subsidy cost for analytics
}
```

#### DeliveryFeeModel Enum
```java
enum DeliveryFeeModel {
    STANDARD,       // Receiver pays full delivery fee
    SUBSIDIZED,     // Sender pays portion of delivery fee
    FREE,           // Sender pays full delivery fee
    BUNDLED,        // Delivery fee included in COD payment
    DYNAMIC,        // Fee calculated based on conditions
    SUBSCRIPTION    // Covered by delivery subscription
}
```

#### Database Extensions for Delivery Fees
```sql
-- Add to delivery_items table
ALTER TABLE delivery_items ADD COLUMN delivery_fee_model VARCHAR(20);
ALTER TABLE delivery_items ADD COLUMN subsidized_amount DECIMAL(10,2) DEFAULT 0;
ALTER TABLE delivery_items ADD COLUMN effective_delivery_fee DECIMAL(10,2);
ALTER TABLE delivery_items ADD COLUMN delivery_fee_paid BOOLEAN DEFAULT FALSE;
ALTER TABLE delivery_items ADD COLUMN delivery_fee_paid_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE delivery_items ADD COLUMN delivery_fee_paid_by UUID REFERENCES users(id);
ALTER TABLE delivery_items ADD COLUMN fee_category VARCHAR(20);
ALTER TABLE delivery_items ADD COLUMN marketing_cost DECIMAL(10,2) DEFAULT 0;

-- Delivery fee transactions (separate from commercial payments)
CREATE TABLE delivery_fee_transactions (
    id UUID PRIMARY KEY,
    delivery_item_id UUID REFERENCES delivery_items(id),
    amount DECIMAL(10,2) NOT NULL,
    paid_by UUID REFERENCES users(id),     -- Who paid (sender or receiver)
    paid_to UUID REFERENCES users(id),     -- Who received (driver/company/platform)
    payment_method VARCHAR(20),
    transaction_reference VARCHAR(255),
    paid_at TIMESTAMP WITH TIME ZONE,
    fee_type VARCHAR(20)                   -- "BASE_FEE", "FUEL_SURCHARGE", "INSURANCE", etc.
);
```

#### API Extensions for Delivery Fees
```json
// Enhanced delivery creation
POST /api/deliveries
{
  "itemValue": 10.00,
  "deliveryFeeModel": "SUBSIDIZED",
  "subsidizedAmount": 2.00,        // Sender pays $2, receiver pays $0
  "effectiveDeliveryFee": 0.00,    // What receiver actually pays
  "feeCategory": "FREE_DELIVERY"
}

// Delivery fee payment
POST /api/deliveries/{id}/pay-delivery-fee
{
  "paymentMethod": "ABA_PAY",
  "amount": 2.00
}
```

### Business Rules for Delivery Fee Management

#### 1. **Fee Calculation Logic**
```
Base Delivery Fee = f(distance, weight, urgency, location)
Effective Fee = Base Fee - Subsidized Amount
Total Receiver Pays = Item Value + Effective Fee
Sender Cost = Delivery Fee Paid to Service + Subsidized Amount
```

#### 2. **Payment Flow Logic**
- **Standard**: Receiver pays delivery fee upfront or on delivery
- **Subsidized**: System automatically charges sender for subsidized portion
- **Free**: Sender charged full delivery fee, receiver pays $0
- **Bundled**: COD payment split between item cost and delivery fee

#### 3. **Analytics Triggers**
- Track every delivery fee payment with full attribution
- Calculate sender subsidy costs for marketing ROI
- Monitor delivery fee collection rates by region/method
- Identify optimal pricing strategies through A/B testing

## Technical Implementation

#### Database Schema - COMMERCIAL PAYMENTS
```sql
-- Commercial payment transactions
CREATE TABLE commercial_payments (
    id UUID PRIMARY KEY,
    delivery_item_id UUID REFERENCES delivery_items(id),
    seller_id UUID REFERENCES users(id),  -- Who should receive payment
    buyer_id UUID REFERENCES users(id),   -- Who makes payment
    payment_type VARCHAR(30) NOT NULL,
    item_amount DECIMAL(10,2) NOT NULL,   -- Total amount buyer owes
    amount_paid DECIMAL(10,2) DEFAULT 0,  -- Amount paid so far
    outstanding_amount DECIMAL(10,2),     -- Remaining amount
    currency VARCHAR(3) DEFAULT 'USD',
    payment_method VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    transaction_reference VARCHAR(255),
    due_date TIMESTAMP WITH TIME ZONE,    -- When payment is due
    paid_at TIMESTAMP WITH TIME ZONE,     -- When fully paid
    payment_terms VARCHAR(100),           -- e.g., "Net 30", "COD"
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES users(id),
    is_deleted BOOLEAN DEFAULT FALSE
);

-- Seller wallets for COD payments
CREATE TABLE seller_wallets (
    id UUID PRIMARY KEY,
    seller_id UUID REFERENCES users(id) UNIQUE,
    balance DECIMAL(10,2) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',
    last_updated TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES users(id),
    is_deleted BOOLEAN DEFAULT FALSE
);

-- Payment installments for installment plans
CREATE TABLE payment_installments (
    id UUID PRIMARY KEY,
    commercial_payment_id UUID REFERENCES commercial_payments(id),
    installment_number INTEGER NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    due_date TIMESTAMP WITH TIME ZONE,
    paid_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES users(id),
    is_deleted BOOLEAN DEFAULT FALSE
);
```

#### API Endpoints - COMMERCIAL PAYMENTS
- `POST /api/payments/commercial/initiate` - Start commercial payment process
- `POST /api/payments/commercial/confirm` - Confirm payment received (COD)
- `GET /api/payments/commercial/{id}` - Get payment details
- `POST /api/payments/commercial/installment` - Record installment payment
- `GET /api/payments/commercial/outstanding` - Get outstanding payments
- `POST /api/payments/commercial/dispute` - File payment dispute
- `GET /api/wallets/seller/balance` - Check seller wallet balance

#### Delivery Fee API Endpoints
- `POST /deliveries/{id}/calculate-fee` - Calculate delivery fee
- `POST /deliveries/{id}/pay-delivery-fee` - Pay delivery fee
- `GET /deliveries/{id}/fee-status` - Check delivery fee payment status
- `POST /deliveries/{id}/subsidize-fee` - Sender subsidizes delivery fee
- `GET /analytics/delivery-fees` - Delivery fee analytics

### Challenges & Solutions - COMMERCIAL PAYMENTS

#### 1. **COD Payment Collection**
**Challenge**: Ensuring drivers collect and report COD payments accurately
**Solution**:
- Digital payment confirmation with photo verification
- GPS tracking of payment collection location
- Real-time balance updates in driver wallet
- Dispute resolution for payment disagreements

#### 2. **Installment Payment Tracking**
**Challenge**: Managing complex payment schedules over time
**Solution**:
- Automated payment reminders and notifications
- Payment plan templates (3-month, 6-month, etc.)
- Late payment penalties and grace periods
- Integration with accounting systems

#### 3. **B2B Payment Terms**
**Challenge**: Managing credit terms and overdue accounts
**Solution**:
- Credit limit management per buyer
- Automated dunning processes
- Integration with business accounting software
- Legal recourse for non-payment

#### 4. **Cross-border Payments**
**Challenge**: Currency conversion and international transfers
**Solution**:
- Multi-currency wallet support
- Real-time exchange rate integration
- Compliance with international payment regulations
- Partnership with international payment processors

#### 5. **Fraud Prevention in COD**
**Challenge**: Fake deliveries or payment disputes
**Solution**:
- Delivery confirmation with recipient photo
- GPS-verified delivery locations
- Digital signatures for payment confirmation
- Insurance coverage for disputed deliveries

## Sender Delivery Creation Process

### Step-by-Step Workflow

#### Step 1: Item Information Setup
**Sender Actions:**
- Take photos or select from gallery (up to 5 photos)
- Enter item description and category
- Set item value/price (what receiver owes sender)
- Specify item dimensions and weight (affects delivery fee)

**System Validation:**
- Photo quality check and optimization
- Item category validation for fee calculation
- Price reasonableness check

#### Step 2: Delivery Fee Configuration (Cambodian Flexibility)
**Flexible Fee Options:**
- **System Suggested**: Based on distance, weight, urgency
- **Custom Amount**: Sender sets their own fee
- **Free Delivery**: Sender subsidizes full fee (marketing)
- **Discounted Fee**: Partial subsidy (e.g., 50% off)

**Cambodian Context:**
- Rural vs urban pricing differences
- Fuel cost variations
- Competition-based pricing
- Seasonal demand adjustments

#### Step 3: Pricing Calculation
**Formula:**
```
Item Price (set by sender) = $10.00
Delivery Fee (flexible) = $2.00 (or $0 for free delivery)
Total Amount Receiver Pays = $12.00
```

**Breakdown Display:**
- Item Cost: $10.00 (goes to sender)
- Delivery Fee: $2.00 (goes to delivery service)
- **Total: $12.00** (receiver payment amount)

#### Step 4: Receiver Information
**Contact Details:**
- Phone number (required)
- Name (optional)
- Address details with province/khan-district

**Auto-Entity Creation:**
- System creates unverified receiver if phone not found
- Receiver can claim account later via verification

#### Step 5: Delivery Method Selection
**Options:**
- **Company Delivery**: Select by name/phone (auto-creates if needed)
- **Direct Driver**: Choose specific driver by phone
- **Auto-Assignment**: Let system choose best option

#### Step 6: Payment Type Configuration
**Commercial Payment Setup:**
- **Already Paid**: Item price = $0 (receiver paid sender separately)
- **Cash on Delivery**: Receiver pays full amount to driver
- **Installment**: Set up payment schedule

#### Step 7: Review and Confirmation
**Final Summary:**
```
📦 Item: Electronics Package
💰 Item Value: $10.00
🚚 Delivery Fee: $2.00 (you pay $0, receiver pays $2.00)
💵 Total Receiver Pays: $12.00
📍 Delivery To: John Doe (+855 12 345 678)
🏢 Via: FastDelivery Inc
⏰ Estimated: 2-3 hours
```

**Sender Cost Analysis:**
- If standard: Pay $2.00 to delivery service
- If free delivery: Pay $2.00 (marketing cost)
- If COD: Pay $2.00, receive $10.00 from driver

### Cambodian Market Considerations

#### Delivery Fee Flexibility
**Factors Affecting Fees:**
- **Distance**: Phnom Penh vs provinces
- **Urgency**: Same-day vs next-day
- **Item Type**: Documents vs heavy packages
- **Competition**: Local market rates
- **Fuel Costs**: Current gas prices

**Common Fee Ranges:**
- **Within City**: $1-3
- **Inter-Province**: $3-8
- **Express Service**: $5-15
- **Heavy Items**: +$2-5 surcharge

#### Business Strategies
**Free Delivery Tactics:**
- High-value items ($50+)
- First-time customers
- Bulk orders
- Marketing promotions

**Premium Pricing:**
- Time-sensitive deliveries
- Fragile items
- Remote locations
- Peak hours

### Technical Implementation - Sender Interface

#### API Request Structure
```json
POST /deliveries
{
  "item": {
    "description": "iPhone 15 Pro",
    "category": "ELECTRONICS",
    "value": 10.00,
    "photos": ["photo1.jpg", "photo2.jpg"],
    "weight": 0.5,
    "dimensions": "15x8x2cm"
  },
  "pricing": {
    "itemPrice": 10.00,
    "deliveryFeeModel": "STANDARD",
    "customDeliveryFee": 2.00,
    "subsidizedAmount": 0.00,
    "totalReceiverPays": 12.00
  },
  "receiver": {
    "phone": "+85512345678",
    "name": "John Doe",
    "address": "123 Street, Phnom Penh",
    "province": "Phnom Penh",
    "district": "Chamkarmon"
  },
  "pickup": {
    "address": "456 Sender St, Phnom Penh",
    "province": "Phnom Penh",
    "district": "Daun Penh"
  },
  "delivery": {
    "method": "COMPANY",
    "companyName": "FastDelivery Inc",
    "companyPhone": "+85598765432",
    "priority": "STANDARD",
    "specialInstructions": "Handle with care"
  },
  "payment": {
    "commercialType": "CASH_ON_DELIVERY",
    "paymentTerms": "COD"
  }
}
```

#### Response Structure
```json
{
  "deliveryId": "uuid-123",
  "status": "CREATED",
  "pricing": {
    "itemPrice": 10.00,
    "deliveryFee": 2.00,
    "totalAmount": 12.00,
    "senderCost": 2.00
  },
  "estimatedDelivery": "2024-01-01T14:00:00Z",
  "trackingCode": "DEL123456"
}
```

### Business Logic - Pricing Engine

#### Fee Calculation Rules
```java
public BigDecimal calculateDeliveryFee(DeliveryRequest request) {
    BigDecimal baseFee = calculateBaseFee(request.getDistance(), request.getWeight());
    BigDecimal urgencyMultiplier = getUrgencyMultiplier(request.getPriority());
    BigDecimal locationMultiplier = getLocationMultiplier(request.getProvince());
    
    BigDecimal calculatedFee = baseFee.multiply(urgencyMultiplier).multiply(locationMultiplier);
    
    // Allow sender override within reasonable bounds
    if (request.getCustomFee() != null) {
        BigDecimal minFee = calculatedFee.multiply(0.5); // 50% discount max
        BigDecimal maxFee = calculatedFee.multiply(2.0); // 100% premium max
        return Math.max(minFee, Math.min(maxFee, request.getCustomFee()));
    }
    
    return calculatedFee;
}
```

#### Validation Rules
- **Item Price**: Must be > $0 (unless already paid)
- **Delivery Fee**: Must be ≥ $0 (can be free)
- **Total Amount**: Item Price + Delivery Fee
- **Reasonable Bounds**: System prevents unrealistic pricing

### Analytics Integration

#### Sender Behavior Tracking
- **Average Delivery Fees**: How much senders typically charge
- **Free Delivery Rate**: Percentage of subsidized deliveries
- **Pricing Strategy**: Standard vs custom fee usage
- **Profitability**: Revenue vs delivery costs

#### Market Intelligence
- **Regional Pricing**: Fee variations by province/district
- **Category Pricing**: Different fees for different item types
- **Time-based Trends**: Peak hour pricing patterns
- **Competition Analysis**: How local rates compare

This process creates a seamless experience where senders can easily set up deliveries with flexible pricing while the system handles all the complex logistics behind the scenes! 🚀

### Delivery Item Management

#### POST /deliveries
Create new delivery item

**Request:**
```json
{
  "receiverPhone": "+1234567890",
  "receiverName": "John Doe",
  "deliveryType": "COMPANY", // or "DRIVER"
  "companyName": "FastDelivery Inc",
  "companyPhone": "+1987654321",
  "driverPhone": "+1555123456", // alternative to company
  "itemDescription": "Electronics package",
  "itemPhotos": ["photo1.jpg", "photo2.jpg"],
  "pickupAddress": "123 Sender St, City, State",
  "pickupProvince": "Phnom Penh",
  "pickupDistrict": "Daun Penh",
  "deliveryAddress": "456 Receiver Ave, City, State",
  "deliveryProvince": "Phnom Penh",
  "deliveryDistrict": "Chamkarmon",
  "estimatedValue": 299.99,
  "deliveryFee": 3.50, // Optional: Custom delivery fee
  "deliveryFeeModel": "STANDARD", // Optional: "STANDARD", "CUSTOM", "FREE"
  "specialInstructions": "Handle with care"
}
```

#### GET /deliveries
List user's deliveries (as sender)

**Query Parameters:**
- `status`: Filter by status
- `page`, `size`: Pagination

#### GET /deliveries/{id}
Get delivery details

#### PUT /deliveries/{id}/status
Update delivery status (for drivers/companies)

**Request:**
```json
{
  "status": "PICKED_UP",
  "location": "Current location description",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "notes": "Picked up successfully"
}
```

### Entity Claiming

#### POST /api/companies/claim
Claim unverified company

**Request:**
```json
{
  "companyId": "uuid",
  "verificationCode": "123456"
}
```

#### POST /api/drivers/claim
Claim driver account

#### POST /api/receivers/claim
Claim receiver account

### Photo Upload

#### POST /api/uploads/photos
Upload delivery item photos

**Multipart Form Data:**
- `files`: Multiple image files
- `deliveryId`: Associated delivery (optional)

## Business Logic Flows

### 1. Delivery Creation Flow
```
1. User submits delivery request
2. Validate sender authentication
3. Process recipient info:
   - Check if receiver exists by phone
   - If not, create unverified receiver account
4. Process delivery method:
   - If COMPANY: Check/create unverified company
   - If DRIVER: Check/create unverified driver
5. Upload and process photos
6. Calculate delivery fee
7. Create DeliveryItem with CREATED status
8. Send notifications to involved parties
```

### 2. Entity Claiming Flow
```
1. Unverified entity user receives invitation
2. User initiates claim via Telegram verification
3. System validates phone number matches
4. Transfer ownership to claiming user
5. Update entity verification status
6. Grant appropriate permissions
```

### 3. Status Update Flow
```
1. Authorized user (driver/company) updates status
2. Validate permission to update
3. Create DeliveryTracking record
4. Update DeliveryItem status
5. Send notifications to sender/receiver
6. Handle special cases (delivered, cancelled, etc.)
```

## Integration Points

### Existing Systems
- **User Authentication**: JWT tokens, Telegram verification
- **Company Management**: Extend existing company model
- **Phone Verification**: Reuse OTP system
- **File Upload**: Integrate with existing upload infrastructure

### External Services
- **Maps Integration**: Address geocoding, route optimization
- **Payment Processing**: Delivery fees, COD payments
- **SMS/Telegram**: Notifications and updates
- **Image Processing**: Photo optimization, OCR for receipts

## Technical Challenges & Solutions

### 1. **Entity Auto-Creation**
**Challenge**: Creating unverified entities without compromising security
**Solution**:
- Use phone number as unique identifier
- Require verification before granting full access
- Implement claiming workflow with time limits

### 2. **Photo Management**
**Challenge**: Handling multiple photos per delivery with optimization
**Solution**:
- Cloud storage integration (AWS S3, etc.)
- Image compression and format optimization
- CDN for fast delivery
- Backup and redundancy

### 3. **Real-time Tracking**
**Challenge**: Providing live updates to multiple stakeholders
**Solution**:
- WebSocket connections for real-time updates
- Push notifications via Telegram
- SMS fallbacks for critical updates
- Polling API for simple integrations

### 4. **Permission Management**
**Challenge**: Complex permission matrix for different user types
**Solution**:
- Role-based access control extension
- Entity ownership verification
- Time-based permissions for temporary access

## Database Schema Extensions

### New Tables
```sql
-- Delivery items
CREATE TABLE delivery_items (
    id UUID PRIMARY KEY,
    sender_id UUID REFERENCES users(id),
    receiver_id UUID REFERENCES users(id),
    delivery_company_id UUID REFERENCES companies(id),
    delivery_driver_id UUID REFERENCES users(id),
    item_description TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES users(id),
    is_deleted BOOLEAN DEFAULT FALSE,
    estimated_delivery_time TIMESTAMP WITH TIME ZONE,
    pickup_address TEXT,
    pickup_province VARCHAR(100),
    pickup_district VARCHAR(100),
    delivery_address TEXT,
    delivery_province VARCHAR(100),
    delivery_district VARCHAR(100),
    pickup_lat DECIMAL, pickup_lng DECIMAL,
    delivery_lat DECIMAL, delivery_lng DECIMAL,
    delivery_fee DECIMAL(10,2),
    item_value DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'USD'
);

-- Delivery tracking history
CREATE TABLE delivery_tracking (
    id UUID PRIMARY KEY,
    delivery_item_id UUID REFERENCES delivery_items(id),
    status VARCHAR(20) NOT NULL,
    description TEXT,
    timestamp TIMESTAMP WITH TIME ZONE,
    updated_by UUID REFERENCES users(id),
    location TEXT,
    latitude DECIMAL, longitude DECIMAL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES users(id),
    is_deleted BOOLEAN DEFAULT FALSE
);

-- Delivery photos
CREATE TABLE delivery_photos (
    id UUID PRIMARY KEY,
    delivery_item_id UUID REFERENCES delivery_items(id),
    photo_url TEXT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE,
    sequence_order INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES users(id),
    is_deleted BOOLEAN DEFAULT FALSE
);
```

### Extended Tables
```sql
-- Add to companies table
ALTER TABLE companies ADD COLUMN verified BOOLEAN DEFAULT FALSE;
ALTER TABLE companies ADD COLUMN claimed_by UUID REFERENCES users(id);
ALTER TABLE companies ADD COLUMN delivery_services TEXT[];

-- Add to users table for driver functionality
ALTER TABLE users ADD COLUMN vehicle_type VARCHAR(50);
ALTER TABLE users ADD COLUMN license_plate VARCHAR(20);
ALTER TABLE users ADD COLUMN availability_status VARCHAR(20);
ALTER TABLE users ADD COLUMN driver_rating DECIMAL(3,2);
ALTER TABLE users ADD COLUMN completed_deliveries INTEGER DEFAULT 0;
```

## Implementation Phases

### Phase 1: Core Delivery Creation
- Basic delivery item creation
- Photo upload functionality
- Simple status tracking

### Phase 2: Entity Management
- Auto-creation of companies/drivers
- Claiming workflow integration
- Extended user profiles

### Phase 3: Advanced Features
- Real-time tracking
- Route optimization
- Payment integration
- Analytics and reporting

### Phase 4: Mobile Optimization
- Camera integration
- Offline functionality
- Push notifications

## Success Metrics
- Delivery creation time < 2 minutes
- Photo upload success rate > 99%
- Real-time update latency < 5 seconds
- Entity claiming conversion rate > 70%
- User satisfaction score > 4.5/5

## Risk Mitigation
- **Data Privacy**: Implement proper data retention policies
- **Fraud Prevention**: Phone verification and identity checks
- **System Reliability**: Comprehensive error handling and fallbacks
- **Scalability**: Design for horizontal scaling from day one

This design provides a solid foundation for the Product Delivery module while leveraging existing authentication and verification systems.