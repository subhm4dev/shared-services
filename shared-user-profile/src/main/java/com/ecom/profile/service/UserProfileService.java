package com.ecom.profile.service;

import com.ecom.profile.model.request.ProfileRequest;
import com.ecom.profile.model.response.ProfileResponse;

import java.util.UUID;

/**
 * Service interface for user profile operations
 */
public interface UserProfileService {
    
    /**
     * Create or update user profile
     * 
     * @param userId User ID (extracted from JWT token via Gateway headers)
     * @param request Profile request DTO
     * @return ProfileResponse with updated profile data
     */
    ProfileResponse createOrUpdateProfile(UUID userId, ProfileRequest request);
    
    /**
     * Get user profile by user ID
     * 
     * @param userId User ID
     * @return ProfileResponse if found
     * @throws com.ecom.error.exception.BusinessException if profile not found
     */
    ProfileResponse getProfileByUserId(UUID userId);
    
    /**
     * Check if user can access another user's profile
     * Users can view their own profile, admins/sellers can view any profile
     * 
     * @param currentUserId Currently authenticated user ID
     * @param targetUserId Profile owner's user ID
     * @param userRoles Current user's roles
     * @return true if access is allowed
     */
    boolean canAccessProfile(UUID currentUserId, UUID targetUserId, java.util.List<String> userRoles);
}

