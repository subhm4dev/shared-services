package com.ecom.payment.gateway.dto;

import java.math.BigDecimal;

/**
 * Refund Request DTO for gateway
 */
public record RefundRequest(
    String gatewayTransactionId,
    String gatewayPaymentId,
    BigDecimal amount,
    String reason
) {}

