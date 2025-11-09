# End-to-End Testing Guide for Shared Services

This guide explains how to test the shared services end-to-end with the namaste-fab website.

## Prerequisites

1. **Infrastructure Running:**
   - PostgreSQL (port 5432)
   - Redis (port 6379)
   - Kafka (port 9092)
   - Config Server (port 8888) - optional

2. **Published Services:**
   - All shared services published to GitHub Packages
   - Or run from source (for local testing)

## Step 1: Publish Shared Services

### Option A: Publish via GitHub Actions (Recommended)

1. Push code to `shared-services` repository:
   ```bash
   cd shared-services
   git add .
   git commit -m "Initial shared services"
   git push origin main
   ```

2. GitHub Actions will automatically:
   - Build all 5 services
   - Publish to GitHub Packages:
     - `shared-iam:1.0.0`
     - `shared-user-profile:1.0.0`
     - `shared-address:1.0.0`
     - `shared-payment:1.0.0`
     - `shared-gateway:1.0.0`

3. Verify publication:
   - Go to: https://github.com/subhm4dev/shared-services/packages

### Option B: Publish Locally

```bash
cd shared-services

# Configure Maven settings.xml with GitHub token
# Then publish:
mvn clean deploy
```

## Step 2: Start Infrastructure

```bash
# Start PostgreSQL, Redis, Kafka
cd ecommerce/ecom-infrastructure
docker-compose up -d

# Start Config Server (optional)
cd ecommerce/ecom-config-server
mvn spring-boot:run
```

## Step 3: Start Shared Services

Open multiple terminals and start each service:

### Terminal 1: IAM Service
```bash
cd shared-services/shared-iam
mvn spring-boot:run
# Runs on port 8081
```

### Terminal 2: User Profile Service
```bash
cd shared-services/shared-user-profile
mvn spring-boot:run
# Runs on port 8082
```

### Terminal 3: Address Service
```bash
cd shared-services/shared-address
mvn spring-boot:run
# Runs on port 8083
```

### Terminal 4: Payment Service
```bash
cd shared-services/shared-payment
mvn spring-boot:run
# Runs on port 8089
```

### Terminal 5: Gateway Service
```bash
cd shared-services/shared-gateway
mvn spring-boot:run
# Runs on port 8080
```

**Verify services are running:**
```bash
# Check IAM
curl http://localhost:8081/actuator/health

# Check User Profile
curl http://localhost:8082/actuator/health

# Check Address
curl http://localhost:8083/actuator/health

# Check Payment
curl http://localhost:8089/actuator/health

# Check Gateway
curl http://localhost:8080/actuator/health
```

## Step 4: Start Ecommerce Services

### Terminal 6: Cart Service
```bash
cd ecommerce/ecom-cart
mvn spring-boot:run
# Runs on port 8087
```

### Terminal 7: Catalog Service
```bash
cd ecommerce/ecom-catalog
mvn spring-boot:run
# Runs on port 8084
```

### Terminal 8: Checkout Service
```bash
cd ecommerce/ecom-checkout
mvn spring-boot:run
# Runs on port 8088
```

### Terminal 9: Order Service
```bash
cd ecommerce/ecom-order
mvn spring-boot:run
# Runs on port 8090
```

## Step 5: Test End-to-End Flow

### Test 1: User Registration & Login

```bash
# 1. Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!@#",
    "role": "CUSTOMER"
  }'

# Response should include accessToken and refreshToken
# Save the accessToken for next steps
```

### Test 2: Create User Profile

```bash
# Use the token from registration
TOKEN="your-access-token-here"

curl -X POST http://localhost:8080/api/v1/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1234567890"
  }'
```

### Test 3: Add Address

```bash
curl -X POST http://localhost:8080/api/v1/address \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA"
  }'

# Save the addressId from response
```

### Test 4: Browse Products (Catalog)

```bash
# Get products
curl http://localhost:8080/api/v1/ecommerce/catalog/products \
  -H "Authorization: Bearer $TOKEN"
```

### Test 5: Add to Cart

```bash
# Add product to cart
curl -X POST http://localhost:8080/api/v1/ecommerce/cart/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "skuId": "product-sku-id",
    "quantity": 2
  }'
```

