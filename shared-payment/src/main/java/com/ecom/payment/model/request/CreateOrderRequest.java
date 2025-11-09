package com.ecom.payment.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating a Razorpay order (without processing payment)
 * Used for client-side Razorpay checkout flow
 */
public record CreateOrderRequest(
    @JsonProperty("order_id")
    UUID orderId,
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,
    
    String currency,
    
    @JsonProperty("payment_method_type")
    String paymentMethodType // CARD, UPI, etc.
) {}

