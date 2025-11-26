# Delivery API Module Testing Guide

## 1. Authentication Setup

First, get an authentication token:

### For Admin User:
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"username":"Admin","password":"newAdminPassword123"}' \
  http://localhost:8081/api/auth/login
```

### For Regular User (Dev):
```bash
curl -X GET "http://localhost:8081/api/auth/dev/login/123e4567-e89b-12d3-a456-426614174003"
```

## 2. Product Category Module

### 2.1 Initialize Default Categories
```bash
curl -X POST "http://localhost:8081/api/product-categories/initialize-defaults"
```

### 2.2 Get All Categories
```bash
curl -X GET "http://localhost:8081/api/product-categories"
```

### 2.3 Create New Category (Admin only)
```bash
curl -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -d '{"code":"TEST","name":"Test Category","khmerName":"សាកល្បង","sortOrder":100}' \
  http://localhost:8081/api/product-categories
```

### 2.4 Update Category (Admin only)
```bash
curl -X PUT -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -d '{"name":"Updated Test Category","sortOrder":101}' \
  http://localhost:8081/api/product-categories/{category-id}
```

### 2.5 Deactivate Category
```bash
curl -X POST -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  http://localhost:8081/api/product-categories/{category-id}/deactivate
```

## 3. Product Module

### 3.1 Get Company Products
```bash
curl -X GET -H "Authorization: Bearer YOUR_TOKEN" \
  "http://localhost:8081/api/products"
```

### 3.2 Search Products
```bash
curl -X GET -H "Authorization: Bearer YOUR_TOKEN" \
  "http://localhost:8081/api/products?search=laptop"
```

### 3.3 Update Product
```bash
curl -X PUT -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"name":"Updated Product Name","defaultPrice":25.50}' \
  http://localhost:8081/api/products/{product-id}
```

### 3.4 Deactivate Product
```bash
curl -X POST -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8081/api/products/{product-id}/deactivate

### 3.5 Upload Image and Attach to Product
1. Upload image(s):
```bash
curl -X POST -H "Authorization: Bearer YOUR_TOKEN" -F "files=@/path/to/image.jpg" http://localhost:8081/api/images/upload
```
The response contains an object with `id` and `url` for each uploaded image. Use the `id` when referencing the image from `productPhotos`.

2. Create or update product including `productPhotos` with image `id` or `url`:
```bash
curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"name":"New Product","productPhotos":["<image-id>"] }' \
  http://localhost:8081/api/products
```

3. Add a single photo to an existing product:
```bash
curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer YOUR_TOKEN" \
 -d '{"imageRef":"<image-id>"}' http://localhost:8081/api/products/{product-id}/photos
```

4. Remove a photo from a product:
```bash
curl -X DELETE -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8081/api/products/{product-id}/photos/{photoId}
```

Notes:
- `imageRef` may be the `imageId` returned by `/api/images/upload` or the `url`.
- The API validates that the image belongs to the same company or was uploaded by the same user.
```

## 4. Delivery Module

### 4.1 Create Delivery
```bash
curl -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "receiverPhone": "+85512345678",
    "receiverName": "John Doe",
    "deliveryType": "COMPANY",
    "companyName": "ABC Company",
    "companyPhone": "+85587654321",
    "itemDescription": "Documents",
    "pickupAddress": "123 Main St, Phnom Penh",
    "pickupProvince": "Phnom Penh",
    "pickupDistrict": "Chamkarmon",
    "deliveryAddress": "456 Oak Ave, Phnom Penh",
    "deliveryProvince": "Phnom Penh",
    "deliveryDistrict": "Daun Penh",
    "deliveryFee": 15.00,
    "estimatedValue": 100.00
  }' \
  http://localhost:8081/api/deliveries
```

### 4.2 Get User Deliveries
```bash
curl -X GET -H "Authorization: Bearer YOUR_TOKEN" \
  "http://localhost:8081/api/deliveries"
```

### 4.3 Get Specific Delivery
```bash
curl -X GET -H "Authorization: Bearer YOUR_TOKEN" \
  "http://localhost:8081/api/deliveries/{delivery-id}"
```

## Testing Order

1. **Start the application**: `./run-api.sh start`
2. **Authentication**: Get tokens using the auth endpoints above
3. **Product Categories**: Initialize defaults, then test CRUD operations
4. **Products**: Test product operations (may need categories first)
5. **Deliveries**: Test delivery creation and retrieval

## Notes

- Replace `YOUR_TOKEN` with actual JWT tokens from authentication
- Replace `{category-id}`, `{product-id}`, `{delivery-id}` with actual UUIDs
- Admin-only endpoints require SYSTEM_ADMINISTRATOR role
- Regular users can only see their company's data
- System admins can see all data across companies
