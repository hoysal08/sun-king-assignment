package com.oms.inventory.controller;

import com.oms.common.dto.ApiResponse;
import com.oms.common.dto.InventoryDto.*;
import com.oms.common.dto.PagedResponse;
import com.oms.inventory.config.RateLimiter;
import com.oms.inventory.service.InventoryService;
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
 * REST controller for inventory management.
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management operations")
public class InventoryController {

    private final InventoryService inventoryService;
    private final RateLimiter rateLimiter;

    @GetMapping
    @Operation(summary = "List all products", description = "Get paginated list of all products with stock information")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getAllProducts(
            @Parameter(description = "Page number (0-based)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(name = "size", defaultValue = "20") int size) {
        
        rateLimiter.checkLimit();
        PagedResponse<ProductResponse> products = inventoryService.getAllProducts(page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{sku}")
    @Operation(summary = "Get product by SKU", description = "Get product details including stock information")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySku(
            @Parameter(description = "Product SKU") @PathVariable("sku") String sku) {
        
        rateLimiter.checkLimit();
        ProductResponse product = inventoryService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/{sku}/stock")
    @Operation(summary = "Check stock availability", description = "Check if product is in stock and available quantity")
    public ResponseEntity<ApiResponse<StockCheckResponse>> checkStock(
            @Parameter(description = "Product SKU") @PathVariable("sku") String sku) {
        
        rateLimiter.checkLimit();
        StockCheckResponse stockCheck = inventoryService.checkStock(sku);
        return ResponseEntity.ok(ApiResponse.success(stockCheck));
    }

    @PostMapping
    @Operation(summary = "Create product", description = "Add a new product to inventory")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        
        rateLimiter.checkLimit();
        ProductResponse product = inventoryService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
    }

    @PutMapping("/{sku}")
    @Operation(summary = "Update stock quantity", description = "Update the stock quantity for a product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @Parameter(description = "Product SKU") @PathVariable("sku") String sku,
            @Valid @RequestBody UpdateStockRequest request) {
        
        rateLimiter.checkLimit();
        ProductResponse product = inventoryService.updateStock(sku, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Stock updated successfully"));
    }

    @PostMapping("/{sku}/reserve")
    @Operation(summary = "Reserve stock", description = "Reserve stock for an order (internal API)")
    public ResponseEntity<ApiResponse<Void>> reserveStock(
            @Parameter(description = "Product SKU") @PathVariable("sku") String sku,
            @Valid @RequestBody ReserveStockRequest request,
            @Parameter(description = "Order ID") @RequestHeader("X-Order-Id") UUID orderId) {
        
        rateLimiter.checkLimit();
        inventoryService.reserveStock(sku, request.getQuantity(), orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Stock reserved successfully"));
    }

    @PostMapping("/{sku}/release")
    @Operation(summary = "Release reserved stock", description = "Release reserved stock (e.g., order cancelled)")
    public ResponseEntity<ApiResponse<Void>> releaseStock(
            @Parameter(description = "Product SKU") @PathVariable("sku") String sku,
            @Valid @RequestBody ReserveStockRequest request,
            @Parameter(description = "Order ID") @RequestHeader("X-Order-Id") UUID orderId) {
        
        rateLimiter.checkLimit();
        inventoryService.releaseStock(sku, request.getQuantity(), orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Stock released successfully"));
    }
}
