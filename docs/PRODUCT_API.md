# Product API Documentation

## Overview
The Product API provides endpoints for managing products in the delivery system. Products can be browsed, searched, created, and embedded into delivery payloads for streamlined order creation.

## Authentication
All endpoints require JWT authentication via `Authorization: Bearer <token>` header.

## Endpoints

### 1. Search Products
**GET** `/api/products/search`

Advanced search endpoint with filtering and pagination for browsing products.

**Query Parameters:**
- `query` (optional): Search term to match against product name and description
- `category` (optional): Filter by category name or code
- `published` (optional): Filter by published status (true/false)
- `page` (optional, default: 0): Page number for pagination
- `limit` (optional, default: 20): Items per page (max: 100)

**Response:**
```json
{
  "products": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "iPhone 15 Pro",
      "description": "Latest iPhone with A17 Pro chip",
      "categoryId": "660e8400-e29b-41d4-a716-446655440001",
      "categoryName": "Electronics",
      "defaultPrice": 1200.00,
      "buyingPrice": 1000.00,
      "sellingPrice": 1200.00,
      "lastSellPrice": 1200.00,
      "isPublished": true,
      "weightKg": 0.187,
      "dimensions": "146.6 x 70.6 x 7.8 mm",
      "productPhotos": [
        {
          "id": "770e8400-e29b-41d4-a716-446655440002",
          "url": "https://example.com/images/iphone15pro.jpg"
        }
      ],
      "isActive": true,
      "usageCount": 42,
      "lastUsedAt": "2025-12-01T10:30:00Z",
      "companyId": "880e8400-e29b-41d4-a716-446655440003",
      "companyName": "Tech Solutions Co."
    }
  ],
  "total": 145,
  "page": 0,
  "limit": 20,
  "hasMore": true
}
```

**Example Requests:**
```bash
# Search for "iPhone"
GET /api/products/search?query=iPhone

# Filter by category
GET /api/products/search?category=Electronics

# Get published products only
GET /api/products/search?published=true

# Combine filters with pagination
GET /api/products/search?query=phone&category=Electronics&published=true&page=1&limit=10
```

---

### 2. Get Product Suggestions
**GET** `/api/products/suggestions`

Quick autocomplete/typeahead suggestions for product names.

**Query Parameters:**
- `query` (required): Search term (minimum 1 character)

**Response:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "iPhone 15 Pro",
    "defaultPrice": 1200.00,
    "lastSellPrice": 1200.00,
    "productPhotos": [...]
  }
]
```

**Example:**
```bash
GET /api/products/suggestions?query=iPh
```

---

### 3. Get All Products (Company Scope)
**GET** `/api/products`

Get all products for the current user's company.

**Query Parameters:**
- `search` (optional): Filter products by name

**Response:** Array of ProductDTO

**Example:**
```bash
# All company products
GET /api/products

# Search within company products
GET /api/products?search=laptop
```

---

### 4. Get Product by ID
**GET** `/api/products/{productId}`

Retrieve detailed information about a specific product.

**Response:** Single ProductDTO

**Example:**
```bash
GET /api/products/550e8400-e29b-41d4-a716-446655440000
```

---

### 5. Create Product
**POST** `/api/products`

Create a new product (requires OWNER, MANAGER, or STAFF role).

**Request Body:**
```json
{
  "name": "iPhone 15 Pro",
  "description": "Latest iPhone with A17 Pro chip, 256GB storage",
  "category": "ELECTRONICS",
  "defaultPrice": 1200.00,
  "buyingPrice": 1000.00,
  "sellingPrice": 1200.00,
  "lastSellPrice": 1200.00,
  "isPublished": true,
  "productPhotos": [
    "https://example.com/images/iphone15pro-1.jpg",
    "https://example.com/images/iphone15pro-2.jpg"
  ]
}
```

**Response:** ProductDTO of created product

---

### 6. Update Product
**PUT** `/api/products/{productId}`

Update an existing product (requires OWNER, MANAGER, or STAFF role).

**Request Body:** Same as Create Product (all fields optional)

**Response:** ProductDTO of updated product

---

### 7. Deactivate Product
**POST** `/api/products/{productId}/deactivate`

Soft delete a product (requires OWNER, MANAGER, or STAFF role).

**Response:**
```json
"Product deactivated successfully"
```

---

### 8. Add Photo to Product
**POST** `/api/products/{productId}/photos`

Add a photo to an existing product.

**Request Body:**
```json
{
  "imageRef": "https://example.com/images/photo.jpg"
}
```

**Response:** ProductDTO with updated photos

---

### 9. Remove Photo from Product
**DELETE** `/api/products/{productId}/photos/{photoId}`

Remove a specific photo from a product.

**Response:** ProductDTO with updated photos

---

## Embedding Products in Delivery Payloads

When creating a delivery, you can reference products to auto-fill item details:

### Option 1: Reference by Product ID
```json
{
  "receiverPhone": "012345678",
  "receiverName": "John Doe",
  "items": [
    {
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "quantity": 2,
      "price": 1200.00,
      "itemDiscount": 50.00
    }
  ]
}
```

### Option 2: Use Product Name (Auto-create/lookup)
```json
{
  "receiverPhone": "012345678",
  "receiverName": "John Doe",
  "items": [
    {
      "productName": "iPhone 15 Pro",
      "itemDescription": "256GB, Blue",
      "quantity": 1,
      "price": 1200.00
    }
  ]
}
```

### Frontend Workflow
1. **Search Products**: Use `/api/products/search?query=iPhone` to browse available products
2. **Select Product**: User picks from search results
3. **Auto-fill Delivery**: Use product details (price, photos, description) in delivery creation form
4. **Create Delivery**: Submit with `productId` or `productName` in items array

---

## Product Photo Upload Flow

1. **Upload Image**: POST `/api/images/upload` (multipart/form-data)
   ```bash
   curl -X POST http://localhost:8081/api/images/upload \
     -H "Authorization: Bearer <token>" \
     -F "image=@product-photo.jpg"
   ```
   Response: `{ "imageId": "...", "url": "https://..." }`

2. **Create/Update Product**: Include image URL in `productPhotos` array
   ```json
   {
     "name": "iPhone 15 Pro",
     "productPhotos": ["https://example.com/images/uploaded-photo.jpg"]
   }
   ```

---

## Response Fields

### ProductDTO Structure
```typescript
interface ProductDTO {
  id: string;                    // UUID
  name: string;                  // Product name
  description?: string;          // Detailed description
  categoryId?: string;           // Category UUID
  categoryName?: string;         // Category display name
  defaultPrice: number;          // Default selling price
  buyingPrice?: number;          // Purchase/cost price
  sellingPrice?: number;         // Current selling price
  lastSellPrice?: number;        // Last sold price
  isPublished: boolean;          // Visible to customers
  weightKg?: number;             // Product weight
  dimensions?: string;           // Physical dimensions
  productPhotos: ImageDTO[];     // Product images
  isActive: boolean;             // Not deleted
  usageCount: number;            // Times used in deliveries
  lastUsedAt?: string;           // ISO 8601 timestamp
  companyId: string;             // Owner company UUID
  companyName: string;           // Company display name
}

