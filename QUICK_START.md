# Quick Start Guide - Shared Services

## 🚀 Quick Test Flow

### 1. Start Infrastructure (One-time)
```bash
cd ecommerce/ecom-infrastructure
docker-compose up -d
```

### 2. Start Shared Services (5 terminals)

**Terminal 1 - IAM:**
```bash
cd shared-services/shared-iam && mvn spring-boot:run
```

**Terminal 2 - User Profile:**
```bash
cd shared-services/shared-user-profile && mvn spring-boot:run
```

**Terminal 3 - Address:**
```bash
cd shared-services/shared-address && mvn spring-boot:run
```

**Terminal 4 - Payment:**
```bash
cd shared-services/shared-payment && mvn spring-boot:run
```

**Terminal 5 - Gateway:**
```bash
cd shared-services/shared-gateway && mvn spring-boot:run
```

### 3. Test Registration (Quick Test)
```bash
# Register user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!@#","role":"CUSTOMER"}'

# Should return accessToken - use it for next requests
```

### 4. Test with Frontend
```bash
cd ecommerce/frontend
pnpm dev
# Open http://localhost:3000
```

## ✅ Health Check All Services

```bash
curl http://localhost:8081/actuator/health  # IAM
curl http://localhost:8082/actuator/health  # User Profile
curl http://localhost:8083/actuator/health  # Address
curl http://localhost:8089/actuator/health  # Payment
curl http://localhost:8080/actuator/health  # Gateway
```

All should return: `{"status":"UP"}`

## 📦 Publishing

**Automatic (via GitHub Actions):**
- Push to `main` branch → Auto-publishes

**Manual:**
```bash
cd shared-services
mvn clean deploy
```

## 🔍 Verify Published Packages

Visit: https://github.com/subhm4dev/shared-services/packages

You should see:
- `shared-iam:1.0.0`
- `shared-user-profile:1.0.0`
- `shared-address:1.0.0`
- `shared-payment:1.0.0`
- `shared-gateway:1.0.0`

