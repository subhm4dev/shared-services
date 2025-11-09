# Security Architecture for User Profile Service

## 🚨 Current Security Gaps (Your Concerns)

### Question 1: Can we access user profile APIs directly? Should they come from Gateway?

**Current State:**
- ✅ **Gateway validates JWT** and adds `X-User-Id`, `X-Roles` headers
- ❌ **Service trusts headers blindly** - doesn't validate JWT itself
- ❌ **Direct access is possible** - if someone bypasses Gateway, they can fake headers

**Risk:**
If the service is directly accessible (e.g., in dev/staging without firewall), an attacker could:
- Call the service directly with fake `X-User-Id` and `X-Roles` headers
- Access any user's profile without a valid JWT

---

### Question 2: If anyone can pass userId and roles in headers, isn't that a security issue?

**YES! This is a valid security concern.**

**Current Vulnerability:**
```bash
# Attacker can do this:
curl http://localhost:8082/api/v1/profile/{anyUserId} \
  -H "X-User-Id: attacker-user-id" \
  -H "X-Roles: ADMIN"
```

**Why it works:**
- Service doesn't validate the JWT token
- Service trusts headers without verification
- No signature/secret to verify headers came from Gateway

---

### Question 3: Why haven't we used @PreAuthorize or @Secured?

**Current Approach:**
- Manual authorization checks in service layer (`canAccessProfile()`)
- No Spring Security method-level security
- Headers are trusted without JWT validation

**Why @PreAuthorize wasn't used:**
1. Spring Security doesn't know about `X-User-Id` headers by default
2. We'd need a custom `Authentication` filter to extract headers
3. We'd still need to validate JWT to trust the headers

---

## ✅ Secure Architecture Options

### **Option 1: Service Validates JWT (Recommended for Microservices)**

Each service validates the JWT token itself:

```java
// In each service:
1. Extract JWT from Authorization header
2. Validate signature using JWKS
3. Check expiry
4. Check blacklist (Redis)
5. Extract userId, roles from token claims
6. Use @PreAuthorize for authorization
```

**Pros:**
- Services don't trust headers
- Each service validates independently
- Can use @PreAuthorize for clean authorization

**Cons:**
- JWT validation duplicated in each service
- Each service needs JWKS client
- Slightly slower (validation on each service)

---

### **Option 2: Gateway-Signed Headers (Current + Enhancement)**

Gateway signs headers with a shared secret:

```java
// Gateway:
String headerSignature = HMAC.sign("X-User-Id:" + userId, SHARED_SECRET);
request.header("X-User-Id", userId);
request.header("X-User-Id-Signature", headerSignature);

// Service:
String signature = request.getHeader("X-User-Id-Signature");
if (!HMAC.verify("X-User-Id:" + userId, signature, SHARED_SECRET)) {
    throw new SecurityException("Header signature invalid");
}
```

**Pros:**
- Services don't need JWT validation
- Fast (HMAC verification)
- Prevents header manipulation

**Cons:**
- Shared secret management
- Need to rotate secrets securely

---

### **Option 3: Service Mesh / mTLS (Production)**

Use service mesh (Istio, Linkerd) or mTLS:
- Services communicate via TLS
- Only Gateway can call services
- Headers are trusted within secure network

**Pros:**
- Network-level security
- No code changes needed
- Industry standard

**Cons:**
- Infrastructure complexity
- Requires Kubernetes/service mesh

---

### **Option 4: Hybrid (Gateway + Service Validation)**

Gateway validates and forwards JWT, service also validates:

```java
// Gateway: Validates JWT, adds headers + forwards original JWT
request.header("X-User-Id", userId);
request.header("Authorization", "Bearer " + originalJWT);

// Service: Validates JWT from Authorization header, ignores X-User-Id
String jwt = extractJWT(request);
JWTClaimsSet claims = validateJWT(jwt); // Use JWKS
String actualUserId = claims.getClaim("userId");
```

**Pros:**
- Defense in depth
- Gateway headers are hints, JWT is source of truth
- Works even if Gateway is compromised

**Cons:**
- Still duplicate validation
- More complex

---

## 🎯 Recommended Solution: **Option 1 (Service Validates JWT)**

For our microservices architecture, each service should validate JWT:

1. **Consistent security model** - each service is independently secure
2. **Works with @PreAuthorize** - can use Spring Security properly
3. **No shared secrets** - uses public key cryptography (JWKS)
4. **Defense in depth** - even if Gateway is compromised, services are protected

---

## Implementation Plan

1. **Add JWT Validation to User Profile Service**
   - Create `JwtValidationService` (similar to Gateway)
   - Create `JwtAuthenticationFilter` (Spring Security filter)
   - Extract JWT from `Authorization` header (not from `X-User-Id`)
   - Validate JWT, then trust the claims

2. **Use @PreAuthorize for Authorization**
   - Create custom `Authentication` from JWT claims
   - Use `@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")`

3. **Keep Gateway Headers as Hints Only**
   - Service validates JWT itself
   - Headers are for convenience (logging, tenant context)
   - Authorization comes from JWT, not headers

---

## Current vs Secure Flow

### ❌ Current (Insecure):
```
Client → Gateway (validates JWT) → Service (trusts headers blindly)
```

### ✅ Secure:
```
Client → Gateway (validates JWT, adds headers) 
      → Service (validates JWT again, uses @PreAuthorize)
```

Or even better:
```
Client → Gateway (forwards JWT) 
      → Service (validates JWT, extracts claims, uses @PreAuthorize)
```

