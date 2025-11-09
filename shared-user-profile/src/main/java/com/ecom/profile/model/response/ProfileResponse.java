package com.ecom.profile.model.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user profile
 */
public record ProfileResponse(
    /**
     * Profile ID (internal)
     */
    UUID id,

    /**
     * User ID from Identity service
     */
    UUID userId,

    /**
     * Full name of the user
     */
    String fullName,

    /**
     * Phone number
     */
    String phone,

    /**
     * URL to user's avatar/profile picture
     */
    String avatarUrl,

    /**
     * Profile creation timestamp
     */
    LocalDateTime createdAt,

    /**
     * Profile last update timestamp
     */
    LocalDateTime updatedAt
) {
}

