# Database Reset Script - Quick Reference

## Usage

Run the script:
```bash
./reset-db.sh
```

## Options

### 0) FULL RESET
Deletes ALL user data while preserving settings:
- ✅ Deletes: Users, Companies, Deliveries, Products, Customers, Auth data
- ⛔ Preserves: Provinces, Districts, Company Categories, Product Categories, Exchange Rates

**Use this for**: Starting fresh from signup/telegram process

### 1) AUTHENTICATION MODULE
Deletes: OTP, Telegram data, JWT tokens
- `auth_identities`
- `otp_attempts`
- `user_phones`

**Use this for**: Testing login/signup flows

### 2) USER MANAGEMENT MODULE
Deletes: All users (including non-customers), audit trails
- `users` (all types)
- `user_audits`

**Use this for**: Testing user registration

### 3) COMPANY MANAGEMENT MODULE
Deletes: Companies, employees, invitations, categories
- `companies`
- `employees`
- `company_invitations`
- `pending_employees`
- ⛔ Preserves: `company_categories` (setting)

**Use this for**: Testing company creation/management

### 4) DELIVERY MODULE
Deletes: All delivery records
- `delivery_items`
- `delivery_tracking`
- `delivery_photos`

**Use this for**: Testing delivery creation

### 5) PRODUCT MODULE
Deletes: Products and categories
- `products`
- `product_photos`
- ⛔ Preserves: `product_categories` (setting)

**Use this for**: Testing product management

### 6) CUSTOMER MODULE
Deletes: Only customer records (user_type = 'CUSTOMER')
- Customers only from `users` table

**Use this for**: Testing customer management

### 7) PRICING MODULE
Deletes: Delivery pricing rules
- `delivery_pricing_rules`

**Use this for**: Testing pricing configuration

## Examples

### Full Reset for Fresh Testing
```bash
# Run script and select option 0
./reset-db.sh
# Enter: 0
# Confirm: y
```

### Reset Multiple Modules
```bash
# Run script and select options 1 3 4 (auth + companies + deliveries)
./reset-db.sh
# Enter: 1 3 4
# Confirm: y
```

### Quick Full Reset (Non-interactive)
```bash
echo -e "0\ny" | ./reset-db.sh
```

## What Gets Preserved

The following reference data is **ALWAYS PRESERVED**:

### Geographic Data
- ✅ **Provinces** (25 provinces)
- ✅ **Districts** (218 districts)

### Category Settings
- ✅ **Company Categories** (2 categories):
  - `DELIVERY` - Delivery (សេវាដឹកជញ្ជូន)
  - `JEWELRY` - Jewelry Store (ហាងគ្រឿងអលង្ការ)

- ✅ **Product Categories** (8 categories):
  - `ELECTRONICS` - Electronics (អេឡិចត្រូនិច)
  - `CLOTHING` - Clothing (សម្លៀកបំពាក់)
  - `FOOD` - Food (អាហារ)
  - `BOOKS` - Books (សៀវភៅ)
  - `COSMETICS` - Cosmetics (គ្រឿងសម្អាង)
  - `MEDICINE` - Medicine (ឱសថ)
  - `DOCUMENTS` - Documents (ឯកសារ)
  - `OTHER` - Other (ផ្សេងៗ)

### Exchange Rates
- ✅ **Exchange Rates** (1 rate):
  - USD to KHR: 4000.0000 (1 USD = 4000 KHR)

### System
- ✅ Database schema/structure

## Verification

After each reset, the script shows a verification table:
```
table_name                    | record_count | status
------------------------------+--------------+----------
auth_identities               | 0            | CLEARED
provinces (settings)          | 25           | PRESERVED
districts (settings)          | 218          | PRESERVED
company_categories (settings) | 8            | PRESERVED
product_categories (settings) | 12           | PRESERVED
exchange_rates (settings)     | 1            | PRESERVED
...
```

- `CLEARED` = Successfully deleted
- `PRESERVED` = Intentionally kept

## Database Credentials

Default:
- Username: `postgres`
- Password: `postgres`
- Database: `deliverydb`
- Port: `5433` (via Docker)

Override with environment variables:
```bash
export DB_USERNAME=myuser
export DB_PASSWORD=mypass
./reset-db.sh
```

## For Your Testing Scenario

**"I want to test from signup with telegram process"**

Use **Option 0 (FULL RESET)**:
```bash
./reset-db.sh
# Enter: 0
# Confirm: y
```

This will:
1. ✅ Delete all users → Fresh signup
2. ✅ Delete all auth data → Fresh Telegram login
3. ✅ Delete all companies → Fresh company creation
4. ✅ Delete all deliveries → Fresh delivery testing
5. ✅ Delete all products → Fresh product testing
6. ✅ Delete all customers → Fresh customer management
7. ⛔ Keep provinces/districts → No need to re-import
8. ⛔ Keep company categories → No need to re-import
9. ⛔ Keep product categories → No need to re-import
10. ⛔ Keep exchange rates → No need to re-import

After reset, you can:
1. Open Telegram mini-app
2. Sign up with phone number
3. Verify OTP
4. Create company profile
5. Test all features from scratch

## Troubleshooting

**Script permission denied?**
```bash
chmod +x reset-db.sh
```

**Database connection error?**
```bash
# Check Docker is running
docker ps | grep delivery-postgres

# If not running, start it
docker compose up -d
```

**Want to see what will be deleted?**
- The script shows a confirmation before executing
- Read the module descriptions carefully
- You can quit anytime by entering 'q'
