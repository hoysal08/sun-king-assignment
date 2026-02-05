package com.oms.inventory.service;

import com.oms.common.dto.InventoryDto.*;
import com.oms.common.dto.PagedResponse;
import com.oms.common.event.InventoryEvent;
import com.oms.common.exception.InsufficientInventoryException;
import com.oms.common.exception.ProductNotFoundException;
import com.oms.inventory.entity.Product;
import com.oms.inventory.kafka.InventoryEventProducer;
import com.oms.inventory.mapper.ProductMapper;
import com.oms.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for inventory management operations.
 * Implements race condition handling with pessimistic locking and retry mechanism.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryEventProducer eventProducer;
    private final ProductMapper productMapper;

    /**
     * Get all products with pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Product> productPage = productRepository.findAll(pageable);
        
        return PagedResponse.<ProductResponse>builder()
                .content(productPage.getContent().stream()
                        .map(productMapper::toResponse)
                        .toList())
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .hasNext(productPage.hasNext())
                .hasPrevious(productPage.hasPrevious())
                .build();
    }

    /**
     * Get product by SKU.
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));
        return productMapper.toResponse(product);
    }

    /**
     * Check stock availability for a SKU.
     */
    @Transactional(readOnly = true)
    public StockCheckResponse checkStock(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));
        return StockCheckResponse.builder()
                .sku(sku)
                .availableQuantity(product.getAvailableQuantity())
                .inStock(product.getAvailableQuantity() > 0)
                .build();
    }

    /**
     * Create a new product.
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DataIntegrityViolationException("Product with SKU '" + request.getSku() + "' already exists");
        }
        
        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .reservedQuantity(0)
                .price(request.getPrice())
                .build();
        
        product = productRepository.save(product);
        log.info("Created product with SKU: {}", product.getSku());
        
        return productMapper.toResponse(product);
    }

    /**
     * Update stock quantity for a product.
     */
    @Transactional
    public ProductResponse updateStock(String sku, UpdateStockRequest request) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));
        
        int previousQuantity = product.getQuantity();
        product.setQuantity(request.getQuantity());
        
        product = productRepository.save(product);
        log.info("Updated stock for SKU {}: {} -> {}", sku, previousQuantity, request.getQuantity());
        
        return productMapper.toResponse(product);
    }

    /**
     * Reserve stock for an order.
     * Uses pessimistic locking to prevent race conditions during concurrent reservations.
     * Includes retry mechanism for optimistic locking failures.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public boolean reserveStock(String sku, int quantity, UUID orderId) {
        log.info("Reserving {} units of SKU {} for order {}", quantity, sku, orderId);
        
        // Use pessimistic locking to prevent concurrent modifications
        Product product = productRepository.findBySkuForUpdate(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));
        
        int available = product.getAvailableQuantity();
        if (available < quantity) {
            log.warn("Insufficient inventory for SKU {}: requested {}, available {}", 
                    sku, quantity, available);
            throw new InsufficientInventoryException(sku, quantity, available);
        }
        
        int previousReserved = product.getReservedQuantity();
        product.reserve(quantity);
        productRepository.save(product);
        
        // Publish inventory event
        eventProducer.publishInventoryEvent(
                InventoryEvent.reserved(sku, quantity, orderId, available)
        );
        
        log.info("Reserved {} units of SKU {} for order {}. Reserved: {} -> {}", 
                quantity, sku, orderId, previousReserved, product.getReservedQuantity());
        return true;
    }

    /**
     * Release reserved stock (e.g., order cancelled).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public boolean releaseStock(String sku, int quantity, UUID orderId) {
        log.info("Releasing {} units of SKU {} for order {}", quantity, sku, orderId);
        
        Product product = productRepository.findBySkuForUpdate(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));
        
        int previousReserved = product.getReservedQuantity();
        product.release(quantity);
        productRepository.save(product);
        
        // Publish inventory event
        eventProducer.publishInventoryEvent(
                InventoryEvent.released(sku, quantity, orderId, product.getAvailableQuantity() - quantity)
        );
        
        log.info("Released {} units of SKU {} for order {}. Reserved: {} -> {}", 
                quantity, sku, orderId, previousReserved, product.getReservedQuantity());
        return true;
    }

    /**
     * Confirm stock deduction when order is shipped.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean confirmDeduction(String sku, int quantity) {
        log.info("Confirming deduction of {} units of SKU {}", quantity, sku);
        
        Product product = productRepository.findBySkuForUpdate(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));
        
        product.confirmDeduction(quantity);
        productRepository.save(product);
        
        log.info("Confirmed deduction of {} units of SKU {}", quantity, sku);
        return true;
    }
}