interface ImageDTO {
  id: string;                    // Image UUID
  url: string;                   // Image URL
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "error": "Invalid product data"
}
```

### 401 Unauthorized
```json
{
  "error": "User not authenticated"
}
```

### 403 Forbidden
```json
{
  "error": "Not authorized"
}
```

### 404 Not Found
```json
{
  "error": "Product not found"
}
```

---

## Best Practices

### For Frontend Integration
1. **Use Search Endpoint**: Prefer `/api/products/search` for browsing with filters
2. **Implement Autocomplete**: Use `/api/products/suggestions` for typeahead search
3. **Cache Results**: Cache product lists locally to reduce API calls
4. **Pagination**: Request only needed pages (20-50 items per page recommended)
5. **Lazy Load Images**: Load product photos on-demand for better performance

### For Product Management
1. **Upload Photos First**: Always upload images before creating/updating products
2. **Set Default Price**: Always provide `defaultPrice` for accurate cost tracking
3. **Use Categories**: Categorize products for better organization and filtering
4. **Mark as Published**: Only set `isPublished: true` for customer-facing products
5. **Track Usage**: Monitor `usageCount` to identify popular products

### For Delivery Creation
1. **Reference by ID**: Use `productId` when available for accurate product linking
2. **Include Photos**: Product photos auto-populate in delivery items
3. **Update Usage Stats**: System automatically tracks product usage in deliveries
4. **Price Override**: Can override product price with item-specific `price` field

---

## Examples

### Complete Frontend Workflow Example

```javascript
// 1. Search for products
const searchResults = await fetch('/api/products/search?query=iPhone&published=true&limit=10', {
  headers: { 'Authorization': `Bearer ${token}` }
}).then(r => r.json());

// 2. User selects product from results
const selectedProduct = searchResults.products[0];

// 3. Create delivery with product
const deliveryPayload = {
  receiverPhone: "012345678",
  receiverName: "John Doe",
  deliveryType: "COMPANY",
  companyName: "Fast Delivery Co.",
  deliveryAddress: "123 Main St",
  deliveryProvince: "Phnom Penh",
  deliveryDistrict: "Chamkar Mon",
  deliveryFee: 5.00,
  items: [
    {
      productId: selectedProduct.id,        // Link to product
      productName: selectedProduct.name,     // Auto-filled
      itemDescription: selectedProduct.description,
      price: selectedProduct.defaultPrice,   // Use product price
      quantity: 2,
      itemPhotos: selectedProduct.productPhotos.map(p => p.url) // Auto-filled
    }
  ]
};

// 4. Submit delivery
const delivery = await fetch('/api/deliveries', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify(deliveryPayload)
}).then(r => r.json());
```

---

## Migration Notes

### Existing Functionality
- GET `/api/products` with `?search=` parameter remains functional
- GET `/api/products/{id}` unchanged
- All product CRUD operations backward compatible

### New Functionality
- **NEW**: GET `/api/products/search` with advanced filtering
- **ENHANCED**: Pagination support via `page` and `limit` parameters
- **ENHANCED**: Category and published status filtering
- **ENHANCED**: Structured response with metadata (total, hasMore, etc.)

### Breaking Changes
None - all existing endpoints remain backward compatible.
