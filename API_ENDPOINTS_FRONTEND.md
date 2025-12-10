# API Endpoints for Frontend Integration

**Base URL:** `http://localhost:8081/api`

## Authentication Header
All authenticated endpoints require:
```
Authorization: Bearer <access_token>
```

---

## 1. User Profile Management

### GET /auth/profile
Get current user's profile information.

**Request:**
```http
GET /api/auth/profile
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
{
  "id": "uuid",
  "displayName": "John Doe",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe",
  "userType": "COMPANY",
  "userRole": "OWNER",
  "companyId": "uuid",
  "companyName": "My Company",
  "incomplete": false,
  "avatarUrl": "https://example.com/avatar.jpg",
  "phoneNumber": "+855123456789",
  "email": "john@example.com",
  "address": "123 Main Street, Phnom Penh",
  "defaultProvinceId": "uuid",
  "defaultDistrictId": "uuid"
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token

---

### PUT /auth/profile
Update current user's profile.

**Request:**
```http
PUT /api/auth/profile
Authorization: Bearer <token>
Content-Type: application/json

{
  "userType": "COMPANY",
  "firstName": "John",
  "lastName": "Doe",
  "displayName": "John D",
  "companyName": "My Company",
  "avatarUrl": "https://example.com/avatar.jpg",
  "email": "john@example.com",
  "address": "123 Main Street, Phnom Penh",
  "defaultProvinceId": "uuid-of-province",
  "defaultDistrictId": "uuid-of-district"
}
```

**All fields are optional** - only send fields you want to update.

**Response (200 OK):**
```json
{
  "message": "Profile updated successfully"
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token
- `400 Bad Request`: Invalid data or cannot change company
- `404 Not Found`: User not found

**Notes:**
- Phone number cannot be updated via this endpoint (requires re-verification)
- If `companyName` is provided:
  - If user has no company: Creates new company or joins existing one
  - If user is OWNER: Renames their company
  - If user is not OWNER: Returns error (cannot change company)

---

## 2. Company Management

### GET /companies/my
Get current user's company information.

**Request:**
```http
GET /api/companies/my
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
{
  "id": "uuid",
  "name": "My Delivery Company",
  "phoneNumber": "+855123456789",
  "address": "123 Business Street, Phnom Penh",
  "categoryCode": "DELIVERY",
  "categoryName": "Delivery Services",
  "categoryNameKm": "សេវាកម្មដឹកជញ្ជូន",
  "provinceName": "Phnom Penh",
  "provinceNameKm": "ភ្នំពេញ",
  "districtName": "Chamkar Mon",
  "districtNameKm": "ចំការមន",
  "active": true
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token
- `400 Bad Request`: User is not part of any company
- `404 Not Found`: User not found

---

### PUT /companies/my
Update current user's company information (OWNER only).

**Request:**
```http
PUT /api/companies/my
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Updated Company Name",
  "address": "456 New Street, Phnom Penh",
  "phoneNumber": "+855987654321",
  "provinceId": "uuid-of-province",
  "districtId": "uuid-of-district",
  "categoryId": "uuid-of-category"
}
```

**Field Details:**
- `name` (required): Company name
- `address` (optional): Company address
- `phoneNumber` (optional): Company phone number
- `provinceId` (optional): UUID of province (foreign key)
- `districtId` (optional): UUID of district (foreign key)
- `categoryId` (optional): UUID of company category (foreign key)

**Response (200 OK):**
```json
{
  "id": "uuid",
  "name": "Updated Company Name",
  "phoneNumber": "+855987654321",
  "address": "456 New Street, Phnom Penh",
  "categoryCode": "DELIVERY",
  "categoryName": "Delivery Services",
  "categoryNameKm": "សេវាកម្មដឹកជញ្ជូន",
  "provinceName": "Phnom Penh",
  "provinceNameKm": "ភ្នំពេញ",
  "districtName": "Chamkar Mon",
  "districtNameKm": "ចំការមន",
  "active": true
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token
- `403 Forbidden`: Only company owners can update company information
- `400 Bad Request`: User is not part of any company or validation error
- `404 Not Found`: User not found

**Authorization:**
- Only users with `userRole: "OWNER"` can update company information

---

## 3. Geographic Data (for dropdowns)

### GET /provinces
Get all provinces.

**Request:**
```http
GET /api/provinces
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
[
  {
    "id": "uuid",
    "name": "Phnom Penh",
    "nameKm": "ភ្នំពេញ",
    "code": "PP"
  },
  {
    "id": "uuid",
    "name": "Kandal",
    "nameKm": "កណ្តាល",
    "code": "KD"
  }
]
```

---

### GET /provinces/{provinceId}/districts
Get districts by province.

**Request:**
```http
GET /api/provinces/{provinceId}/districts
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
[
  {
    "id": "uuid",
    "name": "Chamkar Mon",
    "nameKm": "ចំការមន",
    "provinceId": "uuid"
  },
  {
    "id": "uuid",
    "name": "Daun Penh",
    "nameKm": "ដូនពេញ",
    "provinceId": "uuid"
  }
]
```

---

## 4. Company Categories (for dropdowns)

### GET /company-categories
Get all company categories.

**Request:**
```http
GET /api/company-categories
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
[
  {
    "id": "uuid",
    "code": "DELIVERY",
    "name": "Delivery Services",
    "nameKm": "សេវាកម្មដឹកជញ្ជូន"
  },
  {
    "id": "uuid",
    "code": "JEWELRY",
    "name": "Jewelry Store",
    "nameKm": "ហាងគ្រឿងអលង្ការ"
  }
]
```

---

## Common Response Codes

- `200 OK`: Request successful
- `201 Created`: Resource created successfully
- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Missing or invalid authentication token
- `403 Forbidden`: Authenticated but not authorized for this action
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

---

## Example Frontend Implementation (JavaScript/TypeScript)

```javascript
// Configure base API URL
const API_BASE_URL = 'http://localhost:8081/api';

// Get auth token from storage
const getAuthToken = () => localStorage.getItem('access_token');

// API Client
const api = {
  // Get current user profile
  async getProfile() {
    const response = await fetch(`${API_BASE_URL}/auth/profile`, {
      headers: {
        'Authorization': `Bearer ${getAuthToken()}`
      }
    });
    if (!response.ok) throw new Error('Failed to fetch profile');
    return response.json();
  },

  // Update user profile
  async updateProfile(data) {
    const response = await fetch(`${API_BASE_URL}/auth/profile`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${getAuthToken()}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    });
    if (!response.ok) throw new Error('Failed to update profile');
    return response.json();
  },

  // Get user's company
  async getMyCompany() {
    const response = await fetch(`${API_BASE_URL}/companies/my`, {
      headers: {
        'Authorization': `Bearer ${getAuthToken()}`
      }
    });
    if (!response.ok) throw new Error('Failed to fetch company');
    return response.json();
  },

  // Update company (OWNER only)
  async updateMyCompany(data) {
    const response = await fetch(`${API_BASE_URL}/companies/my`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${getAuthToken()}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    });
    if (!response.ok) throw new Error('Failed to update company');
    return response.json();
  },

  // Get all provinces
  async getProvinces() {
    const response = await fetch(`${API_BASE_URL}/provinces`, {
      headers: {
        'Authorization': `Bearer ${getAuthToken()}`
      }
    });
    if (!response.ok) throw new Error('Failed to fetch provinces');
    return response.json();
  },

  // Get districts by province
  async getDistricts(provinceId) {
    const response = await fetch(`${API_BASE_URL}/provinces/${provinceId}/districts`, {
      headers: {
        'Authorization': `Bearer ${getAuthToken()}`
      }
    });
    if (!response.ok) throw new Error('Failed to fetch districts');
    return response.json();
  },

  // Get company categories
  async getCompanyCategories() {
    const response = await fetch(`${API_BASE_URL}/company-categories`, {
      headers: {
        'Authorization': `Bearer ${getAuthToken()}`
      }
    });
    if (!response.ok) throw new Error('Failed to fetch categories');
    return response.json();
  }
};

// Usage Examples
async function loadUserProfile() {
  try {
    const profile = await api.getProfile();
    console.log('User profile:', profile);
  } catch (error) {
    console.error('Error loading profile:', error);
  }
}

async function updateUserProfile() {
  try {
    await api.updateProfile({
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      address: '123 Main St',
      defaultProvinceId: 'province-uuid',
      defaultDistrictId: 'district-uuid'
    });
    console.log('Profile updated successfully');
  } catch (error) {
    console.error('Error updating profile:', error);
  }
}

async function updateCompany() {
  try {
    await api.updateMyCompany({
      name: 'My Company',
      address: '456 Business Ave',
      phoneNumber: '+855123456789',
      provinceId: 'province-uuid',
      districtId: 'district-uuid',
      categoryId: 'category-uuid'
    });
    console.log('Company updated successfully');
  } catch (error) {
    console.error('Error updating company:', error);
  }
}
```

---

## React Hook Example

```typescript
import { useState, useEffect } from 'react';

interface UserProfile {
  id: string;
  displayName: string;
  username: string;
  firstName: string;
  lastName: string;
  email?: string;
  phoneNumber: string;
  address?: string;
  defaultProvinceId?: string;
  defaultDistrictId?: string;
  userType: string;
  userRole: string;
  companyId?: string;
  companyName?: string;
}

export function useUserProfile() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadProfile();
  }, []);

  async function loadProfile() {
    try {
      setLoading(true);
      const data = await api.getProfile();
      setProfile(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function updateProfile(updates: Partial<UserProfile>) {
    try {
      await api.updateProfile(updates);
      await loadProfile(); // Reload to get updated data
    } catch (err) {
      setError(err.message);
      throw err;
    }
  }

  return { profile, loading, error, updateProfile, reload: loadProfile };
}
```

---

## Testing with cURL

```bash
# Get user profile
curl -X GET http://localhost:8081/api/auth/profile \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Update user profile
curl -X PUT http://localhost:8081/api/auth/profile \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "address": "123 Main St",
    "defaultProvinceId": "province-uuid",
    "defaultDistrictId": "district-uuid"
  }'

# Get company
curl -X GET http://localhost:8081/api/companies/my \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Update company
curl -X PUT http://localhost:8081/api/companies/my \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Company",
    "address": "456 Business Ave",
    "phoneNumber": "+855123456789",
    "provinceId": "province-uuid",
    "districtId": "district-uuid",
    "categoryId": "category-uuid"
  }'

# Get provinces
curl -X GET http://localhost:8081/api/provinces \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Get districts by province
curl -X GET http://localhost:8081/api/provinces/{provinceId}/districts \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Get company categories
curl -X GET http://localhost:8081/api/company-categories \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## Notes for Frontend Team

1. **Token Management:**
   - Store access token securely (localStorage or sessionStorage)
   - Include token in `Authorization: Bearer <token>` header for all authenticated requests
   - Handle 401 responses by redirecting to login

2. **Form Validation:**
   - All PUT endpoints accept partial updates (only send changed fields)
   - Company name is required when updating company
   - Province/District/Category use UUID foreign keys (use dropdowns)

3. **User Roles:**
   - Only `userRole: "OWNER"` can update company information
   - Check user role before showing company edit UI

4. **Error Handling:**
   - Check response status codes
   - Display appropriate error messages to users
   - Handle network errors gracefully

5. **Geographic Data:**
   - Load provinces on component mount
   - Load districts when province is selected
   - Clear district selection when province changes

6. **Company Creation Flow:**
   - Users can create company via PUT /auth/profile with `companyName`
   - Once company exists, use PUT /companies/my for updates
