package com.ecom.payment.service;

import com.ecom.payment.model.request.ProcessPaymentRequest;
import com.ecom.payment.model.request.RefundPaymentRequest;
import com.ecom.payment.model.request.SavePaymentMethodRequest;
import com.ecom.payment.model.response.PaymentMethodResponse;
import com.ecom.payment.model.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Payment Service Interface
 */
public interface PaymentService {
    
    /**
     * Process a payment
     */
    PaymentResponse processPayment(UUID userId, UUID tenantId, ProcessPaymentRequest request);
    
    /**
     * Refund a payment
     */
    PaymentResponse refundPayment(UUID userId, UUID tenantId, List<String> userRoles, RefundPaymentRequest request);
    
    /**
     * Save payment method
     */
    PaymentMethodResponse savePaymentMethod(UUID userId, UUID tenantId, SavePaymentMethodRequest request);
    
    /**
     * Get saved payment methods
     */
    List<PaymentMethodResponse> getPaymentMethods(UUID userId, UUID tenantId);
    
    /**
     * Delete payment method
     */
    void deletePaymentMethod(UUID userId, UUID tenantId, UUID paymentMethodId);
    
    /**
     * Get payment history
     */
    Page<PaymentResponse> getPaymentHistory(UUID userId, UUID tenantId, Pageable pageable);
    
    /**
     * Get payment status
     */
    PaymentResponse getPaymentStatus(UUID userId, UUID tenantId, List<String> userRoles, UUID paymentId);
    
    /**
     * Handle webhook from payment gateway
     */
    void handleWebhook(String payload, String signature);
    
    /**
     * Create a Razorpay order for client-side checkout
     * Returns order_id that can be used to open Razorpay checkout modal
     */
    com.ecom.payment.model.response.CreateOrderResponse createOrder(UUID userId, UUID tenantId, com.ecom.payment.model.request.CreateOrderRequest request);
}

