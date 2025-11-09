package com.ecom.payment.service.impl;

import com.ecom.error.exception.BusinessException;
import com.ecom.error.model.ErrorCode;
import com.ecom.payment.entity.Payment;
import com.ecom.payment.entity.PaymentMethod;
import com.ecom.payment.entity.PaymentRefund;
import com.ecom.payment.gateway.PaymentGateway;
import com.ecom.payment.gateway.dto.*;
import com.ecom.payment.model.request.ProcessPaymentRequest;
import com.ecom.payment.model.request.RefundPaymentRequest;
import com.ecom.payment.model.request.SavePaymentMethodRequest;
import com.ecom.payment.model.response.PaymentMethodResponse;
import com.ecom.payment.model.response.PaymentResponse;
import com.ecom.payment.repository.PaymentMethodRepository;
import com.ecom.payment.repository.PaymentRefundRepository;
import com.ecom.payment.repository.PaymentRepository;
import com.ecom.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payment Service Implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final PaymentGateway paymentGateway; // Injected via @Qualifier if multiple gateways
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String PAYMENT_PROCESSED_TOPIC = "payment-processed";
    private static final String PAYMENT_REFUNDED_TOPIC = "payment-refunded";
    
    @Override
    @Transactional
    public PaymentResponse processPayment(UUID userId, UUID tenantId, ProcessPaymentRequest request) {
        log.info("Processing payment: userId={}, amount={}", userId, request.amount());
        
        Payment.PaymentMethodType methodType = determineMethodType(request);
        String token = null;
        String upiId = null;
        
        // Get payment method if provided
        if (request.paymentMethodId() != null) {
            PaymentMethod paymentMethod = paymentMethodRepository
                .findByUserIdAndTenantIdAndId(userId, tenantId, request.paymentMethodId())
                .orElseThrow(() -> new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Payment method not found"
                ));
            token = paymentMethod.getToken();
            methodType = paymentMethod.getType();
            upiId = paymentMethod.getUpiId();
        } else if (request.paymentMethod() != null) {
            // Tokenize new payment method if provided
            PaymentMethodTokenizeRequest tokenizeRequest = new PaymentMethodTokenizeRequest(
                methodType,
                request.paymentMethod().cardNumber(),
                request.paymentMethod().expiryMonth(),
                request.paymentMethod().expiryYear(),
                request.paymentMethod().cvv(),
                request.paymentMethod().upiId(),
                request.phoneNumber()
            );
            PaymentMethodTokenizeResponse tokenizeResponse = paymentGateway.tokenizePaymentMethod(tokenizeRequest);
            token = tokenizeResponse.token();
        }
        
        com.ecom.payment.gateway.dto.PaymentResponse gatewayResponse;
        
        // Idempotency check: If paymentGatewayTransactionId is provided, check if payment already exists
        if (request.paymentGatewayTransactionId() != null && !request.paymentGatewayTransactionId().isEmpty()) {
            // Check if payment with this transaction ID already exists
            Optional<Payment> existingPayment = paymentRepository.findByGatewayTransactionId(
                request.paymentGatewayTransactionId()
            );
            
            if (existingPayment.isPresent()) {
                Payment payment = existingPayment.get();
                // Verify it belongs to the same user and tenant
                if (payment.getUserId().equals(userId) && payment.getTenantId().equals(tenantId)) {
                    log.info("Payment already exists for transaction ID: {}, returning existing payment: paymentId={}", 
                        request.paymentGatewayTransactionId(), payment.getId());
                    return toResponse(payment);
                } else {
                    log.warn("Payment with transaction ID {} exists but belongs to different user/tenant. userId={}, tenantId={}, existingUserId={}, existingTenantId={}", 
                        request.paymentGatewayTransactionId(), userId, tenantId, payment.getUserId(), payment.getTenantId());
                    throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "Payment transaction ID already used by another user"
                    );
                }
            }
            
            // Payment doesn't exist, verify the payment status with the gateway
            log.info("Verifying client-side payment: paymentGatewayTransactionId={}", request.paymentGatewayTransactionId());
            gatewayResponse = paymentGateway.getPaymentStatus(request.paymentGatewayTransactionId());
        } else {
            // Process payment through gateway (server-side)
        PaymentRequest gatewayRequest = new PaymentRequest(
            userId,
            tenantId,
            request.orderId(),
            request.paymentMethodId(),
            methodType,
            request.amount(),
            request.currency() != null ? request.currency() : "INR",
            token,
            upiId,
            request.phoneNumber(),
            "Order payment",
            null // callback URL
        );
        
            gatewayResponse = paymentGateway.processPayment(gatewayRequest);
        }
        
        // Create payment entity
        Payment payment = Payment.builder()
            .userId(userId)
            .tenantId(tenantId)
            .orderId(request.orderId())
            .paymentMethodId(request.paymentMethodId())
            .status(gatewayResponse.status())
            .methodType(methodType)
            .amount(request.amount())
            .currency(request.currency() != null ? request.currency() : "INR")
            .gatewayProvider(paymentGateway.getProviderName())
            .gatewayTransactionId(gatewayResponse.gatewayTransactionId())
            .gatewayPaymentId(gatewayResponse.gatewayPaymentId())
            .paymentLink(gatewayResponse.paymentLink())
            .failureReason(gatewayResponse.failureReason())
            .processedAt(gatewayResponse.status() == Payment.PaymentStatus.SUCCESS 
                ? LocalDateTime.now() 
                : null)
            .createdAt(LocalDateTime.now())
            .build();
        
        Payment savedPayment = paymentRepository.save(payment);
        
        log.info("Payment processed: paymentId={}, status={}", savedPayment.getId(), savedPayment.getStatus());
        
        // Publish event
        try {
            kafkaTemplate.send(PAYMENT_PROCESSED_TOPIC, savedPayment.getId().toString(), savedPayment);
        } catch (Exception e) {
            log.error("Failed to publish payment processed event", e);
        }
        
        return toResponse(savedPayment);
    }
    
    @Override
    @Transactional
    public PaymentResponse refundPayment(UUID userId, UUID tenantId, List<String> userRoles, RefundPaymentRequest request) {
        log.info("Processing refund: paymentId={}, amount={}", request.paymentId(), request.amount());
        
        Payment payment = paymentRepository.findById(request.paymentId())
            .orElseThrow(() -> new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "Payment not found"
            ));
        
        // Check access
        boolean isAdmin = userRoles != null && (
            userRoles.contains("ADMIN") || 
            userRoles.contains("SELLER") ||
            userRoles.contains("STAFF")
        );
        
        if (!isAdmin && !payment.getUserId().equals(userId)) {
            throw new BusinessException(
                ErrorCode.ACCESS_DENIED,
                "Access denied to payment"
            );
        }
        
        // Verify tenant
        if (!payment.getTenantId().equals(tenantId)) {
            throw new BusinessException(
                ErrorCode.ACCESS_DENIED,
                "Payment belongs to different tenant"
            );
        }
        
        // Check if payment can be refunded
        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw new BusinessException(
                ErrorCode.INVALID_OPERATION,
                "Payment must be successful to refund. Current status: " + payment.getStatus()
            );
        }
        
        // Determine refund amount
        BigDecimal refundAmount = request.amount() != null 
            ? request.amount() 
            : payment.getAmount();
        
        // Check if refund amount is valid
        BigDecimal totalRefunded = payment.getRefunds().stream()
            .filter(r -> r.getStatus() == PaymentRefund.RefundStatus.SUCCESS)
            .map(PaymentRefund::getRefundAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (refundAmount.add(totalRefunded).compareTo(payment.getAmount()) > 0) {
            throw new BusinessException(
                ErrorCode.INVALID_OPERATION,
                "Refund amount exceeds payment amount"
            );
        }
        
        // Process refund through gateway
        RefundRequest refundRequest = new RefundRequest(
            payment.getGatewayTransactionId(),
            payment.getGatewayPaymentId(),
            refundAmount,
            request.reason()
        );
        
        RefundResponse refundResponse = paymentGateway.processRefund(refundRequest);
        
        // Create refund entity
        PaymentRefund refund = PaymentRefund.builder()
            .payment(payment)
            .status(refundResponse.status())
            .refundAmount(refundAmount)
            .gatewayRefundId(refundResponse.gatewayRefundId())
            .reason(request.reason())
            .failureReason(refundResponse.failureReason())
            .processedAt(refundResponse.status() == PaymentRefund.RefundStatus.SUCCESS 
                ? LocalDateTime.now() 
                : null)
            .createdAt(LocalDateTime.now())
            .build();
        
        paymentRefundRepository.save(refund);
        payment.getRefunds().add(refund);
        
        // Update payment status
        if (refundAmount.compareTo(payment.getAmount()) == 0) {
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(Payment.PaymentStatus.PARTIALLY_REFUNDED);
        }
        
        Payment savedPayment = paymentRepository.save(payment);
        
        log.info("Refund processed: refundId={}, status={}", refund.getId(), refund.getStatus());
        
        // Publish event
        try {
            kafkaTemplate.send(PAYMENT_REFUNDED_TOPIC, savedPayment.getId().toString(), refund);
        } catch (Exception e) {
            log.error("Failed to publish payment refunded event", e);
        }
        
        return toResponse(savedPayment);
    }
    
    @Override
    @Transactional
    public PaymentMethodResponse savePaymentMethod(UUID userId, UUID tenantId, SavePaymentMethodRequest request) {
        log.info("Saving payment method: userId={}, type={}", userId, request.type());
        
        Payment.PaymentMethodType methodType = Payment.PaymentMethodType.valueOf(request.type().toUpperCase());
        
        // Tokenize payment method
        PaymentMethodTokenizeRequest tokenizeRequest = new PaymentMethodTokenizeRequest(
            methodType,
            request.cardNumber(),
            request.expiryMonth(),
            request.expiryYear(),
            null, // CVV not stored
            request.upiId(),
            request.phoneNumber()
        );
        
        PaymentMethodTokenizeResponse tokenizeResponse = paymentGateway.tokenizePaymentMethod(tokenizeRequest);
        
        // If this is set as default, unset other defaults
        if (Boolean.TRUE.equals(request.isDefault())) {
            List<PaymentMethod> existingMethods = paymentMethodRepository.findByUserIdAndTenantId(userId, tenantId);
            existingMethods.forEach(m -> m.setIsDefault(false));
            paymentMethodRepository.saveAll(existingMethods);
        }
        
        // Create payment method entity
        PaymentMethod paymentMethod = PaymentMethod.builder()
            .userId(userId)
            .tenantId(tenantId)
            .type(methodType)
            .provider(paymentGateway.getProviderName())
            .token(tokenizeResponse.token())
            .maskedNumber(tokenizeResponse.maskedNumber())
            .upiId(request.upiId())
            .phoneNumber(request.phoneNumber())
            .cardType(tokenizeResponse.cardType())
            .expiryMonth(tokenizeResponse.expiryMonth() != null ? Integer.parseInt(tokenizeResponse.expiryMonth()) : null)
            .expiryYear(tokenizeResponse.expiryYear() != null ? Integer.parseInt(tokenizeResponse.expiryYear()) : null)
            .isDefault(Boolean.TRUE.equals(request.isDefault()))
            .createdAt(LocalDateTime.now())
            .build();
        
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        
        log.info("Payment method saved: paymentMethodId={}", saved.getId());
        
        return toPaymentMethodResponse(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getPaymentMethods(UUID userId, UUID tenantId) {
        log.debug("Getting payment methods: userId={}", userId);
        
        List<PaymentMethod> methods = paymentMethodRepository.findByUserIdAndTenantId(userId, tenantId);
        return methods.stream()
            .map(this::toPaymentMethodResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void deletePaymentMethod(UUID userId, UUID tenantId, UUID paymentMethodId) {
        log.info("Deleting payment method: paymentMethodId={}", paymentMethodId);
        
        PaymentMethod paymentMethod = paymentMethodRepository
            .findByUserIdAndTenantIdAndId(userId, tenantId, paymentMethodId)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "Payment method not found"
            ));
        
        paymentMethodRepository.delete(paymentMethod);
        
        log.info("Payment method deleted: paymentMethodId={}", paymentMethodId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentHistory(UUID userId, UUID tenantId, Pageable pageable) {
        log.debug("Getting payment history: userId={}", userId);
        
        Page<Payment> payments = paymentRepository.findByUserIdAndTenantId(userId, tenantId, pageable);
        return payments.map(this::toResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(UUID userId, UUID tenantId, List<String> userRoles, UUID paymentId) {
        log.debug("Getting payment status: paymentId={}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "Payment not found"
            ));
        
        // Check access
        boolean isAdmin = userRoles != null && (
            userRoles.contains("ADMIN") || 
            userRoles.contains("SELLER") ||
            userRoles.contains("STAFF")
        );
        
        if (!isAdmin && !payment.getUserId().equals(userId)) {
            throw new BusinessException(
                ErrorCode.ACCESS_DENIED,
                "Access denied to payment"
            );
        }
        
        // Verify tenant
        if (!payment.getTenantId().equals(tenantId)) {
            throw new BusinessException(
                ErrorCode.ACCESS_DENIED,
                "Payment belongs to different tenant"
            );
        }
        
        // Optionally sync with gateway if status is PENDING
        if (payment.getStatus() == Payment.PaymentStatus.PENDING && payment.getGatewayTransactionId() != null) {
            try {
                com.ecom.payment.gateway.dto.PaymentResponse gatewayResponse = paymentGateway.getPaymentStatus(payment.getGatewayTransactionId());
                if (gatewayResponse.status() != payment.getStatus()) {
                    payment.setStatus(gatewayResponse.status());
                    payment.setFailureReason(gatewayResponse.failureReason());
                    payment = paymentRepository.save(payment);
                }
            } catch (Exception e) {
                log.warn("Failed to sync payment status with gateway", e);
            }
        }
        
        return toResponse(payment);
    }
    
    @Override
    @Transactional
    public void handleWebhook(String payload, String signature) {
        log.info("Handling payment webhook");
        
        // Verify webhook signature
        if (!paymentGateway.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid webhook signature");
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Invalid webhook signature"
            );
        }
        
        // Parse webhook payload and update payment status
        // This is a simplified implementation - in production, you'd parse the actual webhook payload
        log.info("Webhook verified and processed");
    }
    
    @Override
    @Transactional
    public com.ecom.payment.model.response.CreateOrderResponse createOrder(UUID userId, UUID tenantId, com.ecom.payment.model.request.CreateOrderRequest request) {
        log.info("Creating Razorpay order for client-side checkout: userId={}, amount={}", userId, request.amount());
        
        // Determine payment method type
        Payment.PaymentMethodType methodType = request.paymentMethodType() != null 
            ? Payment.PaymentMethodType.valueOf(request.paymentMethodType().toUpperCase())
            : Payment.PaymentMethodType.CARD;
        
        // Create gateway payment request (for order creation only)
        PaymentRequest gatewayRequest = new PaymentRequest(
            userId,
            tenantId,
            request.orderId(),
            null, // No payment method ID for new orders
            methodType,
            request.amount(),
            request.currency() != null ? request.currency() : "INR",
            null, // No token needed for order creation
            null, // No UPI ID needed for order creation
            null, // No phone number needed
            "Order payment",
            null // No callback URL
        );
        
        // Create order in gateway (returns Razorpay order_id)
        String razorpayOrderId = paymentGateway.createOrder(gatewayRequest);
        
        log.info("Razorpay order created: razorpayOrderId={}", razorpayOrderId);
        
        return new com.ecom.payment.model.response.CreateOrderResponse(
            request.orderId(),
            razorpayOrderId,
            request.amount(),
            request.currency() != null ? request.currency() : "INR",
            "CREATED"
        );
    }
    
    /**
     * Determine payment method type from request
     */
    private Payment.PaymentMethodType determineMethodType(ProcessPaymentRequest request) {
        if (request.paymentMethod() != null && request.paymentMethod().type() != null) {
            return Payment.PaymentMethodType.valueOf(request.paymentMethod().type().toUpperCase());
        }
        return Payment.PaymentMethodType.CARD; // Default
    }
    
    /**
     * Convert Payment entity to PaymentResponse DTO
     */
    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getUserId(),
            payment.getTenantId(),
            payment.getOrderId(),
            payment.getPaymentMethodId(),
            payment.getStatus(),
            payment.getMethodType(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getGatewayProvider(),
            payment.getGatewayTransactionId(),
            payment.getGatewayPaymentId(),
            payment.getPaymentLink(),
            payment.getFailureReason(),
            payment.getRefunds().stream()
                .map(refund -> new PaymentResponse.PaymentRefundResponse(
                    refund.getId(),
                    PaymentResponse.PaymentRefundStatus.valueOf(refund.getStatus().name()),
                    refund.getRefundAmount(),
                    refund.getGatewayRefundId(),
                    refund.getReason(),
                    refund.getFailureReason(),
                    refund.getProcessedAt(),
                    refund.getCreatedAt()
                ))
                .collect(Collectors.toList()),
            payment.getProcessedAt(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
    
    /**
     * Convert PaymentMethod entity to PaymentMethodResponse DTO
     */
    private PaymentMethodResponse toPaymentMethodResponse(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(
            paymentMethod.getId(),
            paymentMethod.getType(),
            paymentMethod.getMaskedNumber(),
            paymentMethod.getUpiId(),
            paymentMethod.getPhoneNumber(),
            paymentMethod.getCardType(),
            paymentMethod.getExpiryMonth(),
            paymentMethod.getExpiryYear(),
            paymentMethod.getIsDefault(),
            paymentMethod.getCreatedAt()
        );
    }
}

