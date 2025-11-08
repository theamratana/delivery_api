# User Account Module Summary

## Overview
The User Account module provides comprehensive user management, authentication, and authorization functionality for the Delivery API. It supports multi-tenant company structures with employee management, Telegram-based authentication, JWT security, and OTP verification.

## Completed Features

### 1. Project Setup and Infrastructure
- **Spring Boot 3.4.0** application with Gradle build system
- **Java 21 LTS** runtime (upgraded from Java 17)
- PostgreSQL database integration with JPA/Hibernate
- Docker Compose setup for local development
- Comprehensive logging and request tracing

### 2. Authentication & Authorization
- **Telegram Bot Integration**: Users authenticate via Telegram bot with polling mechanism
- **JWT Token Management**: Secure token-based authentication with configurable expiration
- **Role-Based Access Control**: Support for different user roles and types
- **Security Configuration**: Spring Security integration with custom filters

### 3. User Management
- **User Registration**: Multi-step registration process with phone verification
- **Profile Management**: User profile updates and information retrieval
- **Company Structure**: Multi-tenant architecture with companies and employees
- **Employee Management**: Company admins can manage employees and invitations

### 4. Verification Systems
- **OTP (One-Time Password)**: SMS-based verification for phone numbers
- **Telegram Verification**: Bot-based user verification and authentication
- **Phone Number Validation**: International phone number support

### 5. Data Models
- **User**: Core user entity with roles and types
- **Company**: Multi-tenant company management
- **Employee**: Company-employee relationships
- **AuthIdentity**: Authentication provider management
- **OTP Attempts**: Verification attempt tracking
- **User Audit**: User action logging

## Tricky Solutions Implemented

### 1. Telegram Authentication Flow
**Challenge**: Implementing secure authentication via Telegram bot while maintaining stateless API design.

**Solution**:
- Custom `TelegramAuthService` for bot interaction
- Polling mechanism for real-time message handling
- JWT token generation upon successful verification
- Secure token storage and validation

### 2. Multi-Tenant Company Architecture
**Challenge**: Supporting multiple companies with isolated employee management.

**Solution**:
- Company-based data segregation
- Invitation system for employee onboarding
- Role-based permissions within companies
- Secure company context in authentication

### 3. OTP Verification System
**Challenge**: Reliable OTP generation, delivery, and validation with rate limiting.

**Solution**:
- Database-backed OTP attempt tracking
- Configurable expiration and retry limits
- Phone number normalization and validation
- Secure OTP generation and verification

### 4. JWT Security Implementation
**Challenge**: Implementing secure JWT tokens with proper claims and validation.

**Solution**:
- Custom `JwtService` for token management
- Spring Security filter integration
- Configurable token expiration
- Secure secret key management

### 5. Java 21 Upgrade
**Challenge**: Upgrading from Java 17 to 21 LTS while maintaining compatibility.

**Solution**:
- Updated `sourceCompatibility` in build.gradle
- Upgraded Spring Boot to 3.4.0 for Java 21 support
- Verified all dependencies and build process
- Maintained backward compatibility

## Technical Architecture

### Controllers
- `AuthController`: Authentication endpoints
- `UsersController`: User management operations
- `OtpController`: OTP verification endpoints
- `HealthController`: Application health checks

### Services
- `TelegramAuthService`: Telegram bot authentication
- `OtpService`: OTP generation and validation
- `CompanyInvitationService`: Employee invitation management
- `UserPhoneService`: Phone number handling

### Security
- `JwtAuthenticationFilter`: Request authentication
- `JwtService`: Token operations
- `SecurityConfig`: Spring Security configuration

### Configuration
- Telegram bot properties
- JWT configuration
- Database connection settings
- Logging and request tracing

## Database Schema
- Users, companies, employees tables
- OTP attempts and audit logs
- Authentication identities
- Company invitations

## API Documentation

### Enums

#### UserType
```json
{
  "CUSTOMER": "End customer placing orders",
  "DRIVER": "Delivery driver",
  "COMPANY": "Company representative",
  "DELIVERY_COMPANY": "Delivery company admin",
  "STORE_OWNER": "Store/restaurant owner",
  "ADMIN": "System administrator"
}
```

#### UserRole
```json
{
  "OWNER": "Company creator with full access",
  "MANAGER": "Can manage team and orders",
  "STAFF": "Customer service and basic operations",
  "DRIVER": "Delivery personnel"
}
```

#### AuthProvider
```json
{
  "TELEGRAM": "Telegram bot authentication",
  "PHONE": "Phone number authentication",
  "GOOGLE": "Google OAuth",
  "FACEBOOK": "Facebook OAuth",
  "TIKTOK": "TikTok OAuth"
}
```

### Authentication Endpoints

#### POST /auth/telegram/verify
Telegram authentication verification.

**Request Body:**
```json
{
  "id": "123456789",
  "first_name": "John",
  "last_name": "Doe",
  "username": "johndoe",
  "photo_url": "https://example.com/photo.jpg",
  "auth_date": "1640995200",
  "hash": "abc123def456"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "displayName": "John Doe",
  "username": "johndoe",
  "provider": "TELEGRAM"
}
```

**Error Responses:**
- `401`: `{"error": "invalid_signature"}`

