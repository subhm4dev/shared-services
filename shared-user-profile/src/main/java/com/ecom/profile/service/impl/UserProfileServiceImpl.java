package com.ecom.profile.service.impl;

import com.ecom.error.exception.BusinessException;
import com.ecom.error.model.ErrorCode;
import com.ecom.profile.entity.UserProfile;
import com.ecom.profile.model.event.ProfileUpdatedEvent;
import com.ecom.profile.model.request.ProfileRequest;
import com.ecom.profile.model.response.ProfileResponse;
import com.ecom.profile.repository.UserProfileRepository;
import com.ecom.profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of UserProfileService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final KafkaTemplate<String, ProfileUpdatedEvent> kafkaTemplate;

    /**
     * Kafka topic for profile update events
     */
    private static final String PROFILE_UPDATED_TOPIC = "profile-updated";

    @Override
    @Transactional
    public ProfileResponse createOrUpdateProfile(UUID userId, ProfileRequest request) {
        log.debug("Creating or updating profile for user: {}", userId);

        // 1. Check if profile exists and create/update accordingly
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElse(null);

        UserProfile savedProfile;
        
        if (profile == null) {
            // 2a. Create new profile
            UserProfile newProfile = UserProfile.builder()
                .userId(userId)
                .fullName(request.fullName())
                .phone(request.phone())
                .avatarUrl(request.avatarUrl())
                .build();
            log.info("Creating new profile for user: {}", userId);
            savedProfile = userProfileRepository.save(newProfile);
        } else {
            // 2b. Update existing profile (only non-null fields)
            if (request.fullName() != null) {
                profile.setFullName(request.fullName());
            }
            if (request.phone() != null) {
                profile.setPhone(request.phone());
            }
            if (request.avatarUrl() != null) {
                profile.setAvatarUrl(request.avatarUrl());
            }
            log.info("Updating existing profile for user: {}", userId);
            savedProfile = userProfileRepository.save(profile);
        }

        // 4. Publish ProfileUpdated event to Kafka
        try {
            ProfileUpdatedEvent event = ProfileUpdatedEvent.of(
                savedProfile.getUserId(),
                savedProfile.getFullName(),
                savedProfile.getPhone(),
                savedProfile.getAvatarUrl()
            );
            kafkaTemplate.send(PROFILE_UPDATED_TOPIC, userId.toString(), event);
            log.info("Published ProfileUpdated event for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish ProfileUpdated event for user: {}", userId, e);
            // Don't fail the request if Kafka publish fails - event can be retried
        }

        // 5. Return response
        return toResponse(savedProfile);
    }

    @Override
    public ProfileResponse getProfileByUserId(UUID userId) {
        log.debug("Getting profile for user: {}", userId);

        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.PROFILE_NOT_FOUND,
                "Profile not found for user: " + userId
            ));

        return toResponse(profile);
    }

    @Override
    public boolean canAccessProfile(UUID currentUserId, UUID targetUserId, List<String> userRoles) {
        // 1. Users can always view their own profile
        if (currentUserId.equals(targetUserId)) {
            return true;
        }

        // 2. Admins and Sellers can view any profile (for order management, etc.)
        boolean isAdmin = userRoles != null && (
            userRoles.contains("ADMIN") || 
            userRoles.contains("SELLER") ||
            userRoles.contains("STAFF")
        );

        if (isAdmin) {
            log.debug("Admin/Seller access granted for profile: {}", targetUserId);
            return true;
        }

        // 3. Default: deny access
        log.warn("Access denied: user {} attempted to access profile {}", currentUserId, targetUserId);
        return false;
    }

    /**
     * Convert UserProfile entity to ProfileResponse DTO
     */
    private ProfileResponse toResponse(UserProfile profile) {
        return new ProfileResponse(
            profile.getId(),
            profile.getUserId(),
            profile.getFullName(),
            profile.getPhone(),
            profile.getAvatarUrl(),
            profile.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }
}

