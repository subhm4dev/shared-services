package com.ecom.addressbook.controller;

import com.ecom.addressbook.model.request.AddressRequest;
import com.ecom.addressbook.model.response.AddressResponse;
import com.ecom.addressbook.security.JwtAuthenticationToken;
import com.ecom.addressbook.service.AddressService;
import com.ecom.error.exception.BusinessException;
import com.ecom.error.model.ErrorCode;
import com.ecom.response.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Address Book Controller
 * 
 * <p>This controller manages shipping addresses for users. Addresses are essential
 * for order fulfillment and delivery tracking. Each user can maintain multiple
 * addresses (home, office, etc.) for flexible delivery options.
 * 
 * <p>Why we need these APIs:
 * <ul>
 *   <li><b>Address Management:</b> Users need to save and manage multiple delivery
 *       addresses for convenience. Essential for checkout flow where users select
 *       delivery address.</li>
 *   <li><b>Order Fulfillment:</b> Checkout service retrieves addresses during order
 *       creation. Delivery service uses addresses for routing and tracking.</li>
 *   <li><b>Data Isolation:</b> Enforces tenant and user isolation - users can only
 *       access their own addresses, ensuring data privacy and security.</li>
 *   <li><b>Duplicate Prevention:</b> Prevents users from saving identical addresses
 *       multiple times, maintaining data quality and avoiding confusion.</li>
 * </ul>
 * 
 * <p>Addresses are tenant-scoped and user-scoped, ensuring proper multi-tenant
 * data isolation while supporting marketplace scenarios.
 * 
 * <p><b>Security:</b> JWT tokens are validated by JwtAuthenticationFilter.
 * User context comes from validated JWT claims (source of truth).
 * Gateway headers (X-User-Id, X-Roles) are hints only, not trusted for security.
 * 
 * <p><b>Role-Based Access Control:</b>
 * <ul>
 *   <li><b>CUSTOMER/SELLER:</b> Can manage only their own addresses</li>
 *   <li><b>ADMIN/STAFF:</b> Can manage any user's addresses (for support, onboarding, etc.)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/address")
@Tag(name = "Address Book", description = "Shipping address management endpoints")
@RequiredArgsConstructor
@Slf4j
public class AddressController {

    private final AddressService addressService;

