package com.marketplace.payment.controller;

import com.marketplace.payment.dto.PaymentHistoryResponse;
import com.marketplace.payment.dto.PaymentResponse;
import com.marketplace.payment.dto.ProcessPaymentRequest;
import com.marketplace.payment.dto.RefundPaymentRequest;
import com.marketplace.payment.service.PaymentService;
import com.marketplace.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/v1/orders/{orderId}/payments")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody ProcessPaymentRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        PaymentResponse response = paymentService.processPayment(userId, orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Payment processed", response));
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
