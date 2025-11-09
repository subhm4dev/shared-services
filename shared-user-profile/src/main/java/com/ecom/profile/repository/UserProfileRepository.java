package com.ecom.profile.repository;

import com.ecom.profile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserProfile entity
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    
    /**
     * Find profile by user ID
     * 
     * @param userId User ID from Identity service
     * @return Optional UserProfile
     */
    Optional<UserProfile> findByUserId(UUID userId);
    
    /**
     * Check if profile exists for user ID
     * 
     * @param userId User ID from Identity service
     * @return true if profile exists
     */
    boolean existsByUserId(UUID userId);
}

