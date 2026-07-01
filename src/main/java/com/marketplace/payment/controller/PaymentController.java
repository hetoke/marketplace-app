package com.marketplace.payment.controller;

import com.marketplace.payment.dto.InitiatePaymentRequest;
import com.marketplace.payment.dto.PaymentCallbackRequest;
import com.marketplace.payment.dto.PaymentHistoryResponse;
import com.marketplace.payment.dto.PaymentResponse;
import com.marketplace.payment.dto.RefundPaymentRequest;
import com.marketplace.payment.dto.SePayCheckoutResponse;
import com.marketplace.payment.service.PaymentService;
import com.marketplace.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/v1/orders/{orderId}/pay")
    public ResponseEntity<ApiResponse<SePayCheckoutResponse>> initiatePayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody InitiatePaymentRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        SePayCheckoutResponse response = paymentService.initiatePayment(userId, orderId, request.paymentMethod());
        return ResponseEntity.ok(ApiResponse.ok("Payment session created", response));
    }

    @PostMapping("/api/v1/payments/ipn")
    public ResponseEntity<Map<String, Object>> handleIpn(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Secret-Key", required = false) String secretKey) {
        log.info("IPN received: X-Secret-Key={}", secretKey != null ? secretKey.substring(0, Math.min(8, secretKey.length())) + "..." : "null");
        try {
            paymentService.handleIpnNotification(rawBody, secretKey);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("IPN processing failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/api/v1/payments/callback")
    public ResponseEntity<ApiResponse<Void>> logCallback(
            @Valid @RequestBody PaymentCallbackRequest request) {
        paymentService.logPaymentCallback(request);
        return ResponseEntity.ok(ApiResponse.ok("Callback logged", null));
    }

    @GetMapping("/api/v1/payments/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable UUID paymentId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        PaymentResponse response = paymentService.getPayment(userId, paymentId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/api/v1/payments/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> requestRefund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundPaymentRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        PaymentResponse response = paymentService.requestRefund(userId, paymentId, request);
        return ResponseEntity.ok(ApiResponse.ok("Refund requested", response));
    }

    @PostMapping("/api/v1/payments/{paymentId}/refund/approve")
    public ResponseEntity<ApiResponse<PaymentResponse>> approveRefund(
            @PathVariable UUID paymentId) {
        PaymentResponse response = paymentService.approveRefund(paymentId);
        return ResponseEntity.ok(ApiResponse.ok("Refund approved", response));
    }

    @GetMapping("/api/v1/payments/history")
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> getPaymentHistory() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<PaymentHistoryResponse> history = paymentService.getPaymentHistory(userId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @GetMapping("/api/v1/payments/seller/history")
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> getSellerPaymentHistory() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<PaymentHistoryResponse> history = paymentService.getSellerPaymentHistory(userId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
