package com.ecom.payment.repository;

import com.ecom.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Payment Repository
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
    
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);
    
    Page<Payment> findByUserIdAndTenantId(UUID userId, UUID tenantId, Pageable pageable);
    
    Page<Payment> findByOrderId(UUID orderId, Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.tenantId = :tenantId " +
           "AND (:status IS NULL OR p.status = :status)")
    Page<Payment> findByUserIdAndTenantIdAndStatus(
        @Param("userId") UUID userId,
        @Param("tenantId") UUID tenantId,
        @Param("status") Payment.PaymentStatus status,
        Pageable pageable
    );
}

