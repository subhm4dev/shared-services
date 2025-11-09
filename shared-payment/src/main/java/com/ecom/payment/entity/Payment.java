package com.ecom.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Payment Entity
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_user_id", columnList = "user_id"),
    @Index(name = "idx_payment_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_payment_order_id", columnList = "order_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_gateway_transaction_id", columnList = "gateway_transaction_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "order_id")
    private UUID orderId;
    
    @Column(name = "payment_method_id")
    private UUID paymentMethodId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", nullable = false)
    private PaymentMethodType methodType;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";
    
    @Column(name = "gateway_provider", nullable = false, length = 50)
    private String gatewayProvider; // RAZORPAY, PAYU, etc.
    
    @Column(name = "gateway_transaction_id", length = 200)
    private String gatewayTransactionId;
    
    @Column(name = "gateway_payment_id", length = 200)
    private String gatewayPaymentId;
    
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    
    @Column(name = "payment_link", length = 500)
    private String paymentLink;
    
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PaymentRefund> refunds = new ArrayList<>();
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        SUCCESS,
        FAILED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }
    
    public enum PaymentMethodType {
        CARD,
        UPI,
        WALLET,
        NET_BANKING,
        CASH_ON_DELIVERY
    }
}

