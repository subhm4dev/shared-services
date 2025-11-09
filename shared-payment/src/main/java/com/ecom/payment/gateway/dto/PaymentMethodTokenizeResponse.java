package com.ecom.payment.gateway.dto;

/**
 * Payment Method Tokenize Response DTO
 */
public record PaymentMethodTokenizeResponse(
    String token,
    String maskedNumber, // Last 4 digits for cards
    String cardType, // VISA, MASTERCARD, etc.
    String expiryMonth,
    String expiryYear
) {}

