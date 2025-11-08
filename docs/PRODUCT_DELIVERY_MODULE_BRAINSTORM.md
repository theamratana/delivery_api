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

    String itemDescription;
    List<String> itemPhotos;  // URLs to uploaded images
    DeliveryStatus status;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    OffsetDateTime estimatedDeliveryTime;

    // Location tracking
    String pickupAddress;
    String deliveryAddress;
    Double pickupLat, pickupLng;
    Double deliveryLat, deliveryLng;

    // Pricing
    BigDecimal deliveryFee;
    BigDecimal itemValue;
    String currency;

    // Metadata
    Map<String, String> metadata;
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

## API Design

### Delivery Item Management

#### POST /api/deliveries
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
  "deliveryAddress": "456 Receiver Ave, City, State",
  "estimatedValue": 299.99,
  "specialInstructions": "Handle with care"
}
```

#### GET /api/deliveries
List user's deliveries (as sender)

**Query Parameters:**
- `status`: Filter by status
- `page`, `size`: Pagination

#### GET /api/deliveries/{id}
Get delivery details

#### PUT /api/deliveries/{id}/status
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
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    estimated_delivery_time TIMESTAMP WITH TIME ZONE,
    pickup_address TEXT,
    delivery_address TEXT,
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
    latitude DECIMAL, longitude DECIMAL
);

-- Delivery photos
CREATE TABLE delivery_photos (
    id UUID PRIMARY KEY,
    delivery_item_id UUID REFERENCES delivery_items(id),
    photo_url TEXT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE,
    sequence_order INTEGER
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