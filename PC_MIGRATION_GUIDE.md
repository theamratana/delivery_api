# PC Migration Guide - Delivery API

**Migration Date:** December 10, 2025  
**Database Backup:** `database-backup-20251210.sql` (147KB)

---

## 📋 Pre-Migration Checklist

✅ **Git Repository:**
- All changes committed and pushed to GitHub
- Latest commit: `84b99bf` - "feat: enhance profile/company APIs and update Telegram bot configuration"
- Repository: https://github.com/theamratana/delivery_api.git
- Branch: `main`

✅ **Database Backup:**
- File: `database-backup-20251210.sql`
- Size: 147KB
- Database: `deliverydb` (PostgreSQL 16)
- Includes: All tables, data, indexes, and constraints

✅ **Environment Configuration:**
- Telegram Bot: RoluunBot (token configured in docker-compose.yml)
- API Port: 8081
- Database Port: 5433 (Docker) → 5432 (Container)

---

## 🚀 New PC Setup Instructions

### Step 1: Install Required Software

1. **Install Git**
   ```bash
   # Windows: Download from https://git-scm.com/download/win
   # Verify installation
   git --version
   ```

2. **Install Docker Desktop**
   ```bash
   # Windows: Download from https://www.docker.com/products/docker-desktop
   # Verify installation
   docker --version
   docker compose version
   ```

3. **Install Java 17 (for development)**
   ```bash
   # Download OpenJDK 17 from https://adoptium.net/
   # Or use SDKMan (Linux/Mac):
   # curl -s "https://get.sdkman.io" | bash
   # sdk install java 17.0.9-tem
   
   # Verify installation
   java -version
   ```

4. **Install IDE (Optional)**
   - IntelliJ IDEA: https://www.jetbrains.com/idea/download/
   - VS Code: https://code.visualstudio.com/

---

### Step 2: Clone Repository

```bash
# Create project directory
mkdir -p ~/IdeaProjects/delivery
cd ~/IdeaProjects/delivery

# Clone repository
git clone https://github.com/theamratana/delivery_api.git
cd delivery_api

# Verify you're on main branch
git branch
git status
```

---

### Step 3: Copy Database Backup

**Option A: From USB/External Drive**
```bash
# Copy the backup file to project directory
cp /path/to/database-backup-20251210.sql ~/IdeaProjects/delivery/delivery_api/
```

**Option B: From Cloud Storage**
- Upload `database-backup-20251210.sql` to Google Drive/Dropbox
- Download to new PC into project directory

**Option C: Via Git (if file size permits)**
```bash
# On old PC - add to git (already done if you see this file)
git add database-backup-20251210.sql
git commit -m "chore: add database backup for PC migration"
git push

# On new PC - pull the backup
git pull
```

---

### Step 4: Start Docker Containers

```bash
cd ~/IdeaProjects/delivery/delivery_api

# Start database and API containers
docker compose up -d

# Wait 10 seconds for database to initialize
sleep 10

# Check containers are running
docker compose ps
# Should see:
# - delivery-postgres (healthy)
# - api (running)
```

---

### Step 5: Restore Database

```bash
# Stop API container to prevent conflicts
docker compose stop api

# Restore database from backup
docker exec -i delivery-postgres psql -U postgres -d deliverydb < database-backup-20251210.sql

# Start API container
docker compose start api

# Verify API started successfully
docker logs roluun-api --tail 50
# Should see: "Started DeliveryApiApplication in X.XXX seconds"
```

---

### Step 6: Verify Everything Works

**Test 1: Check API Health**
```bash
curl http://localhost:8081/api/health
# Should return: {"status":"UP"}
```

**Test 2: Check Database Connection**
```bash
docker exec -it delivery-postgres psql -U postgres -d deliverydb -c "SELECT COUNT(*) FROM users;"
# Should return row count (e.g., 5+)
```

**Test 3: Test Authentication**
```bash
# Use Postman collection: Delivery-API.postman_collection.json
# Import environment: Delivery-API-Local.postman_environment.json
# Test: POST /auth/telegram/request-otp
```

