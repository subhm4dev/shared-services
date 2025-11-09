package com.ecom.addressbook.service.impl;

import com.ecom.addressbook.entity.Address;
import com.ecom.addressbook.model.request.AddressRequest;
import com.ecom.addressbook.model.response.AddressResponse;
import com.ecom.addressbook.repository.AddressRepository;
import com.ecom.addressbook.service.AddressService;
import com.ecom.error.exception.BusinessException;
import com.ecom.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of AddressService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;

    @Override
    @Transactional
    public AddressResponse createAddress(
            UUID targetUserId,
            UUID tenantId,
            UUID currentUserId,
            List<String> roles,
            AddressRequest request) {
        
        log.debug("Creating address for user: {}, tenant: {}", targetUserId, tenantId);

        // 1. Authorization check: Users can only create addresses for themselves, admins/staff can create for any user
        if (!targetUserId.equals(currentUserId) && !hasAdminOrStaffRole(roles)) {
            log.warn("Unauthorized: User {} attempted to create address for user {}", currentUserId, targetUserId);
            throw new BusinessException(
                ErrorCode.UNAUTHORIZED,
                "You can only create addresses for yourself"
            );
        }

        // 2. Check for duplicate address (active addresses only)
        boolean duplicateExists = addressRepository.existsByUserIdAndTenantIdAndLine1AndCityAndPostcodeAndCountryAndDeletedFalse(
            targetUserId,
            tenantId,
            request.line1(),
            request.city(),
            request.postcode(),
            request.country()
        );

        if (duplicateExists) {
            log.warn("Duplicate address detected for user: {}, tenant: {}", targetUserId, tenantId);
            throw new BusinessException(
                ErrorCode.ADDRESS_DUPLICATE,
                "An identical address already exists for this user"
            );
        }

        // 3. If this is set as default, unset other default addresses for this user
        if (request.isDefault() != null && request.isDefault()) {
            List<Address> existingDefaults = addressRepository.findByUserIdAndTenantIdAndDeletedFalse(targetUserId, tenantId)
                .stream()
                .filter(Address::getIsDefault)
                .collect(Collectors.toList());
            
            for (Address existingDefault : existingDefaults) {
                existingDefault.setIsDefault(false);
                addressRepository.save(existingDefault);
            }
        }

        // 4. Create new address
        Address address = Address.builder()
            .userId(targetUserId)
            .tenantId(tenantId)
            .line1(request.line1())
            .line2(request.line2())
            .city(request.city())
            .state(request.state())
            .postcode(request.postcode())
            .country(request.country())
            .label(request.label())
            .isDefault(request.isDefault() != null ? request.isDefault() : false)
            .deleted(false)
            .build();

        Address savedAddress = addressRepository.save(address);
        log.info("Created address {} for user: {}, tenant: {}", savedAddress.getId(), targetUserId, tenantId);

        return toResponse(savedAddress);
    }

    @Override
    public AddressResponse getAddressById(
            UUID addressId,
            UUID currentUserId,
            UUID tenantId,
            List<String> roles,
            boolean includeDeleted) {
        
        log.debug("Getting address {} for user: {}, tenant: {}", addressId, currentUserId, tenantId);

        // 1. Find address (include deleted if requested and user is admin/staff)
        Address address;
        if (includeDeleted && hasAdminOrStaffRole(roles)) {
            address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(
                    ErrorCode.ADDRESS_NOT_FOUND,
                    "Address not found: " + addressId
                ));
        } else {
            address = addressRepository.findByIdAndDeletedFalse(addressId)
                .orElseThrow(() -> new BusinessException(
                    ErrorCode.ADDRESS_NOT_FOUND,
                    "Address not found: " + addressId
                ));
        }

        // 2. Verify tenant isolation
        if (!address.getTenantId().equals(tenantId)) {
            log.warn("Tenant mismatch: address tenant {}, user tenant {}", address.getTenantId(), tenantId);
            throw new BusinessException(
                ErrorCode.UNAUTHORIZED,
                "Address not found"
            );
        }

        // 3. Authorization check
        if (!canAccessAddress(currentUserId, address.getUserId(), roles)) {
            log.warn("Unauthorized: User {} attempted to access address {} owned by user {}", 
                currentUserId, addressId, address.getUserId());
            throw new BusinessException(
                ErrorCode.UNAUTHORIZED,
                "You do not have permission to access this address"
            );
        }

        return toResponse(address);
    }

    @Override
    public List<AddressResponse> getUserAddresses(
            UUID targetUserId,
            UUID tenantId,
            UUID currentUserId,
            List<String> roles,
            boolean includeDeleted) {
        
        log.debug("Getting addresses for user: {}, tenant: {}, includeDeleted: {}", targetUserId, tenantId, includeDeleted);

        // 1. Authorization check: Users can only view their own addresses, admins/staff can view any user's addresses
        if (!targetUserId.equals(currentUserId) && !hasAdminOrStaffRole(roles)) {
            log.warn("Unauthorized: User {} attempted to view addresses for user {}", currentUserId, targetUserId);
            throw new BusinessException(
                ErrorCode.UNAUTHORIZED,
                "You can only view your own addresses"
            );
        }

        // 2. Retrieve addresses
        List<Address> addresses;
        if (includeDeleted && hasAdminOrStaffRole(roles)) {
            addresses = addressRepository.findByUserIdAndTenantId(targetUserId, tenantId);
        } else {
            addresses = addressRepository.findByUserIdAndTenantIdAndDeletedFalse(targetUserId, tenantId);
        }

        return addresses.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(
            UUID addressId,
            UUID currentUserId,
            UUID tenantId,
            List<String> roles,
            AddressRequest request) {
        
        log.debug("Updating address {} for user: {}, tenant: {}", addressId, currentUserId, tenantId);

        // 1. Find address (active only for regular users)
        Address address = addressRepository.findByIdAndDeletedFalse(addressId)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.ADDRESS_NOT_FOUND,
                "Address not found: " + addressId
            ));

        // 2. Verify tenant isolation
        if (!address.getTenantId().equals(tenantId)) {
            log.warn("Tenant mismatch: address tenant {}, user tenant {}", address.getTenantId(), tenantId);
            throw new BusinessException(
                ErrorCode.UNAUTHORIZED,
                "Address not found"
            );
        }

        // 3. Authorization check
        if (!canAccessAddress(currentUserId, address.getUserId(), roles)) {
            log.warn("Unauthorized: User {} attempted to update address {} owned by user {}", 
                currentUserId, addressId, address.getUserId());
            throw new BusinessException(
                ErrorCode.UNAUTHORIZED,
                "You do not have permission to update this address"
            );
        }

        // 4. Check for duplicate if address fields changed (excluding current address)
        if (hasAddressChanged(address, request)) {
            boolean duplicateExists = addressRepository.existsByUserIdAndTenantIdAndLine1AndCityAndPostcodeAndCountryAndDeletedFalse(
                address.getUserId(),
                address.getTenantId(),
                request.line1(),
                request.city(),
                request.postcode(),
                request.country()
            );

            if (duplicateExists) {
                log.warn("Duplicate address detected for user: {}, tenant: {}", address.getUserId(), address.getTenantId());
                throw new BusinessException(
                    ErrorCode.ADDRESS_DUPLICATE,
                    "An identical address already exists for this user"
                );
            }
        }

        // 5. If setting as default, unset other default addresses
        if (request.isDefault() != null && request.isDefault() && !address.getIsDefault()) {
            List<Address> existingDefaults = addressRepository.findByUserIdAndTenantIdAndDeletedFalse(address.getUserId(), address.getTenantId())
                .stream()
                .filter(addr -> !addr.getId().equals(addressId))
                .filter(Address::getIsDefault)
                .collect(Collectors.toList());
            
            for (Address existingDefault : existingDefaults) {
                existingDefault.setIsDefault(false);
                addressRepository.save(existingDefault);
            }
        }

        // 6. Update address fields
        address.setLine1(request.line1());
        if (request.line2() != null) {
            address.setLine2(request.line2());
        }
        address.setCity(request.city());
        if (request.state() != null) {
            address.setState(request.state());
        }
        address.setPostcode(request.postcode());
        address.setCountry(request.country());
        if (request.label() != null) {
            address.setLabel(request.label());
        }
        if (request.isDefault() != null) {
            address.setIsDefault(request.isDefault());
        }

        Address savedAddress = addressRepository.save(address);
        log.info("Updated address {} for user: {}", addressId, address.getUserId());

        return toResponse(savedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(
            UUID addressId,
            UUID currentUserId,
            UUID tenantId,
            List<String> roles) {
        
        log.debug("Deleting address {} for user: {}, tenant: {}", addressId, currentUserId, tenantId);

        // 1. Find address (active only)
        Address address = addressRepository.findByIdAndDeletedFalse(addressId)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.ADDRESS_NOT_FOUND,
                "Address not found: " + addressId
            ));

        // 2. Verify tenant isolation
        if (!address.getTenantId().equals(tenantId)) {
            log.warn("Tenant mismatch: address tenant {}, user tenant {}", address.getTenantId(), tenantId);
            throw new BusinessException(
                ErrorCode.UNAUTHORIZED,
                "Address not found"
            );
        }

        // 3. Authorization check
        if (!canAccessAddress(currentUserId, address.getUserId(), roles)) {
            log.warn("Unauthorized: User {} attempted to delete address {} owned by user {}", 
                currentUserId, addressId, address.getUserId());
            throw new BusinessException(
                ErrorCode.UNAUTHORIZED,
                "You do not have permission to delete this address"
            );
        }

        // 4. Soft delete
        address.setDeleted(true);
        address.setDeletedAt(LocalDateTime.now());
        addressRepository.save(address);
        
        log.info("Soft deleted address {} for user: {}", addressId, address.getUserId());
    }

    @Override
    public boolean canAccessAddress(UUID currentUserId, UUID addressUserId, List<String> roles) {
        // Users can always access their own addresses
        if (currentUserId.equals(addressUserId)) {
            return true;
        }

        // Admins and Staff can access any address
        if (hasAdminOrStaffRole(roles)) {
            log.debug("Admin/Staff access granted for address owned by user: {}", addressUserId);
            return true;
        }

        // Default: deny access
        log.warn("Access denied: user {} attempted to access address owned by user {}", currentUserId, addressUserId);
        return false;
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

    /**
     * Check if address fields have changed (used for duplicate detection)
     */
    private boolean hasAddressChanged(Address address, AddressRequest request) {
        return !address.getLine1().equals(request.line1()) ||
               !address.getCity().equals(request.city()) ||
               !address.getPostcode().equals(request.postcode()) ||
               !address.getCountry().equals(request.country());
    }

    /**
     * Convert Address entity to AddressResponse DTO
     */
    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
            address.getId(),
            address.getUserId(),
            address.getTenantId(),
            address.getLine1(),
            address.getLine2(),
            address.getCity(),
            address.getState(),
            address.getPostcode(),
            address.getCountry(),
            address.getLabel(),
            address.getIsDefault(),
            address.getDeleted(),
            address.getDeletedAt(),
            address.getCreatedAt(),
            address.getUpdatedAt()
        );
    }
}

