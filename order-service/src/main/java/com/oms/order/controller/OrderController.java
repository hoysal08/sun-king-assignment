package com.oms.order.controller;

import com.oms.common.dto.ApiResponse;
import com.oms.common.dto.OrderDto.*;
import com.oms.common.dto.PagedResponse;
import com.oms.common.enums.OrderStatus;
import com.oms.order.config.RateLimiter;
import com.oms.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for order management.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management operations")
public class OrderController {

    private final OrderService orderService;
    private final RateLimiter rateLimiter;

    @PostMapping
    @Operation(summary = "Place order", description = "Place a new order (async processing)")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        rateLimiter.checkLimit();
        OrderResponse order = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(order, "Order placed successfully. Processing in progress."));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order", description = "Get order details by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @Parameter(description = "Order ID") @PathVariable("orderId") UUID orderId) {
        
        rateLimiter.checkLimit();
        OrderResponse order = orderService.getOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping
    @Operation(summary = "List orders", description = "Get paginated list of orders with optional filters")
    public ResponseEntity<ApiResponse<PagedResponse<OrderSummaryResponse>>> getOrders(
            @Parameter(description = "Page number (0-based)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(description = "Filter by customer ID") @RequestParam(name = "customerId", required = false) String customerId,
            @Parameter(description = "Filter by status") @RequestParam(name = "status", required = false) OrderStatus status) {
        
        rateLimiter.checkLimit();
        PagedResponse<OrderSummaryResponse> orders = orderService.getOrders(page, size, customerId, status);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{orderId}/status")
    @Operation(summary = "Get order status", description = "Get the current status of an order")
    public ResponseEntity<ApiResponse<OrderStatus>> getOrderStatus(
            @Parameter(description = "Order ID") @PathVariable("orderId") UUID orderId) {
        
        rateLimiter.checkLimit();
        OrderResponse order = orderService.getOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(order.getStatus()));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status", description = "Update the status of an existing order")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @Parameter(description = "Order ID") @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody UpdateStatusRequest request) {
        
        rateLimiter.checkLimit();
        OrderResponse order = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order status updated successfully"));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an existing order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable("orderId") UUID orderId,
            @Parameter(description = "Cancellation reason") @RequestParam(name = "reason", required = false) String reason) {
        
        rateLimiter.checkLimit();
        OrderResponse order = orderService.cancelOrder(orderId, reason != null ? reason : "Customer requested cancellation");
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled successfully"));
    }
}