#### GET /auth/profile
Get current user profile.

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Response (200):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "displayName": "John Doe",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe",
  "userType": "CUSTOMER",
  "userRole": "OWNER",
  "companyId": "550e8400-e29b-41d4-a716-446655440001",
  "companyName": "Acme Delivery",
  "incomplete": false
}
```

#### PUT /auth/profile
Update user profile.

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Request Body:**
```json
{
  "userType": "DRIVER",
  "firstName": "John",
  "lastName": "Smith",
  "displayName": "John Smith",
  "companyName": "New Company Inc"
}
```

**Response (200):**
```json
{
  "message": "Profile updated successfully"
}
```

#### GET /auth/dev/token/{userId}
Get development token for testing (only when dev mode enabled).

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### OTP Endpoints

#### POST /auth/otp/request
Request OTP verification code.

**Request Body:**
```json
{
  "phone_e164": "+1234567890"
}
```

**Response (200):**
```json
{
  "attemptId": "550e8400-e29b-41d4-a716-446655440000",
  "deepLink": "https://t.me/bot_username?start=otp_550e8400-e29b-41d4-a716-446655440000",
  "expiresAt": "2024-01-01T12:00:00Z",
  "sentDirectly": false
}
```

#### POST /auth/otp/verify
Verify OTP code.

**Request Body:**
```json
{
  "attemptId": "550e8400-e29b-41d4-a716-446655440000",
  "code": "123456"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "displayName": "John Doe",
  "username": "johndoe",
  "provider": "TELEGRAM-OTP"
}
```

**Error Responses:**
- `400`: `{"error": "Invalid or expired verification code"}`
- `409`: `{"error": "Phone number already registered or constraint violation"}`

### Company Management Endpoints

#### POST /auth/companies/{companyId}/add-employee
Add employee to company (requires OWNER or MANAGER role).

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Request Body:**
```json
{
  "role": "DRIVER",
  "phoneNumber": "+1234567890"
}
```

**Response (200):**
```json
{
  "message": "Employee invitation created successfully",
  "phoneNumber": "+1234567890",
  "role": "DRIVER",
  "expiresAt": "2024-01-08T12:00:00Z"
}
```

**Error Responses:**
- `400`: `{"error": "An employee invitation already exists for this phone number"}`
- `403`: `{"error": "Insufficient permissions to add employee"}`

#### GET /auth/companies/{companyId}/employees
Get company employees (requires OWNER or MANAGER role).

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Response (200):**
```json
{
  "employees": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "displayName": "John Doe",
      "firstName": "John",
      "lastName": "Doe",
      "phoneNumber": "+1234567890",
      "role": "OWNER",
      "status": "ACTIVE",
      "invitedAt": null,
      "joinedAt": "2024-01-01T10:00:00Z"
    },
    {
      "id": null,
      "displayName": null,
      "firstName": null,
      "lastName": null,
      "phoneNumber": "+1987654321",
      "role": "DRIVER",
      "status": "PENDING",
      "invitedAt": "2024-01-01T11:00:00Z",
      "joinedAt": null
    }
  ],
  "totalCount": 2
}
```

#### POST /auth/leave-company
Leave current company.

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Response (200):**
```json
{
  "message": "Successfully left company"
}
```

### User Phone Management Endpoints

#### GET /users/{userId}/phones
List user's phone numbers.

**Response (200):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "phoneE164": "+1234567890",
    "primary": true,
    "verified": true,
    "createdAt": "2024-01-01T10:00:00Z"
  }
]
```

#### POST /users/{userId}/phones
Add phone number to user.

**Request Body:**
```json
{
  "phoneE164": "+1234567890",
  "primary": true
}
```

**Response (200):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "phoneE164": "+1234567890",
  "primary": true,
  "verified": false,
  "createdAt": "2024-01-01T10:00:00Z"
}
```

#### PATCH /users/{userId}/phones/{phoneId}/primary
Set phone number as primary.

**Response (200):**
```json
{
  "status": "ok"
}
```

### Health Check Endpoints

#### GET /api/ping
Basic health check.

**Response (200):**
```json
{
  "status": "ok",
  "message": "pong",
  "time": "2024-01-01T12:00:00+00:00"
}
```

#### GET /api/db/ping
Database health check.

**Response (200):**
```json
{
  "status": "ok",
  "db": 1,
  "time": "2024-01-01T12:00:00+00:00"
}
```

## Testing
- Unit tests for services and utilities
- Integration tests for authentication flow
- Database reset scripts for development

## Deployment
- Docker Compose for local development
- Gradle wrapper for consistent builds
- Environment-based configuration
- Health check endpoints

## Future Enhancements
- Email-based authentication
- Social login integration
- Advanced permission management
- User activity analytics
- Multi-factor authentication

## Lessons Learned
1. Telegram bot polling requires careful error handling and reconnection logic
2. JWT claims should include minimal necessary data for performance
3. Multi-tenant architecture requires careful data isolation
4. OTP systems need robust rate limiting and expiration handling
5. Java version upgrades require thorough testing of all dependencies

## Configuration Files
- `application.properties`: Main configuration
- `docker-compose.yml`: Local database setup
- `build.gradle`: Build configuration
- `logback-spring.xml`: Logging configuration

This module provides a solid foundation for user management in the delivery system, with secure authentication and flexible company structures.