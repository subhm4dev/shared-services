package com.ecom.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Refund Entity
 */
@Entity
@Table(name = "payment_refunds", indexes = {
    @Index(name = "idx_refund_payment_id", columnList = "payment_id"),
    @Index(name = "idx_refund_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefund {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RefundStatus status = RefundStatus.PENDING;
    
    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;
    
    @Column(name = "gateway_refund_id", length = 200)
    private String gatewayRefundId;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum RefundStatus {
        PENDING,
        PROCESSING,
        SUCCESS,
        FAILED
    }
}

