package com.ecom.payment.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for refunding payment
 */
public record RefundPaymentRequest(
    @NotNull(message = "Payment ID is required")
    @JsonProperty("payment_id")
    UUID paymentId,
    
    @JsonProperty("amount")
    BigDecimal amount, // Optional, full refund if not specified
    
    @NotNull(message = "Reason is required")
    String reason
) {}

