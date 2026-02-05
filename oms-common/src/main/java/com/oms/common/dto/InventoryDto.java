package com.oms.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTOs for inventory operations.
 */
public class InventoryDto {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateProductRequest {
        
        @NotBlank(message = "SKU is required")
        private String sku;
        
        @NotBlank(message = "Product name is required")
        private String name;
        
        private String description;
        
        @NotNull(message = "Initial quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        private Integer quantity;
        
        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        private BigDecimal price;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStockRequest {
        
        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        private Integer quantity;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReserveStockRequest {
        
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private Integer quantity;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductResponse {
        private String id;
        private String sku;
        private String name;
        private String description;
        private int quantity;
        private int reservedQuantity;
        private int availableQuantity;
        private BigDecimal price;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockCheckResponse {
        private String sku;
        private int availableQuantity;
        private boolean inStock;
    }
}
