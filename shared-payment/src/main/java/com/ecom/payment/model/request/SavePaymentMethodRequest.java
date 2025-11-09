package com.ecom.payment.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for saving payment method
 */
public record SavePaymentMethodRequest(
    @NotNull(message = "Type is required")
    String type, // CARD, UPI, WALLET
    
    @JsonProperty("card_number")
    String cardNumber,
    
    @JsonProperty("expiry_month")
    String expiryMonth,
    
    @JsonProperty("expiry_year")
    String expiryYear,
    
    String cvv,
    
    @JsonProperty("upi_id")
    String upiId,
    
    @JsonProperty("phone_number")
    String phoneNumber,
    
    @JsonProperty("is_default")
    Boolean isDefault
) {}

