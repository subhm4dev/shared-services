package com.ecom.payment.repository;

import com.ecom.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment Method Repository
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    
    List<PaymentMethod> findByUserIdAndTenantId(UUID userId, UUID tenantId);
    
    Optional<PaymentMethod> findByUserIdAndTenantIdAndId(UUID userId, UUID tenantId, UUID id);
    
    Optional<PaymentMethod> findByToken(String token);
}

