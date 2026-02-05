package com.oms.order.service;

import com.oms.common.dto.InventoryDto.ProductResponse;
import com.oms.common.dto.OrderDto.*;
import com.oms.common.dto.PagedResponse;
import com.oms.common.enums.OrderStatus;
import com.oms.common.event.OrderEvent;
import com.oms.common.exception.InvalidOrderStateException;
import com.oms.common.exception.OrderNotFoundException;
import com.oms.order.client.InventoryClient;
import com.oms.order.entity.Order;
import com.oms.order.entity.OrderItem;
import com.oms.order.kafka.OrderEventProducer;
import com.oms.order.mapper.OrderMapper;
import com.oms.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for order management operations.
 * Implements Saga pattern for distributed order placement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final OrderEventProducer eventProducer;
    private final OrderMapper orderMapper;

    private static final int MAX_RETRIES = 3;

    /**
     * Place a new order (async processing via Kafka).
     * Returns immediately with PENDING status, actual processing happens asynchronously.
     */
    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest request) {
        log.info("Placing order for customer: {}", request.getCustomerId());

        // Create order with PENDING status
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .items(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        // Add order items
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productSku(itemRequest.getSku())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(BigDecimal.ZERO) // Will be fetched during processing
                    .subtotal(BigDecimal.ZERO)
                    .build();
            order.addItem(item);
        }

        // Save order
        order = orderRepository.save(order);
        log.info("Order created with ID: {}, status: PENDING", order.getId());

        // Publish event for async processing
        OrderEvent event = OrderEvent.created(
                order.getId(),
                order.getCustomerId(),
                order.getItems().stream()
                        .map(item -> new OrderEvent.OrderItemEvent(item.getProductSku(), item.getQuantity()))
                        .collect(Collectors.toList())
        );
        eventProducer.publishOrderEvent(event);

        return orderMapper.toResponse(order);
    }

    /**
     * Process order - reserve inventory and confirm order.
     * This is called by the Kafka consumer.
     */
    @Transactional
    public void processOrder(UUID orderId) {
        log.info("Processing order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} is not in PENDING status, skipping processing", orderId);
            return;
        }

        List<OrderItem> reservedItems = new ArrayList<>();
        
        try {
            // Saga Step 1: Reserve inventory for each item
            for (OrderItem item : order.getItems()) {
                // Fetch product info and price
                ProductResponse product = inventoryClient.getProduct(item.getProductSku());
                item.setProductName(product.getName());
                item.setUnitPrice(product.getPrice());
                item.calculateSubtotal();

                // Reserve stock
                inventoryClient.reserveStock(item.getProductSku(), item.getQuantity(), orderId);
                reservedItems.add(item);
                log.info("Reserved {} units of {} for order {}", 
                        item.getQuantity(), item.getProductSku(), orderId);
            }

            // Calculate total
            order.calculateTotal();

            // Update status to CONFIRMED
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            // Publish confirmation event
            eventProducer.publishOrderEvent(
                    OrderEvent.statusChanged(orderId, OrderStatus.PENDING, OrderStatus.CONFIRMED, null)
            );

            log.info("Order {} confirmed successfully", orderId);

        } catch (Exception e) {
            log.error("Error processing order {}: {}", orderId, e.getMessage());
            
            // Saga Compensation: Release reserved inventory
            compensateReservations(reservedItems, orderId);

            // Mark order as failed
            order.recordFailure(e.getMessage());
            if (order.getRetryCount() >= MAX_RETRIES) {
                order.setStatus(OrderStatus.FAILED);
                log.warn("Order {} failed after {} retries", orderId, MAX_RETRIES);
            }
            orderRepository.save(order);

            // Publish failure event
            eventProducer.publishOrderEvent(
                    OrderEvent.statusChanged(orderId, OrderStatus.PENDING, 
                            order.getStatus(), e.getMessage())
            );

            throw e;
        }
    }

    /**
     * Compensation logic - release reserved inventory on failure.
     */
    private void compensateReservations(List<OrderItem> reservedItems, UUID orderId) {
        log.info("Compensating reservations for order: {}", orderId);
        for (OrderItem item : reservedItems) {
            try {
                inventoryClient.releaseStock(item.getProductSku(), item.getQuantity(), orderId);
                log.info("Released {} units of {} for order {}", 
                        item.getQuantity(), item.getProductSku(), orderId);
            } catch (Exception e) {
                log.error("Failed to release stock for {} in order {}: {}", 
                        item.getProductSku(), orderId, e.getMessage());
                // Log and continue - compensation should not fail the entire process
            }
        }
    }

    /**
     * Get order by ID.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderMapper.toResponse(order);
    }

    /**
     * Get all orders with pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryResponse> getOrders(int page, int size, String customerId, OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage;

        if (customerId != null && status != null) {
            orderPage = orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        } else if (customerId != null) {
            orderPage = orderRepository.findByCustomerId(customerId, pageable);
        } else if (status != null) {
            orderPage = orderRepository.findByStatus(status, pageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        return PagedResponse.<OrderSummaryResponse>builder()
                .content(orderPage.getContent().stream()
                        .map(orderMapper::toSummaryResponse)
                        .toList())
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .first(orderPage.isFirst())
                .last(orderPage.isLast())
                .hasNext(orderPage.hasNext())
                .hasPrevious(orderPage.hasPrevious())
                .build();
    }

    /**
     * Update order status.
     */
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus previousStatus = order.getStatus();
        
        if (!previousStatus.canTransitionTo(request.getStatus())) {
            throw new InvalidOrderStateException(previousStatus, request.getStatus());
        }

        order.setStatus(request.getStatus());
        order = orderRepository.save(order);

        // Publish status change event
        eventProducer.publishOrderEvent(
                OrderEvent.statusChanged(orderId, previousStatus, request.getStatus(), request.getReason())
        );

        log.info("Order {} status updated: {} -> {}", orderId, previousStatus, request.getStatus());
        return orderMapper.toResponse(order);
    }

    /**
     * Cancel an order.
     */
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus previousStatus = order.getStatus();
        
        if (!previousStatus.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new InvalidOrderStateException(
                    "Order cannot be cancelled in status: " + previousStatus);
        }

        // Release inventory if order was confirmed
        if (previousStatus == OrderStatus.CONFIRMED || previousStatus == OrderStatus.PROCESSING) {
            compensateReservations(order.getItems(), orderId);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setFailureReason(reason);
        order = orderRepository.save(order);

        // Publish cancellation event
        eventProducer.publishOrderEvent(
                OrderEvent.statusChanged(orderId, previousStatus, OrderStatus.CANCELLED, reason)
        );

        log.info("Order {} cancelled: {}", orderId, reason);
        return orderMapper.toResponse(order);
    }
}
