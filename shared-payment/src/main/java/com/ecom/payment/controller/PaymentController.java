package com.ecom.payment.controller;

import com.ecom.payment.model.request.CreateOrderRequest;
import com.ecom.payment.model.request.ProcessPaymentRequest;
import com.ecom.payment.model.request.RefundPaymentRequest;
import com.ecom.payment.model.request.SavePaymentMethodRequest;
import com.ecom.payment.model.response.CreateOrderResponse;
import com.ecom.payment.model.response.PaymentMethodResponse;
import com.ecom.payment.model.response.PaymentResponse;
import com.ecom.payment.security.JwtAuthenticationToken;
import com.ecom.payment.service.PaymentService;
import com.ecom.response.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Payment Controller
 * 
 * <p>This controller manages payment processing, payment methods, and payment history.
 * It integrates with external payment gateways (Stripe, PayPal, etc.) to process
 * transactions securely.
 * 
 * <p>Why we need these APIs:
 * <ul>
 *   <li><b>Payment Processing:</b> Handles actual transaction processing through
 *       payment gateways. Essential for completing purchases and receiving revenue.</li>
 *   <li><b>Payment Method Management:</b> Allows users to save payment methods
 *       (credit cards, digital wallets) for faster checkout. Stored securely using
 *       tokenization.</li>
 *   <li><b>Payment Security:</b> Implements PCI-DSS compliance through tokenization.
 *       Actual card details never stored; only tokens are saved.</li>
 *   <li><b>Refund Processing:</b> Handles refunds for cancelled orders or returns.
 *       Integrates with payment gateways to process refunds.</li>
 *   <li><b>Payment History:</b> Provides transaction history for users and admins,
 *       enabling order tracking and financial reporting.</li>
 * </ul>
 * 
 * <p>All sensitive payment data is tokenized and stored securely. Payment gateway
 * tokens are encrypted at rest. This service never stores raw card numbers.
 */
