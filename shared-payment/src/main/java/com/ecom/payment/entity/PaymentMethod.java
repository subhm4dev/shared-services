package com.ecom.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Method Entity (Saved payment methods)
 */
@Entity
@Table(name = "payment_methods", indexes = {
    @Index(name = "idx_payment_method_user_id", columnList = "user_id"),
    @Index(name = "idx_payment_method_tenant_id", columnList = "tenant_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private Payment.PaymentMethodType type;
    
    @Column(name = "provider", nullable = false, length = 50)
    private String provider; // RAZORPAY, PAYU, etc.
    
    @Column(name = "token", nullable = false, length = 500)
    private String token; // Tokenized reference from gateway
    
    @Column(name = "masked_number", length = 50)
    private String maskedNumber; // Last 4 digits for cards
    
    @Column(name = "upi_id", length = 100)
    private String upiId; // For UPI
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber; // For wallets/UPI
    
    @Column(name = "card_type", length = 50)
    private String cardType; // VISA, MASTERCARD, etc.
    
    @Column(name = "expiry_month")
    private Integer expiryMonth;
    
    @Column(name = "expiry_year")
    private Integer expiryYear;
    
    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

