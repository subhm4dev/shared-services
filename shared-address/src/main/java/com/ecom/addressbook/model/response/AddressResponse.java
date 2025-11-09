package com.ecom.addressbook.model.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for address
 */
public record AddressResponse(
    /**
     * Address ID (internal)
     */
    UUID id,

    /**
     * User ID from Identity service
     */
    UUID userId,

    /**
     * Tenant ID for multi-tenant isolation
     */
    UUID tenantId,

    /**
     * First line of address
     */
    String line1,

    /**
     * Second line of address (optional)
     */
    String line2,

    /**
     * City name
     */
    String city,

    /**
     * State or province (optional)
     */
    String state,

    /**
     * Postal or ZIP code
     */
    String postcode,

    /**
     * Country code (ISO 3166-1 alpha-2)
     */
    String country,

    /**
     * Address label (optional)
     */
    String label,

    /**
     * Whether this is the default address
     */
    Boolean isDefault,

    /**
     * Soft delete flag (for admin visibility)
     */
    Boolean deleted,

    /**
     * Timestamp when address was soft deleted (null if active)
     */
    LocalDateTime deletedAt,

    /**
     * Address creation timestamp
     */
    LocalDateTime createdAt,

    /**
     * Address last update timestamp
     */
    LocalDateTime updatedAt
) {
}

