package com.ecom.profile.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka event published when user profile is created or updated
 * 
 * <p>This event is consumed by other services (e.g., orders, notifications)
 * to sync profile changes.
 */
public record ProfileUpdatedEvent(
    /**
     * Event type identifier
     */
    @JsonProperty("event_type")
    String eventType,

    /**
     * User ID whose profile was updated
     */
    @JsonProperty("user_id")
    UUID userId,

    /**
     * Updated full name
     */
    @JsonProperty("full_name")
    String fullName,

    /**
     * Updated phone number
     */
    @JsonProperty("phone")
    String phone,

    /**
     * Updated avatar URL
     */
    @JsonProperty("avatar_url")
    String avatarUrl,

    /**
     * Event timestamp
     */
    @JsonProperty("timestamp")
    LocalDateTime timestamp
) {
    /**
     * Factory method to create ProfileUpdatedEvent
     */
    public static ProfileUpdatedEvent of(UUID userId, String fullName, String phone, String avatarUrl) {
        return new ProfileUpdatedEvent(
            "ProfileUpdated",
            userId,
            fullName,
            phone,
            avatarUrl,
            LocalDateTime.now()
        );
    }
}

