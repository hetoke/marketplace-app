package com.marketplace.wishlist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.GlobalExceptionHandler;
import com.marketplace.shared.exception.ResourceNotFoundException;
import com.marketplace.wishlist.dto.WishlistItemResponse;
import com.marketplace.wishlist.service.WishlistService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class WishlistControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WishlistService wishlistService;

    @InjectMocks
    private WishlistController wishlistController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String userId = "test-user-id";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(wishlistController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private WishlistItemResponse createTestWishlistItemResponse() {
        return new WishlistItemResponse(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "Test Product",
                Instant.now()
        );
    }

    @Test
    void getWishlist_returnsItems() throws Exception {
        WishlistItemResponse item = createTestWishlistItemResponse();
        when(wishlistService.getWishlist(userId)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/buyers/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productName").value("Test Product"));
    }

    @Test
    void getWishlist_returnsEmptyList() throws Exception {
        when(wishlistService.getWishlist(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/buyers/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void addToWishlist_returnsCreated() throws Exception {
        WishlistItemResponse item = createTestWishlistItemResponse();
        String productId = UUID.randomUUID().toString();
        when(wishlistService.addToWishlist(eq(userId), any(UUID.class))).thenReturn(item);

        mockMvc.perform(post("/api/v1/buyers/wishlist/" + productId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Product added to wishlist"));
    }

    @Test
    void addToWishlist_returnsNotFound_whenProductNotFound() throws Exception {
        String productId = UUID.randomUUID().toString();
        when(wishlistService.addToWishlist(eq(userId), any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Product", "id", UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/buyers/wishlist/" + productId))
                .andExpect(status().isNotFound());
    }

    @Test
    void addToWishlist_returnsBadRequest_whenAlreadyInWishlist() throws Exception {
        String productId = UUID.randomUUID().toString();
        when(wishlistService.addToWishlist(eq(userId), any(UUID.class)))
                .thenThrow(new BusinessException("Product already in wishlist"));

        mockMvc.perform(post("/api/v1/buyers/wishlist/" + productId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeFromWishlist_returnsOk() throws Exception {
        String productId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/api/v1/buyers/wishlist/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product removed from wishlist"));
    }

    @Test
    void removeFromWishlist_returnsNotFound_whenItemNotFound() throws Exception {
        String productId = UUID.randomUUID().toString();
        doThrow(new ResourceNotFoundException("Wishlist item", "productId", UUID.randomUUID()))
                .when(wishlistService).removeFromWishlist(eq(userId), any(UUID.class));

        mockMvc.perform(delete("/api/v1/buyers/wishlist/" + productId))
                .andExpect(status().isNotFound());
    }
}
