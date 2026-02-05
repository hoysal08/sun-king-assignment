# Scaling Strategy for Production

## Overview

This document outlines strategies for scaling the Order Management System (OMS) in a production environment to handle Black Friday-level traffic.

## Current Architecture

The OMS is designed as a microservices architecture with:
- **Order Service**: Handles order placement, status updates
- **Inventory Service**: Manages product stock levels
- **Kafka**: Async order processing queue
- **PostgreSQL**: Separate databases per service

## Horizontal Scaling

### Order Service (High Read/Write Volume)

- Minimum 3 replicas for high availability
- Scale up to 10 replicas during peak traffic
- Use CPU-based autoscaling with 70% threshold

### Inventory Service (Locking Intensive)
- Fewer replicas due to pessimistic locking on database
- Scale database connection pool carefully
- Consider read replicas for stock queries

## Database Optimization

### Connection Pooling

```yaml
# HikariCP Configuration
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
```

### Read Replicas

```
┌─────────────┐
│   Primary   │◄── Writes
│  PostgreSQL │
└──────┬──────┘
       │ Replication
┌──────┴──────┐
│   Replica   │◄── Reads
│  PostgreSQL │
└─────────────┘
```

**Implementation:**
- Route read queries to replicas
- Use `@Transactional(readOnly = true)` for read operations
- Configure Spring to use replica for read-only transactions

### Indexing Strategy

Already implemented:
- `idx_orders_customer_id` - Customer lookups
- `idx_orders_status` - Status filtering
- `idx_orders_created_at` - Time-based queries
- `idx_products_sku` - SKU lookups

## Kafka Scaling

### Topic Configuration

```properties
# Production settings
order-events:
  partitions: 12
  replication-factor: 3
  retention.ms: 604800000  # 7 days
```

### Consumer Group Scaling

- 1 partition = 1 consumer max
- Scale consumer instances up to partition count
- Use `ConcurrentKafkaListenerContainerFactory` with `concurrency: 3`

## Rate Limiting Strategy

### Per-Service Rate Limiting

Current: 200 requests/minute/instance

**For Black Friday:**
```yaml
app:
  rate-limit:
    requests-per-minute: 500
```

## Caching Strategy

### Product Cache (Future Enhancement)

```java
@Cacheable(value = "products", key = "#sku")
public ProductResponse getProduct(String sku) {
    // DB lookup
}
```

**Recommended TTL:** 5 minutes for product info

## Circuit Breaker Tuning

### Peak Traffic Settings

```yaml
resilience4j:
  circuitbreaker:
    instances:
      inventoryService:
        slidingWindowSize: 20           # Larger window
        minimumNumberOfCalls: 10        # More samples
        waitDurationInOpenState: 5s     # Faster recovery
        failureRateThreshold: 60        # More tolerant
```

## Summary

1. **Start with**: 3 Order + 2 Inventory pods
2. **Enable**: Kafka with 12 partitions
3. **Configure**: Read replicas for PostgreSQL
4. **Monitor**: Set up alerting for lag and latency
5. **Scale**: Use HPA based on CPU/memory metrics
