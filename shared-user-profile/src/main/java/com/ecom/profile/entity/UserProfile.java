package com.ecom.profile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Profile Entity
 * 
 * <p>Stores user profile information separate from authentication credentials.
 * Linked to user_accounts via user_id (foreign key relationship).
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User ID from Identity service (user_accounts.id)
     * Unique constraint ensures one profile per user
     */
    @Column(nullable = false, unique = true, name = "user_id")
    private UUID userId;

    /**
     * Full name of the user (e.g., "John Doe")
     */
    @Column(name = "full_name")
    private String fullName;

    /**
     * Phone number (may differ from auth phone or be additional)
     * Optional - user may provide different contact number for orders
     */
    @Column
    private String phone;

    /**
     * URL to user's avatar/profile picture
     * Stored as URL (not binary) - actual image stored in object storage (S3, etc.)
     */
    @Column(name = "avatar_url")
    private String avatarUrl;

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}

