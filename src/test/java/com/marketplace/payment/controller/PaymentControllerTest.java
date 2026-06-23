package com.marketplace.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.payment.dto.PaymentHistoryResponse;
import com.marketplace.payment.dto.PaymentResponse;
import com.marketplace.payment.service.PaymentService;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.GlobalExceptionHandler;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String userId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, java.util.List.of()));
    }

    // ==================== PROCESS PAYMENT ====================

    @Test
    void processPayment_success() throws Exception {
        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                new BigDecimal("99.99"), "USD", "CREDIT_CARD", "COMPLETED",
                "4242", "VISA", "MOCK-ABC12345", null, null, Instant.now());

        when(paymentService.processPayment(anyString(), any(UUID.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"4242424242424242\",\"cardHolder\":\"John Doe\","
                                + "\"expiryMonth\":12,\"expiryYear\":2026,\"cvv\":\"123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Payment processed"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.cardBrand").value("VISA"));
    }

    @Test
    void processPayment_invalidCard_returns400() throws Exception {
        when(paymentService.processPayment(anyString(), any(UUID.class), any()))
                .thenThrow(new BusinessException("Invalid card number format"));

        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"123\",\"cardHolder\":\"John Doe\","
                                + "\"expiryMonth\":12,\"expiryYear\":2026,\"cvv\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid card number format"));
    }

    @Test
    void processPayment_orderNotFound_returns404() throws Exception {
        when(paymentService.processPayment(anyString(), any(UUID.class), any()))
                .thenThrow(new ResourceNotFoundException("Order", "id", UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"4242424242424242\",\"cardHolder\":\"John Doe\","
                                + "\"expiryMonth\":12,\"expiryYear\":2026,\"cvv\":\"123\"}"))
                .andExpect(status().isNotFound());
    }

    // ==================== GET PAYMENT ====================

    @Test
    void getPayment_success() throws Exception {
        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                new BigDecimal("99.99"), "USD", "CREDIT_CARD", "COMPLETED",
                "4242", "VISA", "MOCK-ABC12345", null, null, Instant.now());

        when(paymentService.getPayment(anyString(), any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    // ==================== REFUND ====================

    @Test
    void requestRefund_success() throws Exception {
        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                new BigDecimal("99.99"), "USD", "CREDIT_CARD", "REFUNDED",
                "4242", "VISA", "MOCK-ABC12345", null, Instant.now(), Instant.now());

        when(paymentService.requestRefund(anyString(), any(UUID.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Changed my mind\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Refund requested"))
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
    }

    @Test
    void requestRefund_notCompletedPayment_returns400() throws Exception {
        when(paymentService.requestRefund(anyString(), any(UUID.class), any()))
                .thenThrow(new BusinessException("Only completed payments can be refunded"));

        mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Changed my mind\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only completed payments can be refunded"));
    }

    // ==================== PAYMENT HISTORY ====================

    @Test
    void getPaymentHistory_success() throws Exception {
        PaymentHistoryResponse history = new PaymentHistoryResponse(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                new BigDecimal("99.99"), "USD", "CREDIT_CARD", "COMPLETED", Instant.now());

        when(paymentService.getPaymentHistory(anyString())).thenReturn(List.of(history));

        mockMvc.perform(get("/api/v1/payments/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }
}
