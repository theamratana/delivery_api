# Company API Documentation

## Authentication
All endpoints require Bearer token authentication:
```
Authorization: Bearer {accessToken}
```

---

## GET /api/companies/my

Get the authenticated user's company information.

### Request
```http
GET /api/companies/my
Authorization: Bearer {accessToken}
```

### Response - Success (200 OK)
```json
{
  "id": "ed76b4f6-16d2-45f0-96cb-d1b90a6b6f93",
  "name": "Bling Jewelry",
  "phoneNumber": "+85512345678",
  "address": "123 Main Street, Building A",
  "categoryId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "provinceId": "p1234567-89ab-cdef-0123-456789abcdef",
  "districtId": "d1234567-89ab-cdef-0123-456789abcdef"
}
```

### Response Fields
| Field | Type | Description | Nullable |
|-------|------|-------------|----------|
| `id` | string (UUID) | Company unique identifier | No |
| `name` | string | Company name | No |
| `phoneNumber` | string | Company phone number | Yes |
| `address` | string | Company address | Yes |
| `categoryId` | string (UUID) | Company category ID | Yes |
| `provinceId` | string (UUID) | Province/city ID | Yes |
| `districtId` | string (UUID) | District/khan ID | Yes |

### Error Responses

**401 Unauthorized** - Invalid or missing token
```json
{
  "error": "Unauthorized"
}
```

**400 Bad Request** - User not part of any company
```json
{
  "error": "User is not part of any company"
}
```

---

## GET /api/companies/{id}

Get details of a specific company by ID. You can only view your own company or companies created by your company.

### Request
```http
GET /api/companies/{id}
Authorization: Bearer {accessToken}
```

### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | string (UUID) | Company ID |

### Response - Success (200 OK)
```json
{
  "id": "ed76b4f6-16d2-45f0-96cb-d1b90a6b6f93",
  "name": "Bling Jewelry",
  "phoneNumber": "+85512345678",
  "address": "123 Main Street, Building A",
  "categoryId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "provinceId": "p1234567-89ab-cdef-0123-456789abcdef",
  "districtId": "d1234567-89ab-cdef-0123-456789abcdef"
}
```

### Response Fields
Same as GET /api/companies/my

### Error Responses

**401 Unauthorized** - Invalid or missing token
```json
{
  "error": "Unauthorized"
}
```

**400 Bad Request** - User not part of any company
```json
{
  "error": "User is not part of any company"
}
```

**403 Forbidden** - No permission to view this company
```json
{
  "error": "You can only view your own company or companies created by your company"
}
```

**404 Not Found** - Company not found
```json
{
  "error": "Not found"
}
```

---

## PUT /api/companies/{id}

