package com.oms.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Product entity representing inventory items.
 * Uses optimistic locking via @Version for concurrent access handling.
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_sku", columnList = "sku", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Total quantity in stock.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Quantity reserved for pending orders.
     * Available quantity = quantity - reservedQuantity
     */
    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    /**
     * Optimistic locking version for concurrent access control.
     */
    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Calculate available quantity (not reserved).
     */
    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    /**
     * Check if requested quantity is available.
     */
    public boolean hasAvailable(int requestedQuantity) {
        return getAvailableQuantity() >= requestedQuantity;
    }

    /**
     * Reserve stock for an order.
     * @throws IllegalStateException if insufficient stock
     */
    public void reserve(int quantityToReserve) {
        if (!hasAvailable(quantityToReserve)) {
            throw new IllegalStateException(
                String.format("Cannot reserve %d units. Only %d available.", 
                    quantityToReserve, getAvailableQuantity())
            );
        }
        this.reservedQuantity += quantityToReserve;
    }

    /**
     * Release reserved stock (e.g., order cancelled).
     */
    public void release(int quantityToRelease) {
        if (quantityToRelease > this.reservedQuantity) {
            throw new IllegalStateException(
                String.format("Cannot release %d units. Only %d reserved.", 
                    quantityToRelease, this.reservedQuantity)
            );
        }
        this.reservedQuantity -= quantityToRelease;
    }

    /**
     * Confirm reservation and deduct from stock (order shipped).
     */
    public void confirmDeduction(int quantityToDeduct) {
        if (quantityToDeduct > this.reservedQuantity) {
            throw new IllegalStateException(
                String.format("Cannot deduct %d units. Only %d reserved.", 
                    quantityToDeduct, this.reservedQuantity)
            );
        }
        this.reservedQuantity -= quantityToDeduct;
        this.quantity -= quantityToDeduct;
    }
}