### Test 6: Checkout

```bash
# Initiate checkout
curl -X POST http://localhost:8080/api/v1/ecommerce/checkout/initiate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "shippingAddressId": "address-id-from-step-3"
  }'
```

### Test 7: Complete Payment

```bash
# Create payment order
curl -X POST http://localhost:8080/api/v1/payment/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "amount": 100.00,
    "currency": "USD"
  }'
```

## Step 6: Test with Namaste Fab Frontend

### Start Frontend

```bash
cd ecommerce/frontend
pnpm install
pnpm dev
# Frontend runs on http://localhost:3000 (or configured port)
```

### Test Flow in Browser

1. **Open namaste-fab website:**
   - Navigate to: `http://localhost:3000` (or your frontend URL)

2. **Register/Login:**
   - Register a new user or login
   - Verify JWT token is received

3. **Browse Products:**
   - View product catalog
   - Verify products load correctly

4. **Add to Cart:**
   - Add items to cart
   - Verify cart updates

5. **Checkout:**
   - Proceed to checkout
   - Select address
   - Verify checkout flow works

6. **Payment:**
   - Complete payment
   - Verify order is created

## Verification Checklist

- [ ] All shared services start successfully
- [ ] All ecommerce services start successfully
- [ ] Gateway routes requests correctly
- [ ] User registration works
- [ ] User profile creation works
- [ ] Address management works
- [ ] Cart operations work
- [ ] Checkout flow works
- [ ] Payment processing works
- [ ] Frontend connects to backend
- [ ] End-to-end user flow completes successfully

## Troubleshooting

### Service Not Starting

1. **Check ports:**
   ```bash
   lsof -i :8081  # Check if port is in use
   ```

2. **Check database connection:**
   - Verify PostgreSQL is running
   - Check connection string in `application.yml`

3. **Check logs:**
   ```bash
   # Check service logs for errors
   tail -f logs/application.log
   ```

### Services Can't Communicate

1. **Verify service URLs:**
   - Check `application.yml` in each service
   - Ensure URLs match running services

2. **Check Gateway routes:**
   - Verify routes in `ecom-config-repo/gateway/application.yml`
   - Ensure routes point to correct services

3. **Test direct service calls:**
   ```bash
   # Bypass gateway and test service directly
   curl http://localhost:8081/api/v1/auth/register
   ```

### Maven Dependency Issues

1. **Clear Maven cache:**
   ```bash
   rm -rf ~/.m2/repository/com/ecom/shared-*
   mvn clean install
   ```

2. **Verify GitHub Packages access:**
   - Check `~/.m2/settings.xml` has correct token
   - Verify token has `read:packages` permission

## Quick Test Script

Save this as `test-e2e.sh`:

```bash
#!/bin/bash

echo "Testing Shared Services E2E..."

# Test IAM
echo "1. Testing IAM Service..."
curl -s http://localhost:8081/actuator/health | grep -q "UP" && echo "✅ IAM OK" || echo "❌ IAM Failed"

# Test User Profile
echo "2. Testing User Profile Service..."
curl -s http://localhost:8082/actuator/health | grep -q "UP" && echo "✅ User Profile OK" || echo "❌ User Profile Failed"

# Test Address
echo "3. Testing Address Service..."
curl -s http://localhost:8083/actuator/health | grep -q "UP" && echo "✅ Address OK" || echo "❌ Address Failed"

# Test Payment
echo "4. Testing Payment Service..."
curl -s http://localhost:8089/actuator/health | grep -q "UP" && echo "✅ Payment OK" || echo "❌ Payment Failed"

# Test Gateway
echo "5. Testing Gateway Service..."
curl -s http://localhost:8080/actuator/health | grep -q "UP" && echo "✅ Gateway OK" || echo "❌ Gateway Failed"

echo "E2E Test Complete!"
```

Make it executable:
```bash
chmod +x test-e2e.sh
./test-e2e.sh
```

## Next Steps

After successful testing:
1. Update ecommerce services to use published shared services
2. Remove old code from ecommerce folder
3. Update documentation
4. Deploy to production

