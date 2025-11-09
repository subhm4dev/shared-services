# Address Book Service Testing Guide

This guide covers testing the Address Book service through manual testing, unit tests, and integration tests.

## Prerequisites

Before testing, ensure you have:

1. **Identity Service Running** (port 8081)
   - Provides JWT tokens and JWKS endpoint
   - Required for authentication

2. **PostgreSQL Database**
   ```bash
   # Create database
   createdb ecom_address_book
   # Or via Docker
   docker run -d --name postgres-address \
     -e POSTGRES_PASSWORD=postgres \
     -e POSTGRES_DB=ecom_address_book \
     -p 5432:5432 \
     postgres:15
   ```

3. **Redis Running** (for JWT blacklist)
   ```bash
   docker run -d --name redis \
     -p 6379:6379 \
     redis:7-alpine
   ```

4. **Start Address Book Service**
   ```bash
   cd ecom-address-book
   mvn spring-boot:run
   ```
   Service runs on port **8083**

## Manual Testing with cURL

### Step 1: Get JWT Token

First, register/login to get a JWT token from Identity Service:

```bash
# Register a customer
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "SecurePass123!",
    "tenantId": "550e8400-e29b-41d4-a716-446655440000"
  }'

# Login to get token
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "SecurePass123!",
    "tenantId": "550e8400-e29b-41d4-a716-446655440000"
  }'

# Response contains accessToken - save it as ACCESS_TOKEN variable
export ACCESS_TOKEN="<your-access-token>"
```

### Step 2: Test Address Endpoints

#### 1. Create Address (Customer)

```bash
curl -X POST http://localhost:8083/api/v1/address \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "line1": "123 Main Street",
    "line2": "Apartment 4B",
    "city": "New York",
    "state": "NY",
    "postcode": "10001",
    "country": "US",
    "label": "Home",
    "isDefault": true
  }'
```

**Expected Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "uuid-here",
    "userId": "user-uuid",
    "tenantId": "tenant-uuid",
    "line1": "123 Main Street",
    "line2": "Apartment 4B",
    "city": "New York",
    "state": "NY",
    "postcode": "10001",
    "country": "US",
    "label": "Home",
    "isDefault": true,
    "deleted": false,
    "deletedAt": null,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  },
  "message": "Address created successfully"
}
```

#### 2. Create Another Address (Office)

```bash
curl -X POST http://localhost:8083/api/v1/address \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "line1": "456 Business Ave",
    "city": "New York",
    "state": "NY",
    "postcode": "10002",
    "country": "US",
    "label": "Office",
    "isDefault": false
  }'
```

#### 3. Get All Addresses

```bash
curl -X GET "http://localhost:8083/api/v1/address" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

#### 4. Get Address by ID

```bash
# Replace {addressId} with actual ID from create response
curl -X GET "http://localhost:8083/api/v1/address/{addressId}" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

#### 5. Update Address

```bash
curl -X PUT http://localhost:8083/api/v1/address/{addressId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "line1": "123 Main Street",
    "line2": "Suite 5C",
    "city": "New York",
    "state": "NY",
    "postcode": "10001",
    "country": "US",
    "label": "Home",
    "isDefault": true
  }'
```

#### 6. Delete Address (Soft Delete)

```bash
curl -X DELETE http://localhost:8083/api/v1/address/{addressId} \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Expected Response:** 204 No Content

### Step 3: Test Duplicate Prevention

Try creating the same address twice:

```bash
# First creation - should succeed
curl -X POST http://localhost:8083/api/v1/address \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "line1": "789 Duplicate St",
    "city": "Boston",
    "postcode": "02101",
    "country": "US"
  }'

# Second creation with same address - should fail with 409 Conflict
curl -X POST http://localhost:8083/api/v1/address \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "line1": "789 Duplicate St",
    "city": "Boston",
    "postcode": "02101",
    "country": "US"
  }'
```

**Expected Error Response (409 Conflict):**
```json
{
  "error": "ADDRESS_DUPLICATE",
  "message": "An identical address already exists for this user"
}
```

### Step 4: Test Admin/Staff Role Access

#### Login as Admin

```bash
# Register admin (or use existing admin account)
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "AdminPass123!",
    "tenantId": "550e8400-e29b-41d4-a716-446655440000",
    "roles": ["ADMIN"]
  }'

# Login as admin
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "AdminPass123!",
    "tenantId": "550e8400-e29b-41d4-a716-446655440000"
  }'

export ADMIN_TOKEN="<admin-access-token>"
```

#### Admin Creates Address for Customer

```bash
# Get customer user ID first (from customer registration)
export CUSTOMER_USER_ID="<customer-uuid>"

curl -X POST http://localhost:8083/api/v1/address \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "userId": "'$CUSTOMER_USER_ID'",
    "line1": "321 Admin Created St",
    "city": "Seattle",
    "state": "WA",
    "postcode": "98101",
    "country": "US",
    "label": "Warehouse"
  }'
```

#### Admin Views Customer Addresses (Including Deleted)

```bash
curl -X GET "http://localhost:8083/api/v1/address?userId=$CUSTOMER_USER_ID&includeDeleted=true" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### Admin Updates Customer Address

```bash
curl -X PUT http://localhost:8083/api/v1/address/{addressId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "line1": "321 Admin Updated St",
    "city": "Seattle",
    "state": "WA",
    "postcode": "98101",
    "country": "US"
  }'
```

### Step 5: Test Authorization (Unauthorized Access)

#### Customer Tries to Access Another Customer's Address

```bash
# Use customer token but try to access different user's address
curl -X GET "http://localhost:8083/api/v1/address?userId={other-user-id}" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Expected Error Response (401/403):**
```json
{
  "error": "UNAUTHORIZED",
  "message": "You can only view your own addresses"
}
```

