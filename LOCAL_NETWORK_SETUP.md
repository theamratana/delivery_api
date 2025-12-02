# Local Network Setup Guide for ROLUUN API

## Overview
This guide shows how to access the ROLUUN API from your mobile app on the same local network using the hostname `roluun.dev` instead of IP addresses.

## Setup Steps

### 1. Configure Windows Hosts File (On Your Development Machine)

Add the following entry to your Windows hosts file to map `roluun.dev` to localhost:

**File Location**: `C:\Windows\System32\drivers\etc\hosts`

**Add this line**:
```
127.0.0.1       roluun.dev
```

**How to edit**:
1. Open Notepad as Administrator
2. File → Open → Navigate to `C:\Windows\System32\drivers\etc\hosts`
3. Add the line above
4. Save the file

### 2. Build and Run with Docker

```bash
# Stop any running containers
docker-compose down

# Build and start services
docker-compose up --build -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f api
```

The API will be available at:
- From Windows host: `http://roluun.dev:8081/api`
- From Windows host: `http://localhost:8081/api`
If you prefer to use a hostname without a port in your mobile app, you can run the local nginx reverse-proxy (included in this repo) and map it to the `roluun.dev` hostname.

This repo ships an nginx reverse-proxy which listens on host port 8082 by default and proxies /api → the API container. To use it:

1. Start the stack:

```bash
docker-compose up -d
```

2. Map `roluun.dev` to your host IP or 127.0.0.1 in your device's hosts file (or test in a browser using the port):

```text
# on your development machine (Windows hosts file)
127.0.0.1    roluun.dev

# or use your LAN IP so mobile devices can reach it e.g.
192.168.10.170    roluun.dev
```

3. Open the API through the proxy:

```
http://roluun.dev:8082/api
```

If you want `roluun.dev` to work without a port (standard HTTP port 80) later, change the proxy service port mapping in `docker-compose.yml` from `8082:80` to `80:80` (requires admin privileges on Windows). You can also add HTTPS using mkcert — ask me and I can add it.

### 3. Configure Mobile Devices on Same Network

For mobile devices to access the API using `roluun.dev`, you need to configure DNS or hosts file on each device.

#### Option A: Use IP Address (Simple, but changes with network)
1. Find your Windows machine's local IP:
   ```bash
   ipconfig | findstr /i "ipv4"
   ```
   Look for address like `192.168.x.x`

2. Configure mobile app to use:
   ```
   http://<YOUR_LOCAL_IP>:8081/api
   ```

#### Option B: Use mDNS/Bonjour (Recommended)
1. Install Bonjour Print Services on Windows (free from Apple)
2. Your machine will be accessible as: `<COMPUTERNAME>.local`
3. Find your computer name:
   ```bash
   hostname
   ```
4. Configure mobile app to use:
   ```
   http://<COMPUTERNAME>.local:8081/api
   ```

#### Option C: Local DNS Server (Advanced)
Set up a local DNS server (like Pi-hole or dnsmasq) on your network to resolve `roluun.dev` to your machine's IP. This works for all devices automatically.

### 4. Mobile App Configuration

In your mobile app (Flutter/React Native/etc.), set the base API URL:

**Development Environment**:
```javascript
const API_BASE_URL = 'http://roluun.dev:8081/api';  // On Windows host
// OR
const API_BASE_URL = 'http://<COMPUTER>.local:8081/api';  // From mobile devices
// OR
const API_BASE_URL = 'http://192.168.x.x:8081/api';  // Using IP (temporary)
```

**Production Environment** (after hosting):
```javascript
const API_BASE_URL = 'https://roluun.com/api';
```

### 5. Testing the Setup

Test from Windows host:
```bash
curl http://roluun.dev:8081/api/actuator/health
```

Test from mobile device (replace with your method):
```bash
# On mobile browser or app
http://<COMPUTER>.local:8081/api/actuator/health
```

### 6. Firewall Configuration

Ensure Windows Firewall allows port 8081:
1. Windows Security → Firewall & network protection → Advanced settings
2. Inbound Rules → New Rule
3. Port → TCP → 8081 → Allow the connection
4. Apply to all profiles

### 7. Docker Commands Reference

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# Rebuild after code changes
docker-compose up --build -d

# View logs
docker-compose logs -f api

# Restart just the API
docker-compose restart api

# Check container status
docker ps
```

## Recommended Approach

For local development with mobile apps, I recommend **Option B (mDNS/Bonjour)**:

1. Install Bonjour Print Services on Windows
2. Use `http://<COMPUTERNAME>.local:8081/api` in your mobile app
3. This works across different WiFi networks without IP changes
4. Your computer name stays the same regardless of network

## Troubleshooting

### Cannot access from mobile device
- Verify both devices are on same WiFi network
- Check Windows Firewall allows port 8081
- Ping the Windows machine from mobile device
- Ensure Docker containers are running: `docker ps`

### "roluun.dev" not resolving on Windows
- Check hosts file was edited correctly
- Run `ipconfig /flushdns` to clear DNS cache
- Try `ping roluun.dev` - should resolve to 127.0.0.1

### API not responding
- Check logs: `docker-compose logs api`
- Verify database is healthy: `docker-compose ps`
- Test locally first: `curl http://localhost:8081/api/actuator/health`

## Production Migration

When ready to deploy to production on `roluun.com`:
1. Deploy to cloud provider (Azure, AWS, DigitalOcean)
2. Configure DNS A record: `roluun.com` → `<SERVER_IP>`
3. Set up SSL certificate (Let's Encrypt)
4. Update mobile app base URL to `https://roluun.com/api`
5. Update `server.servlet.context-path=/api` (already configured)
