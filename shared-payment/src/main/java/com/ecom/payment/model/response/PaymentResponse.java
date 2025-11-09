package com.ecom.payment.model.response;

import com.ecom.payment.entity.Payment;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for payment details
 */
public record PaymentResponse(
    UUID id,
    
    @JsonProperty("user_id")
    UUID userId,
    
    @JsonProperty("tenant_id")
    UUID tenantId,
    
    @JsonProperty("order_id")
    UUID orderId,
    
    @JsonProperty("payment_method_id")
    UUID paymentMethodId,
    
    Payment.PaymentStatus status,
    
    @JsonProperty("method_type")
    Payment.PaymentMethodType methodType,
    
    BigDecimal amount,
    
    String currency,
    
    @JsonProperty("gateway_provider")
    String gatewayProvider,
    
    @JsonProperty("gateway_transaction_id")
    String gatewayTransactionId,
    
    @JsonProperty("gateway_payment_id")
    String gatewayPaymentId,
    
    @JsonProperty("payment_link")
    String paymentLink,
    
    @JsonProperty("failure_reason")
    String failureReason,
    
    List<PaymentRefundResponse> refunds,
    
    @JsonProperty("processed_at")
    LocalDateTime processedAt,
    
    @JsonProperty("created_at")
    LocalDateTime createdAt,
    
    @JsonProperty("updated_at")
    LocalDateTime updatedAt
) {
    public record PaymentRefundResponse(
        UUID id,
        
        PaymentRefundStatus status,
        
        @JsonProperty("refund_amount")
        BigDecimal refundAmount,
        
        @JsonProperty("gateway_refund_id")
        String gatewayRefundId,
        
        String reason,
        
        @JsonProperty("failure_reason")
        String failureReason,
        
        @JsonProperty("processed_at")
        LocalDateTime processedAt,
        
        @JsonProperty("created_at")
        LocalDateTime createdAt
    ) {}
    
    public enum PaymentRefundStatus {
        PENDING,
        PROCESSING,
        SUCCESS,
        FAILED
    }
}