## Testing with Postman

### Import Collection

1. Create a new collection in Postman
2. Add environment variables:
   - `base_url`: `http://localhost:8083`
   - `identity_url`: `http://localhost:8081`
   - `access_token`: (will be set after login)
   - `admin_token`: (will be set after admin login)
   - `customer_user_id`: (customer UUID)
   - `address_id`: (will be set after creating address)

### Request Examples

1. **Login** (Pre-request script sets token)
   - POST `{{identity_url}}/api/v1/auth/login`
   - Body: `{"email": "...", "password": "...", "tenantId": "..."}`
   - Tests: `pm.environment.set("access_token", pm.response.json().accessToken);`

2. **Create Address**
   - POST `{{base_url}}/api/v1/address`
   - Headers: `Authorization: Bearer {{access_token}}`
   - Body: JSON with address fields

3. **Get Addresses**
   - GET `{{base_url}}/api/v1/address`
   - Headers: `Authorization: Bearer {{access_token}}`

## Unit Testing

Create unit tests for the service layer:

```java
// src/test/java/com/ecom/addressbook/service/impl/AddressServiceImplTest.java
```

Example test structure:

```java
@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {
    
    @Mock
    private AddressRepository addressRepository;
    
    @InjectMocks
    private AddressServiceImpl addressService;
    
    @Test
    void createAddress_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        AddressRequest request = new AddressRequest(
            null, "123 Main St", null, "NYC", "NY", "10001", "US", "Home", true
        );
        
        // When
        when(addressRepository.existsByUserIdAndTenantIdAndLine1AndCityAndPostcodeAndCountryAndDeletedFalse(...))
            .thenReturn(false);
        when(addressRepository.save(any(Address.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Then
        AddressResponse response = addressService.createAddress(
            userId, tenantId, userId, List.of("CUSTOMER"), request
        );
        
        assertThat(response).isNotNull();
        assertThat(response.line1()).isEqualTo("123 Main St");
    }
    
    @Test
    void createAddress_DuplicateAddress_ThrowsException() {
        // Test duplicate prevention
    }
    
    @Test
    void createAddress_UnauthorizedUser_ThrowsException() {
        // Test authorization
    }
}
```

## Integration Testing

Create integration tests with Testcontainers:

```java
// src/test/java/com/ecom/addressbook/integration/AddressControllerIntegrationTest.java
```

Example:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AddressControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void createAddress_WithValidToken_ReturnsCreated() {
        // Setup test JWT token
        // Call endpoint
        // Assert response
    }
}
```

## Testing Scenarios Checklist

### Customer Role Tests
- [ ] Customer can create address for themselves
- [ ] Customer can create multiple addresses
- [ ] Customer cannot create address for another user
- [ ] Customer can view only their own addresses
- [ ] Customer can update only their own addresses
- [ ] Customer can delete only their own addresses
- [ ] Customer cannot view deleted addresses

### Seller Role Tests
- [ ] Seller can create multiple addresses for themselves
- [ ] Seller can manage only their own addresses
- [ ] Seller cannot access other users' addresses

### Admin Role Tests
- [ ] Admin can create address for any user
- [ ] Admin can view any user's addresses
- [ ] Admin can view deleted addresses (includeDeleted=true)
- [ ] Admin can update any user's address
- [ ] Admin can delete any user's address

### Staff Role Tests
- [ ] Staff has same permissions as Admin
- [ ] Staff can manage addresses for customer support

### Business Logic Tests
- [ ] Duplicate address prevention works
- [ ] Soft delete retains address data
- [ ] Default address handling (only one default per user)
- [ ] Tenant isolation enforced
- [ ] Address validation (required fields, country code format)

### Error Handling Tests
- [ ] 401 Unauthorized when no token provided
- [ ] 403 Forbidden when unauthorized access
- [ ] 404 Not Found when address doesn't exist
- [ ] 409 Conflict for duplicate addresses
- [ ] 400 Bad Request for validation errors

## Swagger UI Testing

Access Swagger UI for interactive testing:

```
http://localhost:8083/swagger-ui.html
```

1. Click "Authorize" button
2. Enter: `Bearer <your-token>`
3. Test endpoints directly from the UI

## Performance Testing

### Load Testing with Apache Bench

```bash
# Create 100 addresses
ab -n 100 -c 10 -p address.json -T application/json \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8083/api/v1/address

# Get addresses (1000 requests, 50 concurrent)
ab -n 1000 -c 50 -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8083/api/v1/address
```

## Debugging Tips

1. **Check Logs**
   ```bash
   tail -f logs/address-book.log
   ```

2. **Enable SQL Logging**
   ```yaml
   # application.yml
   spring:
     jpa:
       show-sql: true
       properties:
         hibernate:
           format_sql: true
   ```

3. **Verify JWT Token**
   - Decode at https://jwt.io
   - Check userId, tenantId, roles claims

4. **Check Database**
   ```sql
   SELECT * FROM addresses WHERE deleted = false;
   SELECT * FROM addresses WHERE deleted = true; -- Admin only
   ```

5. **Verify Redis**
   ```bash
   redis-cli
   KEYS jwt:blacklist:*
   ```

## Common Issues

1. **401 Unauthorized**
   - Check token is valid and not expired
   - Verify Identity Service is running
   - Check JWKS endpoint is accessible

2. **403 Forbidden**
   - Verify user has correct role
   - Check authorization logic in service

3. **409 Conflict (Duplicate)**
   - Verify duplicate check logic
   - Check partial unique index in database

4. **Connection Refused**
   - Verify PostgreSQL is running
   - Check database connection string
   - Verify Redis is running

