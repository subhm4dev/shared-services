package com.ecom.payment.model.response;

import com.ecom.payment.entity.Payment;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for payment method
 */
public record PaymentMethodResponse(
    UUID id,
    
    Payment.PaymentMethodType type,
    
    @JsonProperty("masked_number")
    String maskedNumber,
    
    @JsonProperty("upi_id")
    String upiId,
    
    @JsonProperty("phone_number")
    String phoneNumber,
    
    @JsonProperty("card_type")
    String cardType,
    
    @JsonProperty("expiry_month")
    Integer expiryMonth,
    
    @JsonProperty("expiry_year")
    Integer expiryYear,
    
    @JsonProperty("is_default")
    Boolean isDefault,
    
    @JsonProperty("created_at")
    LocalDateTime createdAt
) {}