**Test 4: Check Logs**
```bash
# API logs
docker logs roluun-api --tail 100

# Database logs
docker logs delivery-postgres --tail 50
```

---

## 📁 Important Files to Copy

### Must Copy (Already in Git)
- [x] Source code (all in git)
- [x] `docker-compose.yml` (with RoluunBot config)
- [x] `application.properties` (with Telegram bot settings)
- [x] Postman collection files
- [x] Documentation files

### Must Copy (NOT in Git - Copy Manually)
- [ ] `database-backup-20251210.sql` (147KB) - **CRITICAL**
- [ ] `.env` file (if you created one with secrets)
- [ ] Any uploaded files in `uploads/` directory (if important)

### Optional (can regenerate)
- [ ] `build/` directory (can rebuild with `./gradlew build`)
- [ ] `logs/` directory (new logs will be created)
- [ ] `.gradle/` directory (Gradle cache)

---

## 🔧 Environment Variables

### Current Configuration (in docker-compose.yml)

**Database:**
```yaml
DB_URL=jdbc:postgresql://localhost:5433/deliverydb
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

**Telegram Bot:**
```yaml
TELEGRAM_BOT_TOKEN=8585454641:AAHmpA7V0trkot2KZEID5FOXOFKz8THBUn0
TELEGRAM_BOT_USERNAME=RoluunBot
```

**API Server:**
```yaml
SERVER_PORT=8081
```

### If Using .env File (Alternative)
Create `.env` in project root:
```env
DB_URL=jdbc:postgresql://localhost:5433/deliverydb
DB_USERNAME=postgres
DB_PASSWORD=postgres
TELEGRAM_BOT_TOKEN=8585454641:AAHmpA7V0trkot2KZEID5FOXOFKz8THBUn0
TELEGRAM_BOT_USERNAME=RoluunBot
SERVER_PORT=8081
```

---

## 🗄️ Database Information

**Database Details:**
- **Name:** deliverydb
- **User:** postgres
- **Password:** postgres
- **Host:** localhost
- **Port:** 5433 (external) / 5432 (container internal)
- **Engine:** PostgreSQL 16

**Key Tables:**
- `users` - User accounts and profiles
- `companies` - Company information
- `provinces` - Province data (25 provinces)
- `districts` - District data (197 districts)
- `company_categories` - Business categories
- `products` - Product catalog
- `customers` - Customer management
- `deliveries` - Delivery tracking
- `delivery_items` - Delivery line items
- `delivery_packages` - Package tracking
- `delivery_pricing_rules` - Pricing configuration
- `exchange_rates` - Currency exchange rates
- `user_audits` - Audit trail

**Important Data Preserved:**
- System admin user
- Default product categories (8 categories)
- Geographic data (provinces/districts)
- All test companies and users
- Authentication tokens
- Delivery history

---

## 🛠️ Common Issues & Solutions

### Issue 1: Docker containers won't start
```bash
# Check Docker is running
docker ps

# Restart Docker Desktop
# Then try again:
docker compose up -d
```

### Issue 2: Port 8081 or 5433 already in use
```bash
# Check what's using the port
netstat -ano | findstr :8081
netstat -ano | findstr :5433

# Kill the process or change ports in docker-compose.yml
```

### Issue 3: Database restore fails
```bash
# Drop and recreate database
docker exec -it delivery-postgres psql -U postgres -c "DROP DATABASE IF EXISTS deliverydb;"
docker exec -it delivery-postgres psql -U postgres -c "CREATE DATABASE deliverydb;"

# Restore again
docker exec -i delivery-postgres psql -U postgres -d deliverydb < database-backup-20251210.sql
```

### Issue 4: API can't connect to database
```bash
# Check database is healthy
docker exec delivery-postgres pg_isready -U postgres

# Check connection from API container
docker exec roluun-api ping delivery-postgres

# Restart both containers
docker compose restart
```

### Issue 5: Gradle build fails
```bash
# Clean build
./gradlew clean build --refresh-dependencies

