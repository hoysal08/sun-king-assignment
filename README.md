# Order Management System (OMS) - Microservices

A scalable, production-grade Order Management System built with microservices architecture to handle high-traffic e-commerce scenarios like Black Friday sales.

## 🏗️ Architecture Overview

```
┌─────────────────┐     ┌─────────────────┐
│  Order Service  │────▶│Inventory Service│
│    (Port 8081)  │     │   (Port 8082)   │
└────────┬────────┘     └────────┬────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐     ┌─────────────────┐
│  PostgreSQL     │     │   PostgreSQL    │
│  (Order DB)     │     │ (Inventory DB)  │
└─────────────────┘     └─────────────────┘
         │                       │
         └──────────┬────────────┘
                    ▼
            ┌───────────────┐
            │  Apache Kafka │
            └───────────────┘
```

##  Key Features

- **Microservices Architecture**: Separate Order and Inventory services with independent databases
- **Async Order Processing**: Kafka-based queue for handling traffic spikes
- **Race Condition Handling**: Pessimistic locking for inventory operations
- **Saga Pattern**: Distributed transaction management with compensation logic
- **Rate Limiting**: Bucket4j-based API rate limiting (200 requests/min)
- **Circuit Breaker**: Resilience4j for fault tolerance
- **Retry Mechanism**: Exponential backoff for transient failures

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.2 |
| Database | PostgreSQL 16 |
| Messaging | Apache Kafka |
| Containerization | Docker |
| API Docs | OpenAPI 3 / Swagger UI |

## 📁 Project Structure

```
sun-king-assignment/
├── oms-common/           # Shared DTOs, events, exceptions
├── order-service/        # Order management microservice
├── inventory-service/    # Inventory management microservice
├── docs/
│   ├── ddl/             # Database schemas
│   └── postman/         # API collection
├── docker-compose.yml    # Infrastructure setup
└── pom.xml              # Parent POM
```

## 🚀 Quick Start

### Prerequisites

- Java 21
- Maven 3.8+
- Docker & Docker Compose

** Run with Docker Compose**
```bash
docker-compose up --build
```
** Shut down Docker with delete volume
```bash
docker compose down -v
```

### 4. Access Services

| Service | URL |
|---------|-----|
| Order Service API | http://localhost:8081 |
| Order Service Swagger | http://localhost:8081/swagger-ui.html |
| Inventory Service API | http://localhost:8082 |
| Inventory Service Swagger | http://localhost:8082/swagger-ui.html |
| Kafka UI | http://localhost:8090 |

## 📚 API Documentation

### Inventory Service Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/inventory?page=0&size=20` | List products (paginated) |
| GET | `/api/v1/inventory/{sku}` | Get product by SKU |
| GET | `/api/v1/inventory/{sku}/stock` | Check stock availability |
| POST | `/api/v1/inventory` | Create new product |
| PUT | `/api/v1/inventory/{sku}` | Update stock quantity |
| POST | `/api/v1/inventory/{sku}/reserve` | Reserve stock |
| POST | `/api/v1/inventory/{sku}/release` | Release reserved stock |

### Order Service Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/orders` | Place new order (async) |
| GET | `/api/v1/orders/{id}` | Get order by ID |
| GET | `/api/v1/orders` | List orders (paginated, filterable) |
| GET | `/api/v1/orders/{id}/status` | Get order status |
| PATCH | `/api/v1/orders/{id}/status` | Update order status |
| POST | `/api/v1/orders/{id}/cancel` | Cancel order |

### Order Status Flow

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
    ↓         ↓           ↓
 FAILED    CANCELLED   CANCELLED
```

## 🧪 Testing with Postman

1. Import `docs/postman/OMS-Collection.json` into Postman
2. Run requests in order (Order ID is automatically saved)
3. Test error scenarios with the "Error Scenarios" folder

### Sample Order Request

```json
POST /api/v1/orders
{
  "customerId": "CUST-001",
  "items": [
    { "sku": "LAPTOP-001", "quantity": 1 },
    { "sku": "PHONE-001", "quantity": 2 }
  ],
  "shippingAddress": "123 Main Street, San Francisco, CA"
}
```

### Sample Response

```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "customerId": "CUST-001",
    "status": "PENDING",
    "items": [...],
    "totalAmount": null,
    "createdAt": "2026-02-02T18:00:00Z"
  },
  "message": "Order placed successfully. Processing in progress."
}
```

### Rate Limiting

Default: 200 requests per minute per service instance.

Configure in `application.yml`:
```yaml
app:
  rate-limit:
    requests-per-minute: 200
```

## 📊 Database Schema

See `docs/ddl/` for complete DDL scripts.

### Key Tables

**Order Service:**
- `orders` - Order headers with status tracking
- `order_items` - Order line items

**Inventory Service:**
- `products` - Product catalog with stock levels

## 🏢 Production Scaling Strategy

### Horizontal Scaling

1. **Order Service**: Scale to 3+ pods behind load balancer
2. **Inventory Service**: Scale to 2+ pods (fewer due to locking)
3. **Kafka**: 3-broker cluster with replication

### Database Optimization

1. **Read Replicas**: Route read queries to replicas
2. **Connection Pooling**: HikariCP with optimized settings
3. **Indexes**: Pre-created for common query patterns

### Deploy with Docker

## 📝 Design Patterns Used

| Pattern | Implementation |
|---------|----------------|
| **Saga** | Distributed transaction for order placement |
| **Repository** | Spring Data JPA repositories |
| **Factory** | Event creation with factory methods |
| **Circuit Breaker** | Resilience4j for inter-service calls |
| **Retry** | Exponential backoff for transient failures |

## 🔒 Error Handling

All errors follow a consistent format:

```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_INVENTORY",
    "message": "Insufficient inventory for SKU 'LAPTOP-001'",
    "details": {
      "sku": "LAPTOP-001",
      "requested": 100,
      "available": 50
    },
    "traceId": "abc123"
  },
  "timestamp": "2026-02-02T18:00:00Z"
}
```

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INSUFFICIENT_INVENTORY` | 400 | Not enough stock |
| `ORDER_NOT_FOUND` | 404 | Order doesn't exist |
| `PRODUCT_NOT_FOUND` | 404 | Product doesn't exist |
| `INVALID_ORDER_STATE` | 400 | Invalid status transition |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `SERVICE_UNAVAILABLE` | 503 | Dependency service down |

