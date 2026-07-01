package com.marketplace.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.payment.dto.IpnNotification;
import com.marketplace.payment.dto.PaymentHistoryResponse;
import com.marketplace.payment.dto.PaymentResponse;
import com.marketplace.payment.dto.SePayCheckoutResponse;
import com.marketplace.payment.service.PaymentService;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.GlobalExceptionHandler;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

    // ==================== INITIATE PAYMENT ====================

    @Test
    void initiatePayment_success() throws Exception {
        SePayCheckoutResponse response = new SePayCheckoutResponse(
                "https://pay-sandbox.sepay.vn/v1/checkout/init",
                Map.of("merchant", "test", "signature", "abc123"));

        when(paymentService.initiatePayment(anyString(), any(UUID.class), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"BANK_TRANSFER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment session created"))
                .andExpect(jsonPath("$.data.checkoutUrl").value("https://pay-sandbox.sepay.vn/v1/checkout/init"))
                .andExpect(jsonPath("$.data.formFields.merchant").value("test"));
    }

    @Test
    void initiatePayment_alreadyPaid_returns400() throws Exception {
        when(paymentService.initiatePayment(anyString(), any(UUID.class), anyString()))
                .thenThrow(new BusinessException("Order has already been paid"));

        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"BANK_TRANSFER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Order has already been paid"));
    }

    @Test
    void initiatePayment_orderNotFound_returns404() throws Exception {
        when(paymentService.initiatePayment(anyString(), any(UUID.class), anyString()))
                .thenThrow(new ResourceNotFoundException("Order", "id", UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"BANK_TRANSFER\"}"))
                .andExpect(status().isNotFound());
    }

    // ==================== GET PAYMENT ====================

    @Test
    void getPayment_success() throws Exception {
        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                new BigDecimal("99.99"), "VND", "SEPAY", "COMPLETED",
                null, null, null, "ORDTEST1234", null, null, Instant.now());

        when(paymentService.getPayment(anyString(), any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.method").value("SEPAY"));
    }

    // ==================== REFUND ====================

    @Test
    void requestRefund_success() throws Exception {
        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                new BigDecimal("99.99"), "VND", "SEPAY", "REFUNDED",
                null, null, null, "ORDTEST1234", null, Instant.now(), Instant.now());

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
                new BigDecimal("99.99"), "VND", "SEPAY", "COMPLETED", Instant.now());

        when(paymentService.getPaymentHistory(anyString())).thenReturn(List.of(history));

        mockMvc.perform(get("/api/v1/payments/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }
}
