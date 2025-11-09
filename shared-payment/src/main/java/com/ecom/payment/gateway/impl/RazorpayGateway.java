package com.ecom.payment.gateway.impl;

import com.ecom.payment.entity.Payment;
import com.ecom.payment.entity.PaymentRefund;
import com.ecom.payment.gateway.PaymentGateway;
import com.ecom.payment.gateway.dto.*;
import com.razorpay.Order;
import com.razorpay.Refund;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Razorpay Payment Gateway Implementation
 * 
 * <p>Implements PaymentGateway interface for Razorpay integration.
 * Supports Cards, UPI, Wallets, Net Banking, and Payment Links.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RazorpayGateway implements PaymentGateway {
    
    @Value("${razorpay.key-id}")
    private String keyId;
    
    @Value("${razorpay.key-secret}")
    private String keySecret;
    
    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;
    
    private RazorpayClient razorpayClient;
    
    private RazorpayClient getClient() throws RazorpayException {
        if (razorpayClient == null) {
            razorpayClient = new RazorpayClient(keyId, keySecret);
        }
        return razorpayClient;
    }
    
    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        try {
            RazorpayClient client = getClient();
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.amount().multiply(java.math.BigDecimal.valueOf(100)).longValue()); // Convert to paise
            orderRequest.put("currency", request.currency());
            orderRequest.put("receipt", request.orderId() != null ? request.orderId().toString() : "receipt_" + System.currentTimeMillis());
            
            // Add payment method specific options
            if (request.methodType() == Payment.PaymentMethodType.UPI) {
                orderRequest.put("method", "upi");
                if (request.upiId() != null) {
                    orderRequest.put("upi_id", request.upiId());
                }
            } else if (request.methodType() == Payment.PaymentMethodType.CARD) {
                orderRequest.put("method", "card");
                if (request.token() != null) {
                    orderRequest.put("token", request.token());
                }
            }
            
            Order order = client.orders.create(orderRequest);
            
            String orderId = order.get("id");
            String status = order.get("status");
            
            log.info("Razorpay order created: orderId={}, status={}", orderId, status);
            
            // For UPI, return payment link
            String paymentLink = null;
            if (request.methodType() == Payment.PaymentMethodType.UPI && order.has("short_url")) {
                paymentLink = order.get("short_url");
            }
            
            Payment.PaymentStatus paymentStatus = mapRazorpayStatus(status);
            
            return new PaymentResponse(
                orderId,
                orderId, // Razorpay uses order ID as payment ID initially
                paymentStatus,
                paymentLink,
                null, // QR code not implemented in this version
                null
            );
            
        } catch (RazorpayException e) {
            log.error("Razorpay payment processing failed", e);
            return new PaymentResponse(
                null,
                null,
                Payment.PaymentStatus.FAILED,
                null,
                null,
                e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error processing payment", e);
            return new PaymentResponse(
                null,
                null,
                Payment.PaymentStatus.FAILED,
                null,
                null,
                "Payment processing failed: " + e.getMessage()
            );
        }
    }
    
    @Override
    public RefundResponse processRefund(RefundRequest request) {
        try {
            RazorpayClient client = getClient();
            
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("payment_id", request.gatewayPaymentId());
            refundRequest.put("amount", request.amount().multiply(java.math.BigDecimal.valueOf(100)).longValue()); // Convert to paise
            if (request.reason() != null) {
                refundRequest.put("notes", new JSONObject().put("reason", request.reason()));
            }
            
            Refund refund = client.refunds.create(refundRequest);
            
            String refundId = refund.get("id");
            String status = refund.get("status");
            
            log.info("Razorpay refund created: refundId={}, status={}", refundId, status);
            
            PaymentRefund.RefundStatus refundStatus = mapRazorpayRefundStatus(status);
            
            return new RefundResponse(
                refundId,
                refundStatus,
                null
            );
            
        } catch (RazorpayException e) {
            log.error("Razorpay refund processing failed", e);
            return new RefundResponse(
                null,
                PaymentRefund.RefundStatus.FAILED,
                e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error processing refund", e);
            return new RefundResponse(
                null,
                PaymentRefund.RefundStatus.FAILED,
                "Refund processing failed: " + e.getMessage()
            );
        }
    }
    
    @Override
    public PaymentMethodTokenizeResponse tokenizePaymentMethod(PaymentMethodTokenizeRequest request) {
        // Razorpay handles tokenization through their checkout flow
        // For cards, tokens are generated after successful payment
        // This is a simplified implementation
        try {
            // In a real implementation, you would use Razorpay's tokenization API
            // For now, return a placeholder
            log.warn("Tokenization not fully implemented for Razorpay");
            return new PaymentMethodTokenizeResponse(
                "token_" + System.currentTimeMillis(),
                request.cardNumber() != null && request.cardNumber().length() >= 4 
                    ? "****" + request.cardNumber().substring(request.cardNumber().length() - 4)
                    : null,
                null,
                request.expiryMonth(),
                request.expiryYear()
            );
        } catch (Exception e) {
            log.error("Tokenization failed", e);
            throw new RuntimeException("Tokenization failed", e);
        }
    }
    
    @Override
    public PaymentResponse getPaymentStatus(String gatewayTransactionId) {
        log.info("Fetching payment status from Razorpay: transactionId={}", gatewayTransactionId);
        
        try {
            // Use CompletableFuture with timeout to prevent hanging
            CompletableFuture<com.razorpay.Payment> future = CompletableFuture.supplyAsync(() -> {
                try {
                    RazorpayClient client = getClient();
                    log.debug("Calling Razorpay API to fetch payment: transactionId={}", gatewayTransactionId);
                    com.razorpay.Payment payment = client.payments.fetch(gatewayTransactionId);
                    log.debug("Received response from Razorpay: transactionId={}", gatewayTransactionId);
                    return payment;
                } catch (RazorpayException e) {
                    log.error("RazorpayException in async call: transactionId={}", gatewayTransactionId, e);
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    log.error("Unexpected error in async Razorpay call: transactionId={}", gatewayTransactionId, e);
                    throw new RuntimeException(e);
                }
            });
            
            // Wait max 20 seconds for Razorpay response
            com.razorpay.Payment payment = future.get(20, TimeUnit.SECONDS);
            
            String status = payment.get("status");
            String paymentId = payment.get("id");
            
            log.info("Payment status fetched successfully: transactionId={}, status={}, paymentId={}", 
                gatewayTransactionId, status, paymentId);
            
            Payment.PaymentStatus paymentStatus = mapRazorpayStatus(status);
            
            return new PaymentResponse(
                gatewayTransactionId,
                paymentId,
                paymentStatus,
                null,
                null,
                null
            );
            
        } catch (TimeoutException e) {
            log.error("Razorpay API call timed out after 20 seconds: transactionId={}", gatewayTransactionId);
            return new PaymentResponse(
                gatewayTransactionId,
                null,
                Payment.PaymentStatus.FAILED,
                null,
                null,
                "Payment verification timed out. Please check payment status manually."
            );
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RazorpayException) {
                log.error("Failed to fetch payment status from Razorpay: transactionId={}", gatewayTransactionId, cause);
                return new PaymentResponse(
                    gatewayTransactionId,
                    null,
                    Payment.PaymentStatus.FAILED,
                    null,
                    null,
                    cause.getMessage()
                );
            } else {
                log.error("Unexpected error fetching payment status: transactionId={}", gatewayTransactionId, cause);
                return new PaymentResponse(
                    gatewayTransactionId,
                    null,
                    Payment.PaymentStatus.FAILED,
                    null,
                    null,
                    "Payment verification failed: " + (cause != null ? cause.getMessage() : e.getMessage())
                );
            }
        } catch (Exception e) {
            log.error("Unexpected error in getPaymentStatus: transactionId={}", gatewayTransactionId, e);
            return new PaymentResponse(
                gatewayTransactionId,
                null,
                Payment.PaymentStatus.FAILED,
                null,
                null,
                "Payment verification failed: " + e.getMessage()
            );
        }
    }
    
    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            String expectedSignature = calculateSignature(payload, webhookSecret);
            return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }
    
    /**
     * Create Razorpay order (for client-side checkout)
     * Creates an order without processing payment - used for Razorpay checkout modal
     */
    @Override
    public String createOrder(PaymentRequest request) {
        try {
            RazorpayClient client = getClient();
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.amount().multiply(java.math.BigDecimal.valueOf(100)).longValue()); // Convert to paise
            orderRequest.put("currency", request.currency() != null ? request.currency() : "INR");
            orderRequest.put("receipt", request.orderId() != null ? request.orderId().toString() : "receipt_" + System.currentTimeMillis());
            
            // Don't specify payment method - let user choose in Razorpay modal
            // This allows client-side checkout to handle all payment methods
            
            Order order = client.orders.create(orderRequest);
            
            String razorpayOrderId = order.get("id");
            log.info("Razorpay order created for client-side checkout: orderId={}", razorpayOrderId);
            
            return razorpayOrderId;
            
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error creating Razorpay order", e);
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getProviderName() {
        return "RAZORPAY";
    }
    
    /**
     * Calculate HMAC SHA256 signature for webhook verification
     */
    private String calculateSignature(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
    
    /**
     * Map Razorpay status to PaymentStatus enum
     */
    private Payment.PaymentStatus mapRazorpayStatus(String razorpayStatus) {
        return switch (razorpayStatus.toUpperCase()) {
            case "CREATED", "AUTHORIZED" -> Payment.PaymentStatus.PENDING;
            case "CAPTURED" -> Payment.PaymentStatus.SUCCESS;
            case "FAILED" -> Payment.PaymentStatus.FAILED;
            case "REFUNDED" -> Payment.PaymentStatus.REFUNDED;
            default -> Payment.PaymentStatus.PROCESSING;
        };
    }
    
    /**
     * Map Razorpay refund status to RefundStatus enum
     */
    private PaymentRefund.RefundStatus mapRazorpayRefundStatus(String razorpayStatus) {
        return switch (razorpayStatus.toUpperCase()) {
            case "PENDING" -> PaymentRefund.RefundStatus.PENDING;
            case "PROCESSED" -> PaymentRefund.RefundStatus.SUCCESS;
            case "FAILED" -> PaymentRefund.RefundStatus.FAILED;
            default -> PaymentRefund.RefundStatus.PROCESSING;
        };
    }
}

