package com.ecom.payment.gateway.dto;

import com.ecom.payment.entity.PaymentRefund;

/**
 * Refund Response DTO from gateway
 */
public record RefundResponse(
    String gatewayRefundId,
    PaymentRefund.RefundStatus status,
    String failureReason
) {}

