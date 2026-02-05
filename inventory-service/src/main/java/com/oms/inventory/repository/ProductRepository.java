package com.oms.inventory.repository;

import com.oms.inventory.entity.Product;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Product entity with pessimistic locking support.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Find product by SKU.
     */
    Optional<Product> findBySku(String sku);

    /**
     * Find product by SKU with pessimistic write lock.
     * Use this for reservation operations to prevent race conditions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT p FROM Product p WHERE p.sku = :sku")
    Optional<Product> findBySkuForUpdate(@Param("sku") String sku);

    /**
     * Check if SKU exists.
     */
    boolean existsBySku(String sku);

    /**
     * Find all products with pagination.
     */
    Page<Product> findAll(Pageable pageable);

    /**
     * Search products by name containing (case insensitive).
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find products with low stock (available < threshold).
     */
    @Query("SELECT p FROM Product p WHERE (p.quantity - p.reservedQuantity) < :threshold")
    Page<Product> findLowStockProducts(@Param("threshold") int threshold, Pageable pageable);
}
