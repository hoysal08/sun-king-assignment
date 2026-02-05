package com.oms.order.client;

import com.oms.common.dto.ApiResponse;
import com.oms.common.dto.InventoryDto.*;
import com.oms.common.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.UUID;

/**
 * Client for communicating with Inventory Service.
 * Implements circuit breaker and retry patterns for resilience.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.inventory-service.url}")
    private String inventoryServiceUrl;

    @Value("${app.inventory-service.timeout-seconds:5}")
    private int timeoutSeconds;

    /**
     * Get product information from inventory service.
     */
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "getProductFallback")
    @Retry(name = "inventoryService")
    public ProductResponse getProduct(String sku) {
        log.info("Fetching product info for SKU: {}", sku);
        
        try {
            ApiResponse<ProductResponse> response = webClientBuilder.build()
                    .get()
                    .uri(inventoryServiceUrl + "/api/v1/inventory/{sku}", sku)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<ProductResponse>>() {})
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            
            if (response != null && response.isSuccess()) {
                return response.getData();
            }
            throw new ServiceUnavailableException("Inventory Service");
        } catch (Exception e) {
            log.error("Error fetching product {}: {}", sku, e.getMessage());
            throw new ServiceUnavailableException("Inventory Service", e);
        }
    }

    /**
     * Reserve stock for an order.
     */
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "reserveStockFallback")
    @Retry(name = "inventoryService")
    public boolean reserveStock(String sku, int quantity, UUID orderId) {
        log.info("Reserving {} units of SKU {} for order {}", quantity, sku, orderId);
        
        try {
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .quantity(quantity)
                    .build();
            
            ApiResponse<Void> response = webClientBuilder.build()
                    .post()
                    .uri(inventoryServiceUrl + "/api/v1/inventory/{sku}/reserve", sku)
                    .header("X-Order-Id", orderId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Void>>() {})
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            
            return response != null && response.isSuccess();
        } catch (Exception e) {
            log.error("Error reserving stock for SKU {}: {}", sku, e.getMessage());
            throw new ServiceUnavailableException("Inventory Service", e);
        }
    }

    /**
     * Release reserved stock.
     */
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "releaseStockFallback")
    @Retry(name = "inventoryService")
    public boolean releaseStock(String sku, int quantity, UUID orderId) {
        log.info("Releasing {} units of SKU {} for order {}", quantity, sku, orderId);
        
        try {
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .quantity(quantity)
                    .build();
            
            ApiResponse<Void> response = webClientBuilder.build()
                    .post()
                    .uri(inventoryServiceUrl + "/api/v1/inventory/{sku}/release", sku)
                    .header("X-Order-Id", orderId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Void>>() {})
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            
            return response != null && response.isSuccess();
        } catch (Exception e) {
            log.error("Error releasing stock for SKU {}: {}", sku, e.getMessage());
            throw new ServiceUnavailableException("Inventory Service", e);
        }
    }

    // Fallback methods
    private ProductResponse getProductFallback(String sku, Throwable t) {
        log.error("Fallback: Unable to fetch product {} - {}", sku, t.getMessage());
        throw new ServiceUnavailableException("Inventory Service");
    }

    private boolean reserveStockFallback(String sku, int quantity, UUID orderId, Throwable t) {
        log.error("Fallback: Unable to reserve stock for {} - {}", sku, t.getMessage());
        throw new ServiceUnavailableException("Inventory Service");
    }

    private boolean releaseStockFallback(String sku, int quantity, UUID orderId, Throwable t) {
        log.error("Fallback: Unable to release stock for {} - {}", sku, t.getMessage());
        throw new ServiceUnavailableException("Inventory Service");
    }
}
