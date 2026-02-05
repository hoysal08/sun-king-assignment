package com.oms.order.repository;

import com.oms.common.enums.OrderStatus;
import com.oms.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Order entity.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find orders by customer ID with pagination.
     */
    Page<Order> findByCustomerId(String customerId, Pageable pageable);

    /**
     * Find orders by status with pagination.
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Find orders by customer ID and status.
     */
    Page<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status, Pageable pageable);

    /**
     * Find pending orders that need to be processed.
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.retryCount < :maxRetries")
    List<Order> findPendingOrdersForProcessing(
            @Param("status") OrderStatus status, 
            @Param("maxRetries") int maxRetries);

    /**
     * Find orders created between dates.
     */
    Page<Order> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * Count orders by status.
     */
    long countByStatus(OrderStatus status);
}
