package com.ecom.addressbook.service;

import com.ecom.addressbook.model.request.AddressRequest;
import com.ecom.addressbook.model.response.AddressResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for address operations
 */
public interface AddressService {
    
    /**
     * Create a new address
     * 
     * @param targetUserId User ID for whom the address is being created (may differ from currentUserId if admin/staff)
     * @param tenantId Tenant ID from JWT claims
     * @param currentUserId Currently authenticated user ID
     * @param roles Current user's roles
     * @param request Address request DTO
     * @return AddressResponse with created address data
     * @throws com.ecom.error.exception.BusinessException if duplicate address exists or unauthorized
     */
    AddressResponse createAddress(
        UUID targetUserId,
        UUID tenantId,
        UUID currentUserId,
        List<String> roles,
        AddressRequest request
    );
    
    /**
     * Get address by ID
     * 
     * @param addressId Address ID
     * @param currentUserId Currently authenticated user ID
     * @param tenantId Tenant ID from JWT claims
     * @param roles Current user's roles
     * @param includeDeleted Whether to include deleted addresses (only for admins/staff)
     * @return AddressResponse if found
     * @throws com.ecom.error.exception.BusinessException if address not found or unauthorized
     */
    AddressResponse getAddressById(
        UUID addressId,
        UUID currentUserId,
        UUID tenantId,
        List<String> roles,
        boolean includeDeleted
    );
    
    /**
     * Get all addresses for a user
     * 
     * @param targetUserId User ID whose addresses to retrieve (may differ from currentUserId if admin/staff)
     * @param tenantId Tenant ID from JWT claims
     * @param currentUserId Currently authenticated user ID
     * @param roles Current user's roles
     * @param includeDeleted Whether to include deleted addresses (only for admins/staff)
     * @return List of AddressResponse
     * @throws com.ecom.error.exception.BusinessException if unauthorized
     */
    List<AddressResponse> getUserAddresses(
        UUID targetUserId,
        UUID tenantId,
        UUID currentUserId,
        List<String> roles,
        boolean includeDeleted
    );
    
    /**
     * Update an existing address
     * 
     * @param addressId Address ID
     * @param currentUserId Currently authenticated user ID
     * @param tenantId Tenant ID from JWT claims
     * @param roles Current user's roles
     * @param request Address request DTO with updated fields
     * @return AddressResponse with updated address data
     * @throws com.ecom.error.exception.BusinessException if address not found or unauthorized
     */
    AddressResponse updateAddress(
        UUID addressId,
        UUID currentUserId,
        UUID tenantId,
        List<String> roles,
        AddressRequest request
    );
    
    /**
     * Soft delete an address
     * 
     * @param addressId Address ID
     * @param currentUserId Currently authenticated user ID
     * @param tenantId Tenant ID from JWT claims
     * @param roles Current user's roles
     * @throws com.ecom.error.exception.BusinessException if address not found or unauthorized
     */
    void deleteAddress(
        UUID addressId,
        UUID currentUserId,
        UUID tenantId,
        List<String> roles
    );
    
    /**
     * Check if user can access an address
     * Users can access their own addresses, admins/staff can access any address
     * 
     * @param currentUserId Currently authenticated user ID
     * @param addressUserId Address owner's user ID
     * @param roles Current user's roles
     * @return true if access is allowed
     */
    boolean canAccessAddress(UUID currentUserId, UUID addressUserId, List<String> roles);
}

