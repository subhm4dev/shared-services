package com.ecom.addressbook.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Address Entity
 * 
 * <p>Stores shipping addresses for users. Addresses are tenant-scoped and user-scoped,
 * ensuring proper multi-tenant data isolation while supporting marketplace scenarios.
 * 
 * <p>Soft delete support: All deletions are soft deletes (deleted flag + deletedAt timestamp)
 * for audit trail and data recovery purposes.
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Address {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User ID from Identity service (user_accounts.id)
     * Identifies the owner of this address
     */
    @Column(nullable = false, name = "user_id")
    private UUID userId;

    /**
     * Tenant ID for multi-tenant isolation
     * All queries are filtered by tenant_id
     */
    @Column(nullable = false, name = "tenant_id")
    private UUID tenantId;

    /**
     * First line of address (e.g., "123 Main Street")
     */
    @Column(nullable = false, name = "line1")
    private String line1;

    /**
     * Second line of address (e.g., "Apartment 4B", "Suite 100")
     * Optional field
     */
    @Column(name = "line2")
    private String line2;

    /**
     * City name
     */
    @Column(nullable = false)
    private String city;

    /**
     * State or province
     * Optional field
     */
    @Column
    private String state;

    /**
     * Postal or ZIP code
     */
    @Column(nullable = false)
    private String postcode;

    /**
     * Country code (ISO 3166-1 alpha-2, e.g., "US", "IN")
     */
    @Column(nullable = false)
    private String country;

    /**
     * Address label for identification (e.g., "Home", "Office", "Warehouse")
     * Optional field
     */
    @Column
    private String label;

    /**
     * Whether this is the default address for the user
     */
    @Column(nullable = false, name = "is_default")
    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean isDefault = false;
    
    /**
     * Getter for isDefault field
     * Lombok generates isDefault() for Boolean isDefault, but we need getIsDefault()
     */
    public Boolean getIsDefault() {
        return isDefault;
    }
    
    /**
     * Setter for isDefault field
     */
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    /**
     * Soft delete flag
     * When true, address is considered deleted but retained for audit trail
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    /**
     * Timestamp when address was soft deleted
     * Null if address is active
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}

