package com.ecom.payment.repository;

import com.ecom.payment.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment Refund Repository
 */
@Repository
public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {
    
    List<PaymentRefund> findByPaymentId(UUID paymentId);
    
    Optional<PaymentRefund> findByGatewayRefundId(String gatewayRefundId);
}

