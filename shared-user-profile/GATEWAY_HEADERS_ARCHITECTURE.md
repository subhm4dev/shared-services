# Gateway Headers: When Services Validate JWT

## Your Question
> If we implement Option A (services validate JWT), should we modify Gateway which adds tenant id, role and userId headers? Because if we validate and extract data in each service, what will be the need of setting these headers?

## Answer: **Headers Become Optional (Not Required for Security)**

---

## Two Architectural Approaches

### **Approach 1: Gateway Still Sets Headers (Recommended for Flexibility)**

**Gateway Behavior:**
```java
// Gateway validates JWT, extracts claims, then:
1. Forwards original JWT in Authorization header (REQUIRED)
2. Adds X-User-Id, X-Tenant-Id, X-Roles headers (OPTIONAL - for convenience)
```

**Service Behavior:**
```java
// Service validates JWT itself:
1. Extract JWT from Authorization header (source of truth)
2. Validate JWT (signature, expiry, blacklist)
3. Extract userId/tenantId/roles from validated JWT claims
4. Use headers ONLY for logging/observability (not for security)
```

**Why Keep Headers?**
- ✅ **Performance**: Services can use headers for fast path (if already validated)
- ✅ **Logging**: `tenant-context-starter` reads headers for structured logging
- ✅ **Convenience**: Services can access userId without parsing JWT (but still validate)
- ✅ **Backwards Compatibility**: Services that haven't migrated yet can use headers

**Security Model:**
- **JWT = Source of Truth** (validated by service)
- **Headers = Hints Only** (for performance/logging, NOT trusted for security)

---

### **Approach 2: Gateway Doesn't Set Headers (Cleaner, More Secure)**

**Gateway Behavior:**
```java
// Gateway validates JWT for routing, then:
1. Forwards original JWT in Authorization header (ONLY)
2. Does NOT add X-User-Id, X-Tenant-Id, X-Roles headers
```

**Service Behavior:**
```java
// Service validates JWT itself:
1. Extract JWT from Authorization header
2. Validate JWT
3. Extract userId/tenantId/roles from validated JWT claims
4. No headers to rely on - everything from JWT
```

**Why Remove Headers?**
- ✅ **Cleaner**: No redundancy, single source of truth (JWT)
- ✅ **More Secure**: No risk of header spoofing confusion
- ✅ **Simpler**: Less code, less complexity
- ✅ **Consistent**: All services validate JWT independently

**Trade-offs:**
- ❌ Services must parse JWT themselves (slight performance cost)
- ❌ `tenant-context-starter` won't work unless we parse JWT in filter

---

## Recommendation: **Approach 1 (Keep Headers as Hints)**

### Modified Gateway Filter:
```java
// Gateway validates JWT, then:
ServerHttpRequest modifiedRequest = request.mutate()
    .header("Authorization", "Bearer " + token)  // ✅ KEEP - source of truth
    .header("X-User-Id", userId)                // ✅ KEEP - for convenience/logging
    .header("X-Tenant-Id", tenantId)             // ✅ KEEP - for convenience/logging
    .header("X-Roles", joinRoles(roles))         // ✅ KEEP - for convenience/logging
    .build();
```

### Modified Service Security Filter:
```java
// Service validates JWT first (source of truth):
String jwt = extractJWT(request.getHeader("Authorization"));
JWTClaimsSet claims = validateJWT(jwt);  // ✅ Validate JWT

// Extract from validated JWT (security):
String userId = claims.getClaim("userId");
String tenantId = claims.getClaim("tenantId");
List<String> roles = claims.getClaim("roles");

// Headers are optional hints (for performance/logging):
String headerUserId = request.getHeader("X-User-Id");  // Optional
// If header matches JWT, use header for fast path (already validated)
// If header doesn't match JWT, ignore it (JWT is source of truth)
```

---

## Implementation Strategy

### Option A: **Keep Headers (Recommended)**

**Gateway:** Continues to set headers
**Services:** Validate JWT, use headers as hints

```java
// In service JWT filter:
JWTClaimsSet claims = validateJWT(jwt);  // Validate JWT (source of truth)
String jwtUserId = extractUserId(claims);

// Optional: Check if header matches JWT (for performance)
String headerUserId = request.getHeader("X-User-Id");
if (headerUserId != null && headerUserId.equals(jwtUserId)) {
    // Fast path: header matches JWT, already validated
    // Can use header for logging/context
} else {
    // Use JWT userId (security)
}
```

**Benefits:**
- Headers help with logging (`tenant-context-starter`)
- Performance optimization (avoid re-parsing if header matches)
- Backwards compatible
- Services still secure (validate JWT)

---

### Option B: **Remove Headers (Cleaner)**

**Gateway:** Only forwards JWT
**Services:** Validate JWT, extract everything from claims

**Changes Needed:**
1. Remove header setting in Gateway filter
2. Services validate JWT and extract claims
3. Update `tenant-context-starter` to read from JWT if needed

**Benefits:**
- Cleaner architecture
- No redundancy
- Single source of truth

---

## My Recommendation: **Hybrid Approach**

### Gateway:
```java
// Gateway validates JWT for routing decisions, then:
1. ✅ Forwards original JWT in Authorization header (REQUIRED)
2. ✅ Adds X-User-Id, X-Tenant-Id, X-Roles headers (OPTIONAL - for convenience)
```

### Service:
```java
// Service security model:
1. ✅ Extract JWT from Authorization header (PRIMARY - source of truth)
2. ✅ Validate JWT (signature, expiry, blacklist)
3. ✅ Extract userId/tenantId/roles from validated JWT claims (SECURITY)
4. ✅ Use headers for logging/observability only (CONVENIENCE - not trusted)
```

### Key Principle:
**JWT is always validated and is the source of truth. Headers are convenience hints for performance/logging, but security decisions come from validated JWT claims.**

---

## Updated Gateway Filter (Minimal Changes)

```java
// Gateway filter - keep headers for convenience
ServerHttpRequest modifiedRequest = request.mutate()
    .header("Authorization", "Bearer " + token)  // ✅ Keep JWT (services validate this)
    .header("X-User-Id", userId)                  // ✅ Keep header (hint for logging)
    .header("X-Tenant-Id", tenantId)             // ✅ Keep header (hint for logging)
    .header("X-Roles", joinRoles(roles))         // ✅ Keep header (hint for logging)
    .build();
```

**No changes needed to Gateway!** Services will:
1. Extract JWT from `Authorization` header
2. Validate JWT themselves
3. Trust JWT claims (not headers)
4. Use headers optionally for logging/performance

---

## Conclusion

**Answer to your question:**
- **Yes, we can keep headers** - they're useful for logging/performance
- **But services should NOT trust them** - validate JWT as source of truth
- **Gateway changes:** Minimal - just ensure JWT is forwarded
- **Service changes:** Add JWT validation, use headers as hints only

**Best Practice:**
Gateway validates JWT for routing → forwards JWT + adds headers as hints → Services validate JWT independently → use headers for convenience only