Update a specific company by ID. You can edit your own company (if you're OWNER) or companies created by your company.

### Request
```http
PUT /api/companies/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | string (UUID) | Company ID to update |

### Request Body
```json
{
  "name": "Updated Company Name",
  "address": "123 New Address",
  "phoneNumber": "+85512345678",
  "districtId": "d1234567-89ab-cdef-0123-456789abcdef",
  "provinceId": "p1234567-89ab-cdef-0123-456789abcdef",
  "categoryId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### Request Body Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | No | Company name (max 200 chars, will be trimmed) |
| `address` | string | No | Company address (will be trimmed) |
| `phoneNumber` | string | No | Company phone number (max 20 chars, will be trimmed) |
| `districtId` | string (UUID) | No | District/khan ID (set to `null` to remove) |
| `provinceId` | string (UUID) | No | Province/city ID (set to `null` to remove) |
| `categoryId` | string (UUID) | No | Company category ID (set to `null` to remove) |

**Note:** All fields are optional. Only provide the fields you want to update.

### Response - Success (200 OK)
Same format as GET response:
```json
{
  "id": "ed76b4f6-16d2-45f0-96cb-d1b90a6b6f93",
  "name": "Updated Company Name",
  "phoneNumber": "+85512345678",
  "address": "123 New Address",
  "categoryId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "provinceId": "p1234567-89ab-cdef-0123-456789abcdef",
  "districtId": "d1234567-89ab-cdef-0123-456789abcdef"
}
```

### Error Responses

**401 Unauthorized** - Invalid or missing token
```json
{
  "error": "Unauthorized"
}
```

**400 Bad Request** - User not part of any company
```json
{
  "error": "User is not part of any company"
}
```

**403 Forbidden** - No permission to edit this company
```json
{
  "error": "You can only edit your own company or companies created by your company"
}
```

**403 Forbidden** - Not company owner (when editing own company)
```json
{
  "error": "Only company owners can update their company information"
}
```

**404 Not Found** - Company not found
```json
{
  "error": "Not found"
}
```

---

## PUT /api/companies/my

Update the authenticated user's company information. Only company **OWNER** can update.

### Request
```http
PUT /api/companies/my
Authorization: Bearer {accessToken}
Content-Type: application/json
```

### Request Body
```json
{
  "name": "Bling Jewelry Store",
  "address": "123 Main Street, Building A, Floor 2",
  "phoneNumber": "+85512345678",
  "districtId": "d1234567-89ab-cdef-0123-456789abcdef",
  "provinceId": "p1234567-89ab-cdef-0123-456789abcdef",
  "categoryId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### Request Body Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | **Yes** | Company name (max 200 chars, will be trimmed) |
| `address` | string | No | Company address (will be trimmed) |
| `phoneNumber` | string | No | Company phone number (max 20 chars, will be trimmed) |
| `districtId` | string (UUID) | No | District/khan ID (set to `null` to remove) |
| `provinceId` | string (UUID) | No | Province/city ID (set to `null` to remove) |
| `categoryId` | string (UUID) | No | Company category ID (set to `null` to remove) |

### Response - Success (200 OK)
Same format as GET response:
```json
{
  "id": "ed76b4f6-16d2-45f0-96cb-d1b90a6b6f93",
  "name": "Bling Jewelry Store",
  "phoneNumber": "+85512345678",
  "address": "123 Main Street, Building A, Floor 2",
  "categoryId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "provinceId": "p1234567-89ab-cdef-0123-456789abcdef",
  "districtId": "d1234567-89ab-cdef-0123-456789abcdef"
}
```

### Error Responses

**401 Unauthorized** - Invalid or missing token
```json
{
  "error": "Unauthorized"
}
```

**400 Bad Request** - Validation error
```json
{
  "error": "Company name is required"
}
```

**400 Bad Request** - User not part of any company
```json
{
  "error": "User is not part of any company"
}
```

**403 Forbidden** - User is not company owner
```json
{
  "error": "Only company owners can update company information"
}
```

---

## Notes for Frontend Team

### 1. All UUIDs can be null
The response fields `categoryId`, `provinceId`, and `districtId` will be `null` if not set. Frontend should handle null values gracefully.

### 2. Display names
Frontend should fetch display names separately using the IDs:
- Use `categoryId` to look up category name from `/api/company-categories`
- Use `provinceId` to look up province name from `/api/provinces`
- Use `districtId` to look up district name from `/api/districts`

### 3. Updating company
- Only users with `OWNER` role can update company info
- The `name` field is required (will get validation error if blank)
- All other fields are optional
- Pass `null` explicitly to remove a value

### 4. Province/District relationship
- Districts belong to provinces
- When selecting a district, make sure to also set the correct `provinceId`
- The API doesn't validate the relationship, but it's recommended for data consistency

### 5. Example Usage Flow

**Get current company:**
```javascript
const response = await fetch('/api/companies/my', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const company = await response.json();
```

**Update company:**
```javascript
const response = await fetch('/api/companies/my', {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: 'New Company Name',
    address: '123 New Address',
    phoneNumber: '+85512345678',
    provinceId: 'province-uuid',
    districtId: 'district-uuid',
    categoryId: 'category-uuid'
  })
});
const updatedCompany = await response.json();
```

### 6. Getting Master Data
To populate dropdown/select fields, you'll need separate API calls:

- **Provinces:** GET `/api/provinces` (or similar endpoint)
- **Districts:** GET `/api/districts?provinceId={provinceId}`
- **Categories:** GET `/api/company-categories`

Check with backend team for the exact endpoints to fetch these master data lists.
