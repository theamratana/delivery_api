# Product Search Feature - Quick Start

## What's New

✅ **GET /api/products/search** - Advanced product search endpoint with filtering and pagination

## Why Use This?

The new search endpoint provides:
- **Better UX**: Structured responses with pagination metadata
- **Flexible Filtering**: Search by query, category, published status
- **Frontend Ready**: Designed for browse/select workflows in delivery creation
- **Performance**: Pagination prevents loading huge product lists

## Quick Examples

### 1. Browse All Products
```bash
GET /api/products/search
```

### 2. Search by Name
```bash
GET /api/products/search?query=iPhone
```

### 3. Filter by Category
```bash
GET /api/products/search?category=Electronics
```

### 4. Published Products Only
```bash
GET /api/products/search?published=true
```

### 5. Combined with Pagination
```bash
GET /api/products/search?query=phone&category=Electronics&published=true&page=0&limit=10
```

### 6. Autocomplete (Fast Typeahead)
```bash
GET /api/products/suggestions?query=iP
```

## Response Format

```json
{
  "products": [
    {
      "id": "uuid",
      "name": "iPhone 15 Pro",
      "description": "...",
      "defaultPrice": 1200.00,
      "productPhotos": [...],
      "categoryName": "Electronics",
      "isPublished": true,
      "usageCount": 42
    }
  ],
  "total": 145,        // Total matching products
  "page": 0,          // Current page
  "limit": 20,        // Items per page
  "hasMore": true     // More pages available
}
```

## Frontend Workflow

```javascript
// 1. User types in search box
const results = await fetch('/api/products/search?query=iPhone&published=true')
  .then(r => r.json());

// 2. Display results with "Load More" button
results.products.forEach(product => {
  // Show product card
});

if (results.hasMore) {
  // Show "Load More" button → fetch page 1
}

// 3. User selects product for delivery
const selectedProduct = results.products[0];

// 4. Create delivery with product
const delivery = {
  receiverPhone: "012345678",
  items: [{
    productId: selectedProduct.id,
    productName: selectedProduct.name,
    price: selectedProduct.defaultPrice,
    quantity: 1,
    itemPhotos: selectedProduct.productPhotos.map(p => p.url)
  }]
};
```

## Files to Check

📖 **Full Documentation**: `docs/PRODUCT_API.md`
📝 **Examples**: `product-search-examples.json`
🧪 **Test Script**: `test-product-search.sh`
📚 **README**: Updated with product endpoints section

## Backward Compatibility

✅ All existing endpoints still work:
- GET /products (with optional ?search=)
- GET /products/{id}
- GET /products/suggestions?query=
- POST /products
- PUT /products/{id}
- All photo management endpoints

## Testing

Run the test script:
```bash
./test-product-search.sh
```

Or test manually with curl:
```bash
# Get token
TOKEN=$(curl -s -X POST http://localhost:8081/auth/dev/token/new | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Search products
curl -X GET "http://localhost:8081/api/products/search?query=iPhone&limit=5" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

## Common Use Cases

### 1. Product Catalog Browse
```
GET /api/products/search?published=true&page=0&limit=20
```
Perfect for displaying a product catalog to customers.

### 2. Inventory Management
```
GET /api/products/search?published=false
```
List unpublished/draft products for staff.

### 3. Category Browsing
```
GET /api/products/search?category=Electronics&published=true
```
Filter by specific category.

### 4. Search Within Category
```
GET /api/products/search?query=phone&category=Electronics
```
Combine search and filter.

### 5. Quick Lookup (Autocomplete)
```
GET /api/products/suggestions?query=iPho
```
Fast typeahead for input fields.

## Tips

💡 **Pagination**: Start with limit=20-50 for optimal performance
💡 **Caching**: Cache results for 5-10 seconds to reduce API calls
💡 **Debouncing**: Debounce search input by 300ms before calling API
💡 **Progressive Loading**: Load first page immediately, additional pages on demand
💡 **Fallback**: If search returns no results, try broader query or remove filters

## Next Steps

1. ✅ Endpoint created and tested
2. ✅ Documentation written
3. ✅ Backward compatibility verified
4. 🔄 Frontend integration (your turn!)
5. 🔄 Deploy to production

Need help? Check `docs/PRODUCT_API.md` for complete examples!