# Or use Gradle wrapper installation
./gradlew wrapper --gradle-version 8.5
./gradlew clean build
```

---

## 📞 Testing Telegram Bot Integration

**Telegram Bot:** RoluunBot (@RoluunBot)

**Test Login Flow:**
1. Open http://localhost:8081/ (if you have a login page)
2. Or use Postman:
   ```bash
   # Request OTP
   POST http://localhost:8081/api/auth/telegram/request-otp
   {
     "phoneNumber": "+855123456789"
   }
   
   # Response includes deepLink:
   # "https://t.me/RoluunBot?start=otp_XXXXX"
   ```
3. Open deepLink in Telegram
4. Bot should send OTP code
5. Verify OTP to get access token

---

## 🔐 Security Checklist

- [ ] Change default database password in production
- [ ] Keep Telegram bot token secret (don't commit to public repos)
- [ ] Use environment variables for sensitive data
- [ ] Enable HTTPS in production
- [ ] Set up proper firewall rules
- [ ] Regular database backups (automated)

---

## 📝 Quick Command Reference

```bash
# Start everything
docker compose up -d

# Stop everything
docker compose down

# View logs
docker compose logs -f

# Restart API only
docker compose restart api

# Access database shell
docker exec -it delivery-postgres psql -U postgres -d deliverydb

# Build project
./gradlew clean build

# Run tests
./gradlew test

# Create new database backup
docker exec delivery-postgres pg_dump -U postgres deliverydb > backup-$(date +%Y%m%d-%H%M%S).sql

# Restore from backup
docker exec -i delivery-postgres psql -U postgres -d deliverydb < backup-file.sql
```

---

## 📊 Project Structure

```
delivery_api/
├── src/
│   ├── main/
│   │   ├── java/com/delivery/deliveryapi/
│   │   │   ├── controller/      # REST API endpoints
│   │   │   ├── service/         # Business logic
│   │   │   ├── repository/      # Database access
│   │   │   ├── model/           # JPA entities
│   │   │   ├── dto/             # Data transfer objects
│   │   │   └── config/          # Configuration classes
│   │   └── resources/
│   │       ├── application.properties  # Main config
│   │       └── migration-*.sql         # Database migrations
│   └── test/                    # Unit tests
├── docker-compose.yml           # Container orchestration
├── build.gradle                 # Gradle build config
├── database-backup-20251210.sql # Database backup
├── API_ENDPOINTS_FRONTEND.md    # API documentation
└── README.md                    # Project documentation
```

---

## ✅ Migration Completion Checklist

After setup on new PC, verify:

- [ ] Git repository cloned successfully
- [ ] Docker containers running (postgres + api)
- [ ] Database restored from backup
- [ ] API accessible at http://localhost:8081
- [ ] Postman collection imports correctly
- [ ] Can request OTP via API
- [ ] Telegram bot responds (deepLink works)
- [ ] Can authenticate and get access token
- [ ] Can fetch user profile
- [ ] Can update profile (test new fields)
- [ ] Can manage company data
- [ ] Logs show no errors

---

## 🎯 Next Steps After Migration

1. **Update local environment:**
   ```bash
   cd ~/IdeaProjects/delivery/delivery_api
   git pull  # Ensure you have latest code
   ```

2. **Configure IDE:**
   - Import project as Gradle project
   - Set JDK 17 as project SDK
   - Configure Spring Boot run configuration

3. **Set up automated backups:**
   ```bash
   # Add to cron/Task Scheduler
   0 2 * * * docker exec delivery-postgres pg_dump -U postgres deliverydb > ~/backups/deliverydb-$(date +\%Y\%m\%d).sql
   ```

4. **Continue development:**
   - All your work is preserved
   - Database has all test data
   - Ready to continue from where you left off

---

## 📧 Support

If you encounter issues:
1. Check Docker logs: `docker compose logs`
2. Check API logs: `docker logs roluun-api`
3. Verify database connection: `docker exec delivery-postgres pg_isready`
4. Review this guide's troubleshooting section

---

**Important:** Keep the `database-backup-20251210.sql` file safe until you've verified everything works on the new PC!

**Estimated Migration Time:** 30-60 minutes (depending on internet speed and familiarity with tools)

Good luck with your PC migration! 🚀