@RestController
@RequestMapping("/api/v1/payment")
@Tag(name = "Payment", description = "Payment processing and payment method management endpoints")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;

    /**
     * Create Razorpay order for client-side checkout
     * 
     * <p>Creates a Razorpay order without processing payment. Used for client-side
     * Razorpay checkout modal integration. Returns a Razorpay order_id that can
     * be used to open the Razorpay checkout modal in the frontend.
     * 
     * <p>Flow:
     * <ul>
     *   <li>Creates order in Razorpay with amount and currency</li>
     *   <li>Returns Razorpay order_id</li>
     *   <li>Frontend uses order_id to open Razorpay checkout modal</li>
     *   <li>After payment, frontend calls /process with payment_id to verify</li>
     * </ul>
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @PostMapping("/order/create")
    @Operation(
        summary = "Create Razorpay order",
        description = "Creates a Razorpay order for client-side checkout. Returns order_id to open Razorpay checkout modal."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('SELLER')")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        
        // Debug: Log authentication details
        if (authentication != null) {
            log.debug("Authentication: principal={}, authorities={}", 
                authentication.getPrincipal(), 
                authentication.getAuthorities());
        } else {
            log.warn("Authentication is null!");
        }
        
        log.info("Creating Razorpay order: amount={}", request.amount());
        
        UUID userId = getUserIdFromAuthentication(authentication);
        UUID tenantId = getTenantIdFromAuthentication(authentication);
        
        CreateOrderResponse response = paymentService.createOrder(userId, tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Razorpay order created successfully"));
    }

    /**
     * Process payment
     * 
     * <p>Processes a payment transaction through the configured payment gateway.
     * Used by Checkout service to charge customers for orders.
     * 
     * <p>Payment flow:
     * <ul>
     *   <li>Validates payment method (saved token or new card)</li>
     *   <li>Charges amount through payment gateway</li>
     *   <li>Creates payment record in database</li>
     *   <li>Returns payment confirmation</li>
     * </ul>
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @PostMapping("/process")
    @Operation(
        summary = "Process payment",
        description = "Processes a payment transaction through payment gateway. Used by checkout service."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('SELLER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request,
            Authentication authentication) {
        
        log.info("Processing payment: amount={}", request.amount());
        
        UUID userId = getUserIdFromAuthentication(authentication);
        UUID tenantId = getTenantIdFromAuthentication(authentication);
        
        PaymentResponse response = paymentService.processPayment(userId, tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Payment processed successfully"));
    }

    /**
     * Refund payment
     * 
     * <p>Processes a refund for a completed payment. Used when orders are cancelled
     * or items are returned. Supports full or partial refunds.
     * 
     * <p>This endpoint is protected and requires authentication. Typically accessed
     * by Order service or Admin users.
     */
    @PostMapping("/refund")
    @Operation(
        summary = "Refund payment",
        description = "Processes a refund for a completed payment. Supports full or partial refunds."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @Valid @RequestBody RefundPaymentRequest request,
            Authentication authentication) {
        
        log.info("Processing refund: paymentId={}", request.paymentId());
        
        UUID userId = getUserIdFromAuthentication(authentication);
        UUID tenantId = getTenantIdFromAuthentication(authentication);
        List<String> roles = getRolesFromAuthentication(authentication);
        
        PaymentResponse response = paymentService.refundPayment(userId, tenantId, roles, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Refund processed successfully"));
    }

    /**
     * Save payment method
     * 
     * <p>Tokenizes and saves a payment method (credit card, digital wallet) for
     * future use. Card details are tokenized through payment gateway; only tokens
     * are stored in database.
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @PostMapping("/method")
    @Operation(
        summary = "Save payment method",
        description = "Tokenizes and saves a payment method for future use. Card details are never stored, only tokens."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> savePaymentMethod(
            @Valid @RequestBody SavePaymentMethodRequest request,
            Authentication authentication) {
        
        log.info("Saving payment method: type={}", request.type());
        
        UUID userId = getUserIdFromAuthentication(authentication);
        UUID tenantId = getTenantIdFromAuthentication(authentication);
        
        PaymentMethodResponse response = paymentService.savePaymentMethod(userId, tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Payment method saved successfully"));
    }

    /**
     * Get saved payment methods
     * 
     * <p>Returns all payment methods saved by the authenticated user. Used during
     * checkout to display saved payment options.
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @GetMapping("/method")
    @Operation(
        summary = "Get saved payment methods",
        description = "Returns all payment methods saved by the authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<PaymentMethodResponse>>> getPaymentMethods(
            Authentication authentication) {
        
        log.info("Getting payment methods");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        UUID tenantId = getTenantIdFromAuthentication(authentication);
        
        List<PaymentMethodResponse> response = paymentService.getPaymentMethods(userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment methods retrieved successfully"));
    }

    /**
     * Delete payment method
     * 
     * <p>Removes a saved payment method. User can only delete their own payment methods.
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @DeleteMapping("/method/{paymentMethodId}")
    @Operation(
        summary = "Delete payment method",
        description = "Removes a saved payment method. Users can only delete their own payment methods."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deletePaymentMethod(
            @PathVariable UUID paymentMethodId,
            Authentication authentication) {
        
        log.info("Deleting payment method: paymentMethodId={}", paymentMethodId);
        
        UUID userId = getUserIdFromAuthentication(authentication);
        UUID tenantId = getTenantIdFromAuthentication(authentication);
        
        paymentService.deletePaymentMethod(userId, tenantId, paymentMethodId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Get payment history
     * 
     * <p>Returns payment transaction history for the authenticated user. Includes
     * successful payments, refunds, and failed transactions.
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @GetMapping("/history")
    @Operation(
        summary = "Get payment history",
        description = "Returns payment transaction history for the authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPaymentHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        log.info("Getting payment history: page={}, size={}", page, size);
        
        UUID userId = getUserIdFromAuthentication(authentication);
        UUID tenantId = getTenantIdFromAuthentication(authentication);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentResponse> response = paymentService.getPaymentHistory(userId, tenantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment history retrieved successfully"));
    }

    /**
     * Get payment status
     * 
     * <p>Retrieves current status of a payment transaction. Used for polling payment
     * status during async payment processing or to verify payment completion.
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @GetMapping("/{paymentId}/status")
    @Operation(
        summary = "Get payment status",
        description = "Retrieves current status of a payment transaction"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER') or hasRole('STAFF') or hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(
            @PathVariable UUID paymentId,
            Authentication authentication) {
        
        log.info("Getting payment status: paymentId={}", paymentId);
        
        UUID userId = getUserIdFromAuthentication(authentication);
        UUID tenantId = getTenantIdFromAuthentication(authentication);
        List<String> roles = getRolesFromAuthentication(authentication);
        
        PaymentResponse response = paymentService.getPaymentStatus(userId, tenantId, roles, paymentId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment status retrieved successfully"));
    }
    
    /**
     * Webhook endpoint for payment gateway callbacks
     * 
     * <p>This endpoint receives webhook notifications from payment gateways
     * (Razorpay, PayU, etc.) to update payment status asynchronously.
     * 
     * <p>This endpoint is public (no JWT required) but verifies webhook signature.
     */
    @PostMapping("/webhook")
    @Operation(
        summary = "Payment webhook",
        description = "Receives webhook notifications from payment gateway. Verifies signature and updates payment status."
    )
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        
        log.info("Received payment webhook");
        
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Extract user ID from JWT authentication token
     */
    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            return UUID.fromString(jwtToken.getUserId());
        }
        throw new IllegalStateException("Invalid authentication token");
    }
    
    /**
     * Extract tenant ID from JWT authentication token
     */
    private UUID getTenantIdFromAuthentication(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            return UUID.fromString(jwtToken.getTenantId());
        }
        throw new IllegalStateException("Invalid authentication token");
    }
    
    /**
     * Extract roles from JWT authentication token
     */
    private List<String> getRolesFromAuthentication(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            return jwtToken.getRoles();
        }
        return java.util.Collections.emptyList();
    }
}

