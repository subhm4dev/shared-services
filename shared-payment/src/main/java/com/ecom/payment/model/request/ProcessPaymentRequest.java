package com.ecom.payment.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for processing payment
 */
public record ProcessPaymentRequest(
    @JsonProperty("order_id")
    UUID orderId,
    
    @JsonProperty("payment_method_id")
    UUID paymentMethodId,
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,
    
    String currency,
    
    @JsonProperty("payment_method")
    PaymentMethodRequest paymentMethod,
    
    @JsonProperty("phone_number")
    String phoneNumber,
    
    @JsonProperty("payment_gateway_transaction_id")
    String paymentGatewayTransactionId // For verifying payments already processed client-side (e.g., Razorpay payment_id)
) {
    public record PaymentMethodRequest(
        String type, // CARD, UPI, WALLET, etc.
        
        @JsonProperty("card_number")
        String cardNumber,
        
        @JsonProperty("expiry_month")
        String expiryMonth,
        
        @JsonProperty("expiry_year")
        String expiryYear,
        
        String cvv,
        
        @JsonProperty("upi_id")
        String upiId
    ) {}
}

