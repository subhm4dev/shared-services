package com.ecom.payment.gateway.dto;

import com.ecom.payment.entity.Payment;

/**
 * Payment Method Tokenize Request DTO
 */
public record PaymentMethodTokenizeRequest(
    Payment.PaymentMethodType type,
    String cardNumber, // For cards
    String expiryMonth,
    String expiryYear,
    String cvv,
    String upiId, // For UPI
    String phoneNumber // For wallets/UPI
) {}

