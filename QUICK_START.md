# ROLUUN API - Quick Start Guide

## ✅ Setup Complete!

Your ROLUUN API is now running in Docker and accessible on your local network.

---

## 🌐 Access URLs

### From Windows (Your Development Machine):
- **Using hostname**: `http://roluun.dev:8081/api`
- **Using localhost**: `http://localhost:8081/api`

### From Mobile Devices on Same WiFi Network:
Choose ONE of these methods:

#### **Option 1: Using Computer Name (Recommended)**
Your computer name is: **TRX13**

**Mobile App Base URL**:
```
http://TRX13.local:8081/api
```

**Note**: This requires Bonjour/mDNS support:
- **iOS/macOS**: Works automatically
- **Android**: May need a Bonjour browser app or use Option 2
- **Windows PC**: Install [Bonjour Print Services](https://support.apple.com/kb/DL999)

#### **Option 2: Using IP Address** (Simple but changes with network)
Your current local IP: **192.168.10.170**

**Mobile App Base URL**:
```
http://192.168.10.170:8081/api
```

If you prefer a custom hostname (e.g., `roluun.dev`) you can use the included nginx reverse proxy. It listens on host port 8082 by default and proxies requests to `/api` → the API container. After starting the stack and mapping `roluun.dev` to your host IP you can use:

```
http://roluun.dev:8082/api
```

⚠️ **Note**: This IP will change when you connect to different WiFi networks. You'll need to update your mobile app configuration.

---

## 📱 Mobile App Configuration

### Development Setup
Set your API base URL in your mobile app configuration:

**For React Native**:
```javascript
// config.js
const API_BASE_URL = __DEV__ 
  ? 'http://TRX13.local:8081/api'  // or use IP: http://192.168.10.170:8081/api
  : 'https://roluun.com/api';      // Production URL

export default {
  API_BASE_URL
};
```

**For Flutter**:
```dart
// lib/config/api_config.dart
class ApiConfig {
  static const bool isDevelopment = true;
  
  static const String baseUrl = isDevelopment
      ? 'http://TRX13.local:8081/api'  // or use IP
      : 'https://roluun.com/api';
}
```

### Example API Calls
```javascript
// Login
POST http://TRX13.local:8081/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

// Get deliveries
GET http://TRX13.local:8081/api/deliveries
Authorization: Bearer <your_token>

// Create delivery
POST http://TRX13.local:8081/api/deliveries
Authorization: Bearer <your_token>
Content-Type: application/json

{
  "recipientName": "John Doe",
  "recipientPhone": "+85512345678",
  ...
}
```

---

## 🐳 Docker Management

### Start the API
```bash
docker-compose up -d
```

### Stop the API
```bash
docker-compose down
```

### View Logs
```bash
# View all logs
docker-compose logs -f api

# View last 50 lines
docker-compose logs --tail=50 api
```

### Rebuild After Code Changes
```bash
docker-compose up --build -d
```

### Check Status
```bash
docker-compose ps
```

### Or Use Helper Script (Windows)
```bash
./docker-start.bat
```

---

## 🔧 Testing the Setup

### Test from Windows
```bash
# Test health endpoint
curl http://localhost:8081/api/actuator/health

# Or using hostname
curl http://roluun.dev:8081/api/actuator/health
```

### Test from Mobile Device
1. Ensure mobile device is connected to **same WiFi network**
2. Open mobile browser and navigate to:
   - `http://TRX13.local:8081/api/actuator/health` (if Bonjour works)
   - `http://192.168.10.170:8081/api/actuator/health` (using IP)
3. You should see: `{"status":"UP"}`

---

## 🛠️ Troubleshooting

### Mobile can't connect to API

**Check WiFi**:
- Both devices must be on the **same WiFi network**
- Check if your network allows device-to-device communication (some public WiFi blocks this)

**Check Firewall**:
1. Open "Windows Defender Firewall" → Advanced Settings
2. Inbound Rules → New Rule
3. Port → TCP → Port 8081 → Allow
4. Apply to all profiles

**Test connectivity from mobile**:
```bash
# From mobile terminal/app
ping TRX13.local
# or
ping 192.168.10.170
```

### API not responding

**Check containers are running**:
```bash
docker-compose ps
```

**Check API logs for errors**:
```bash
docker-compose logs --tail=100 api
```

**Restart containers**:
```bash
docker-compose restart
```

### Port 8081 already in use

Stop the non-Docker API first:
```bash
./run-api.sh stop
```

Then start Docker:
```bash
docker-compose up -d
```

### IP address changed

When you connect to a different WiFi network:
1. Get new IP: `ipconfig | grep -i "ipv4"`
2. Update mobile app configuration with new IP
3. **OR** use `TRX13.local` which works regardless of IP

---

## 📦 What's Included

### Services Running
- **PostgreSQL Database** (port 5433 → 5432)
  - Container: `delivery-postgres`
  - Hostname: `roluun-db`
  
- **Spring Boot API** (port 8081)
  - Container: `roluun-api`
  - Hostname: `roluun.dev`
  - Context Path: `/api`

### Configuration
- Server binds to `0.0.0.0` (all network interfaces)
- Context path set to `/api`
- All endpoints prefixed with `/api`
- Database connection via Docker network
- Volumes for data persistence

---

## 🚀 Production Deployment (Future)

When ready to deploy to production:

1. **Choose a Cloud Provider**:
   - Azure App Service
   - AWS Elastic Beanstalk
   - DigitalOcean App Platform
   - Heroku

2. **Configure Domain**:
   - Register `roluun.com`
   - Point DNS A record to server IP
   - Set up SSL certificate (Let's Encrypt)

3. **Update Mobile App**:
   - Change base URL to `https://roluun.com/api`
   - Deploy new version to app stores

4. **Environment Variables**:
   - Use production database credentials
   - Set strong JWT secret
   - Disable dev mode

---

## 📝 Summary

✅ API running in Docker with hostname `roluun.dev`  
✅ Accessible from Windows via `http://localhost:8081/api`  
✅ Accessible from mobile via `http://TRX13.local:8081/api` or `http://192.168.10.170:8081/api`  
✅ All endpoints prefixed with `/api`  
✅ No ngrok needed - pure local network access  
✅ Works across different WiFi locations (using computer name)  

---

## 📞 Need Help?

Check the detailed setup guide: `LOCAL_NETWORK_SETUP.md`

For API documentation, see: `testing_guide.md`

---

**Last Updated**: December 2, 2025  
**Your Computer**: TRX13  
**Current IP**: 192.168.10.170  
**API Container**: roluun-api  
**Status**: ✅ Running
