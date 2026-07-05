package com.marketplace.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.order.dto.OrderItemResponse;
import com.marketplace.order.dto.OrderResponse;
import com.marketplace.order.service.OrderService;
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
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String userId = "test-user-id";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private OrderResponse createTestOrderResponse() {
        OrderItemResponse item = new OrderItemResponse(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "Test Product",
                null,
                new BigDecimal("29.99"),
                2,
                new BigDecimal("59.98")
        );
        return new OrderResponse(
                UUID.randomUUID().toString(),
                "PENDING",
                "PENDING",
                new BigDecimal("59.98"),
                "USD",
                "{\"street\":\"123 Main St\",\"city\":\"Springfield\",\"state\":\"IL\",\"zip\":\"62701\",\"country\":\"US\"}",
                Instant.now(),
                null,
                null,
                null,
                false,
                null,
                null,
                List.of(item),
                Instant.now(),
                null
        );
    }

    @Test
    void placeOrder_returnsCreated() throws Exception {
        OrderResponse response = createTestOrderResponse();
        when(orderService.placeOrder(eq(userId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shipping_address\":{\"street\":\"123 Main St\",\"city\":\"Springfield\",\"state\":\"IL\",\"zip\":\"62701\",\"country\":\"US\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Order placed successfully"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void placeOrder_returnsBadRequest_whenAddressMissing() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_returnsBadRequest_whenCartEmpty() throws Exception {
        when(orderService.placeOrder(eq(userId), any()))
                .thenThrow(new BusinessException("Cart is empty"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shipping_address\":{\"street\":\"123 Main St\",\"city\":\"Springfield\",\"state\":\"IL\",\"zip\":\"62701\",\"country\":\"US\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrders_returnsOrders() throws Exception {
        OrderResponse response = createTestOrderResponse();
        when(orderService.getOrders(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void getOrder_returnsOrder() throws Exception {
        OrderResponse response = createTestOrderResponse();
        when(orderService.getOrder(eq(userId), any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getOrder_returnsNotFound_whenOrderNotFound() throws Exception {
        when(orderService.getOrder(eq(userId), any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Order", "id", UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/orders/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_returnsOk() throws Exception {
        OrderResponse response = createTestOrderResponse();
        when(orderService.updateStatus(eq(userId), any(UUID.class), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/orders/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order status updated"));
    }

    @Test
    void cancelOrder_returnsOk() throws Exception {
        OrderResponse response = createTestOrderResponse();
        when(orderService.cancelOrder(eq(userId), any(UUID.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Changed my mind\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order cancelled"));
    }

    @Test
    void cancelOrder_returnsBadRequest_whenReasonMissing() throws Exception {
        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestReturn_returnsOk() throws Exception {
        OrderResponse response = createTestOrderResponse();
        when(orderService.requestReturn(eq(userId), any(UUID.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Defective product\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Return requested"));
    }

    @Test
    void requestReturn_returnsBadRequest_whenReasonMissing() throws Exception {
        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
