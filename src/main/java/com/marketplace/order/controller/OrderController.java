package com.marketplace.order.controller;

import com.marketplace.order.dto.CancelOrderRequest;
import com.marketplace.order.dto.OrderResponse;
import com.marketplace.order.dto.PlaceOrderRequest;
import com.marketplace.order.dto.ReturnRequest;
import com.marketplace.order.model.OrderStatus;
import com.marketplace.order.service.OrderService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        OrderResponse order = orderService.placeOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed successfully", order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<OrderResponse> orders = orderService.getOrders(userId);
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID orderId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        OrderResponse order = orderService.getOrder(userId, orderId);
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID orderId,
            @RequestBody java.util.Map<String, String> body) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        OrderStatus newStatus = OrderStatus.valueOf(body.get("status"));
        OrderResponse order = orderService.updateStatus(userId, orderId, newStatus);
        return ResponseEntity.ok(ApiResponse.ok("Order status updated", order));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        OrderResponse order = orderService.cancelOrder(userId, orderId, request);
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled", order));
    }

    @PostMapping("/{orderId}/return")
    public ResponseEntity<ApiResponse<OrderResponse>> requestReturn(
            @PathVariable UUID orderId,
            @Valid @RequestBody ReturnRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        OrderResponse order = orderService.requestReturn(userId, orderId, request);
        return ResponseEntity.ok(ApiResponse.ok("Return requested", order));
    }
}
