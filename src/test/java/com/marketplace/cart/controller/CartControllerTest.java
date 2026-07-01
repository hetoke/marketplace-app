package com.marketplace.cart.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.cart.dto.CartItemResponse;
import com.marketplace.cart.dto.CartResponse;
import com.marketplace.cart.service.CartService;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.GlobalExceptionHandler;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
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
class CartControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String userId = "test-user-id";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private CartResponse createTestCartResponse() {
        CartItemResponse item = new CartItemResponse(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "Test Product",
                2,
                new BigDecimal("29.99"),
                new BigDecimal("59.98"),
                10
        );
        return new CartResponse(
                UUID.randomUUID().toString(),
                List.of(item),
                new BigDecimal("59.98"),
                1
        );
    }

    @Test
    void getCart_returnsCart() throws Exception {
        CartResponse response = createTestCartResponse();
        when(cartService.getCart(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(response.id()))
                .andExpect(jsonPath("$.data.itemCount").value(1));
    }

    @Test
    void addItem_returnsCreated() throws Exception {
        CartResponse response = createTestCartResponse();
        String productId = UUID.randomUUID().toString();
        when(cartService.addItem(eq(userId), any(UUID.class), eq(2))).thenReturn(response);

        mockMvc.perform(post("/api/v1/cart/items/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Item added to cart"));
    }

    @Test
    void addItem_returnsBadRequest_whenQuantityLessThanOne() throws Exception {
        String productId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/cart/items/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addItem_returnsNotFound_whenProductNotFound() throws Exception {
        String productId = UUID.randomUUID().toString();
        when(cartService.addItem(eq(userId), any(UUID.class), eq(1)))
                .thenThrow(new ResourceNotFoundException("Product", "id", UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/cart/items/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addItem_returnsBadRequest_whenInsufficientStock() throws Exception {
        String productId = UUID.randomUUID().toString();
        when(cartService.addItem(eq(userId), any(UUID.class), eq(10)))
                .thenThrow(new BusinessException("Insufficient stock. Available: 3"));

        mockMvc.perform(post("/api/v1/cart/items/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateQuantity_returnsOk() throws Exception {
        CartResponse response = createTestCartResponse();
        String itemId = UUID.randomUUID().toString();
        when(cartService.updateQuantity(eq(userId), any(UUID.class), eq(5))).thenReturn(response);

        mockMvc.perform(put("/api/v1/cart/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart item updated"));
    }

    @Test
    void updateQuantity_returnsBadRequest_whenQuantityLessThanOne() throws Exception {
        String itemId = UUID.randomUUID().toString();

        mockMvc.perform(put("/api/v1/cart/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateQuantity_returnsNotFound_whenItemNotFound() throws Exception {
        String itemId = UUID.randomUUID().toString();
        when(cartService.updateQuantity(eq(userId), any(UUID.class), eq(5)))
                .thenThrow(new ResourceNotFoundException("Cart item", "id", UUID.randomUUID()));

        mockMvc.perform(put("/api/v1/cart/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeItem_returnsOk() throws Exception {
        CartResponse response = createTestCartResponse();
        String itemId = UUID.randomUUID().toString();
        when(cartService.removeItem(eq(userId), any(UUID.class))).thenReturn(response);

        mockMvc.perform(delete("/api/v1/cart/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Item removed from cart"));
    }

    @Test
    void removeItem_returnsNotFound_whenItemNotFound() throws Exception {
        String itemId = UUID.randomUUID().toString();
        when(cartService.removeItem(eq(userId), any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Cart item", "id", UUID.randomUUID()));

        mockMvc.perform(delete("/api/v1/cart/items/" + itemId))
                .andExpect(status().isNotFound());
    }

    @Test
    void clearCart_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart cleared"));
    }
}
