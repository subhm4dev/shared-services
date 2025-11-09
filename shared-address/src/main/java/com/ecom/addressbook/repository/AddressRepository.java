package com.ecom.addressbook.repository;

import com.ecom.addressbook.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Address entity
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
    
    /**
     * Find all active (non-deleted) addresses for a user within a tenant
     * 
     * @param userId User ID
     * @param tenantId Tenant ID
     * @return List of active addresses
     */
    List<Address> findByUserIdAndTenantIdAndDeletedFalse(UUID userId, UUID tenantId);
    
    /**
     * Find all addresses for a user within a tenant (including deleted)
     * Used by admins/staff for audit purposes
     * 
     * @param userId User ID
     * @param tenantId Tenant ID
     * @return List of all addresses (active and deleted)
     */
    List<Address> findByUserIdAndTenantId(UUID userId, UUID tenantId);
    
    /**
     * Find active address by ID
     * 
     * @param id Address ID
     * @return Optional Address (only if not deleted)
     */
    Optional<Address> findByIdAndDeletedFalse(UUID id);
    
    /**
     * Find address by ID (including deleted)
     * Used by admins/staff for recovery/audit
     * 
     * @param id Address ID
     * @return Optional Address (may be deleted)
     */
    @Override
    @NonNull
    Optional<Address> findById(@NonNull UUID id);
    
    /**
     * Check if a duplicate active address exists for a user
     * Duplicate is defined as same user_id, tenant_id, line1, city, postcode, and country
     * 
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param line1 First line of address
     * @param city City name
     * @param postcode Postal code
     * @param country Country code
     * @return true if duplicate exists (active address only)
     */
    boolean existsByUserIdAndTenantIdAndLine1AndCityAndPostcodeAndCountryAndDeletedFalse(
        UUID userId,
        UUID tenantId,
        String line1,
        String city,
        String postcode,
        String country
    );
}

