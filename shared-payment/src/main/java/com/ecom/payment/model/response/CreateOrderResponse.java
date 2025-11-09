package com.ecom.payment.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for Razorpay order creation
 * Contains the order_id needed to open Razorpay checkout modal
 */
public record CreateOrderResponse(
    @JsonProperty("order_id")
    UUID orderId,
    
    @JsonProperty("razorpay_order_id")
    String razorpayOrderId,
    
    BigDecimal amount,
    
    String currency,
    
    String status
) {}

