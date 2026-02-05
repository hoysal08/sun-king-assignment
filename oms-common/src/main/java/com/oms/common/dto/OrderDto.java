package com.oms.common.dto;

import com.oms.common.enums.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTOs for order operations.
 */
public class OrderDto {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderRequest {
        
        @NotBlank(message = "Customer ID is required")
        private String customerId;
        
        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        private List<OrderItemRequest> items;
        
        private String shippingAddress;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        
        @NotBlank(message = "SKU is required")
        private String sku;
        
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private Integer quantity;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        
        @NotNull(message = "Status is required")
        private OrderStatus status;
        
        private String reason;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderResponse {
        private UUID id;
        private String customerId;
        private OrderStatus status;
        private List<OrderItemResponse> items;
        private BigDecimal totalAmount;
        private String shippingAddress;
        private String failureReason;
        private Instant createdAt;
        private Instant updatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private UUID id;
        private String sku;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummaryResponse {
        private UUID id;
        private String customerId;
        private OrderStatus status;
        private int itemCount;
        private BigDecimal totalAmount;
        private Instant createdAt;
    }
}
