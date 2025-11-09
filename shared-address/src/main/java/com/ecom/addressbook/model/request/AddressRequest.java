package com.ecom.addressbook.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request DTO for creating or updating an address
 */
public record AddressRequest(
    /**
     * User ID (optional)
     * If provided, must match authenticated user OR user must have ADMIN/STAFF role
     * If null, address will be created for authenticated user
     */
    UUID userId,

    /**
     * First line of address (e.g., "123 Main Street")
     * Required field
     */
    @NotBlank(message = "Line1 is required")
    @Size(max = 255, message = "Line1 must not exceed 255 characters")
    String line1,

    /**
     * Second line of address (e.g., "Apartment 4B", "Suite 100")
     * Optional field
     */
    @Size(max = 255, message = "Line2 must not exceed 255 characters")
    String line2,

    /**
     * City name
     * Required field
     */
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    String city,

    /**
     * State or province
     * Optional field
     */
    @Size(max = 100, message = "State must not exceed 100 characters")
    String state,

    /**
     * Postal or ZIP code
     * Required field
     */
    @NotBlank(message = "Postcode is required")
    @Size(max = 20, message = "Postcode must not exceed 20 characters")
    String postcode,

    /**
     * Country code (ISO 3166-1 alpha-2, e.g., "US", "IN")
     * Required field
     */
    @NotBlank(message = "Country is required")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a valid ISO 3166-1 alpha-2 code (e.g., US, IN)")
    @Size(min = 2, max = 2, message = "Country must be exactly 2 characters")
    String country,

    /**
     * Address label for identification (e.g., "Home", "Office", "Warehouse")
     * Optional field
     */
    @Size(max = 50, message = "Label must not exceed 50 characters")
    String label,

    /**
     * Whether this should be set as the default address
     * Optional field, defaults to false
     */
    Boolean isDefault
) {
}