    /**
     * Create a new shipping address
     * 
     * <p>This endpoint allows authenticated users to save a new shipping address.
     * The user ID is extracted from validated JWT claims (via JwtAuthenticationToken).
     * 
     * <p>Business rules:
     * <ul>
     *   <li>Validates address format and required fields</li>
     *   <li>Prevents duplicate addresses for the same user (exact match)</li>
     *   <li>Associates address with user's tenant for multi-tenant support</li>
     *   <li>If AddressRequest.userId is provided, must match authenticated user OR user must have ADMIN/STAFF role</li>
     * </ul>
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @PostMapping
    @Operation(
        summary = "Create a new shipping address",
        description = "Saves a new shipping address. Users can create for themselves, admins/staff can create for any user. Prevents duplicate addresses."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<AddressResponse> createAddress(
            @Valid @RequestBody AddressRequest addressRequest,
            Authentication authentication) {
        
        // Extract user context from validated JWT (source of truth)
        UUID currentUserId = getUserIdFromAuthentication(authentication);
        UUID tenantId = getTenantIdFromAuthentication(authentication);
        List<String> roles = getRolesFromAuthentication(authentication);
        
        // Determine target user ID
        UUID targetUserId = addressRequest.userId() != null ? addressRequest.userId() : currentUserId;
        
        // Authorization check: If userId in request is provided and doesn't match current user,
        // user must have ADMIN/STAFF role
        if (addressRequest.userId() != null && !addressRequest.userId().equals(currentUserId)) {
            if (!hasAdminOrStaffRole(roles)) {
                throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "You can only create addresses for yourself"
                );
            }
        }
        
        log.info("Creating address for user: {}, tenant: {}", targetUserId, tenantId);
        
        AddressResponse response = addressService.createAddress(
            targetUserId,
            tenantId,
            currentUserId,
            roles,
            addressRequest
        );
        
        return ApiResponse.success(response, "Address created successfully");
    }

    /**
     * Get address by ID
     * 
     * <p>Retrieves a specific address by its ID. Used during checkout when user
     * selects a saved address, or for order confirmation display.
     * 
     * <p>Access control:
     * <ul>
     *   <li>Users can only retrieve their own addresses</li>
     *   <li>Admins/Staff can retrieve any address</li>
     *   <li>Admins/Staff can view deleted addresses via ?includeDeleted=true query param</li>
     * </ul>
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @GetMapping("/{addressId}")
    @Operation(
        summary = "Get address by ID",
        description = "Retrieves a specific shipping address by its ID. Users can access own addresses, admins/staff can access any address. Supports includeDeleted query param for admins/staff."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<AddressResponse> getAddress(
            @PathVariable UUID addressId,
            @RequestParam(required = false, defaultValue = "false") boolean includeDeleted,
            Authentication authentication) {
        
        // Extract user context from validated JWT (source of truth)
        UUID currentUserId = getUserIdFromAuthentication(authentication);
        UUID tenantId = getTenantIdFromAuthentication(authentication);
        List<String> roles = getRolesFromAuthentication(authentication);
        
        // Only admins/staff can include deleted addresses
        if (includeDeleted && !hasAdminOrStaffRole(roles)) {
            includeDeleted = false;
        }
        
        log.info("Getting address {} for user: {}, tenant: {}, includeDeleted: {}", 
            addressId, currentUserId, tenantId, includeDeleted);
        
        AddressResponse response = addressService.getAddressById(
            addressId,
            currentUserId,
            tenantId,
            roles,
            includeDeleted
        );
        
        return ApiResponse.success(response, "Address retrieved successfully");
    }

    /**
     * Get all addresses for the authenticated user
     * 
     * <p>Returns all addresses saved by the current user. Used in checkout flow
     * to display address selection dropdown, or in user settings to manage addresses.
     * 
     * <p>Access control:
     * <ul>
     *   <li>If userId param is provided, must match authenticated user OR user must have ADMIN/STAFF role</li>
     *   <li>If no userId param, returns authenticated user's addresses (active only)</li>
     *   <li>Admins/Staff can view deleted addresses via ?includeDeleted=true query param</li>
     * </ul>
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @GetMapping
    @Operation(
        summary = "Get all addresses for user",
        description = "Retrieves all shipping addresses. Users can view own addresses, admins/staff can view any user's addresses. Supports includeDeleted query param for admins/staff."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<List<AddressResponse>> getUserAddresses(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false, defaultValue = "false") boolean includeDeleted,
            Authentication authentication) {
        
        // Extract user context from validated JWT (source of truth)
        UUID currentUserId = getUserIdFromAuthentication(authentication);
        UUID tenantId = getTenantIdFromAuthentication(authentication);
        List<String> roles = getRolesFromAuthentication(authentication);
        
        // Determine target user ID
        UUID targetUserId = userId != null ? userId : currentUserId;
        
        // Authorization check: If userId param is provided and doesn't match current user,
        // user must have ADMIN/STAFF role
        if (userId != null && !userId.equals(currentUserId)) {
            if (!hasAdminOrStaffRole(roles)) {
                throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "You can only view your own addresses"
                );
            }
        }
        
        // Only admins/staff can include deleted addresses
        if (includeDeleted && !hasAdminOrStaffRole(roles)) {
            includeDeleted = false;
        }
        
        log.info("Getting addresses for user: {}, tenant: {}, includeDeleted: {}", 
            targetUserId, tenantId, includeDeleted);
        
        List<AddressResponse> response = addressService.getUserAddresses(
            targetUserId,
            tenantId,
            currentUserId,
            roles,
            includeDeleted
        );
        
        return ApiResponse.success(response, "Addresses retrieved successfully");
    }

    /**
     * Update an existing address
     * 
     * <p>Allows users to modify saved addresses (e.g., correcting typos, updating
     * apartment numbers). 
     * 
     * <p>Access control:
     * <ul>
     *   <li>Users can only update their own addresses</li>
     *   <li>Admins/Staff can update any address</li>
     * </ul>
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @PutMapping("/{addressId}")
    @Operation(
        summary = "Update an existing address",
        description = "Updates a saved address. Users can modify own addresses, admins/staff can modify any address."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<AddressResponse> updateAddress(
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest addressRequest,
            Authentication authentication) {
        
        // Extract user context from validated JWT (source of truth)
        UUID currentUserId = getUserIdFromAuthentication(authentication);
        UUID tenantId = getTenantIdFromAuthentication(authentication);
        List<String> roles = getRolesFromAuthentication(authentication);
        
        log.info("Updating address {} for user: {}, tenant: {}", addressId, currentUserId, tenantId);
        
        AddressResponse response = addressService.updateAddress(
            addressId,
            currentUserId,
            tenantId,
            roles,
            addressRequest
        );
        
        return ApiResponse.success(response, "Address updated successfully");
    }

    /**
     * Delete an address
     * 
     * <p>Removes an address from the user's address book using soft delete
     * (sets deleted flag and deletedAt timestamp). Address is retained for audit trail.
     * 
     * <p>Access control:
     * <ul>
     *   <li>Users can only delete their own addresses</li>
     *   <li>Admins/Staff can delete any address</li>
     * </ul>
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @DeleteMapping("/{addressId}")
    @Operation(
        summary = "Delete an address",
        description = "Soft deletes an address. Users can delete own addresses, admins/staff can delete any address. Address is retained for audit trail."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable UUID addressId,
            Authentication authentication) {
        
        // Extract user context from validated JWT (source of truth)
        UUID currentUserId = getUserIdFromAuthentication(authentication);
        UUID tenantId = getTenantIdFromAuthentication(authentication);
        List<String> roles = getRolesFromAuthentication(authentication);
        
        log.info("Deleting address {} for user: {}, tenant: {}", addressId, currentUserId, tenantId);
        
        addressService.deleteAddress(
            addressId,
            currentUserId,
            tenantId,
            roles
        );
        
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Extract user ID from Spring Security Authentication
     * 
     * <p>The Authentication object is populated by JwtAuthenticationFilter
     * from validated JWT claims (source of truth).
     */
    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
            throw new BusinessException(
                ErrorCode.UNAUTHORIZED,
                "User ID is required. Please ensure you are authenticated."
            );
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        String userIdStr = jwtAuth.getUserId();
        
        try {
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format in JWT: {}", userIdStr);
            throw new BusinessException(
                ErrorCode.UNAUTHORIZED,
                "Invalid user ID format"
            );
        }
    }

    /**
     * Extract tenant ID from Spring Security Authentication
     */
    private UUID getTenantIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
            throw new BusinessException(
                ErrorCode.UNAUTHORIZED,
                "Tenant ID is required. Please ensure you are authenticated."
            );
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        String tenantIdStr = jwtAuth.getTenantId();
        
        try {
            return UUID.fromString(tenantIdStr);
        } catch (IllegalArgumentException e) {
            log.error("Invalid tenant ID format in JWT: {}", tenantIdStr);
            throw new BusinessException(
                ErrorCode.UNAUTHORIZED,
                "Invalid tenant ID format"
            );
        }
    }

    /**
     * Extract roles from Spring Security Authentication
     */
    private List<String> getRolesFromAuthentication(Authentication authentication) {
        if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
            return List.of();
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        return jwtAuth.getRoles();
    }

    /**
     * Check if user has ADMIN or STAFF role
     */
    private boolean hasAdminOrStaffRole(List<String> roles) {
        return roles != null && (
            roles.contains("ADMIN") || 
            roles.contains("STAFF")
        );
    }
}

