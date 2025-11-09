package com.ecom.profile.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating user profile
 */
public record ProfileRequest(
    /**
     * Full name of the user
     * Optional - user may update name later
     */
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    String fullName,

    /**
     * Phone number
     * Optional - may differ from authentication phone
     * Must be in E.164 format if provided
     */
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone must be in E.164 format (e.g., +919876543210)")
    String phone,

    /**
     * URL to user's avatar/profile picture
     * Optional - user may upload avatar later
     * Should be a valid HTTP/HTTPS URL
     */
    @Size(max = 2048, message = "Avatar URL must not exceed 2048 characters")
    @Pattern(regexp = "^(https?://)?[^\\s]+$", message = "Avatar URL must be a valid URL")
    String avatarUrl
) {
}

