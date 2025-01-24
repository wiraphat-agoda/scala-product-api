# Product Service API
A scalable product management service built with Scala, implementing caching strategies and event-driven architecture.

## Overview
This project demonstrates a modern approach to building microservices using Scala, featuring:
- RESTful API for product management
- Lazy loading and write-through caching with Redis
- Event streaming with Kafka
- PostgreSQL as main storage
- Functional programming with Cats Effect

## Technology Stack
### Core Technologies
- Scala 2.12.18
- PostgreSQL (Main database)
- Redis (Caching layer)
- Apache Kafka (Event streaming)

### Libraries
- Http4s 0.21.31 (HTTP server)
- Circe 0.13.0 (JSON handling)
- Doobie 0.13.4 (PostgreSQL interface)
- Cats Effect 2.2.0 (Effect system)
- PureConfig 0.17.2 (Configuration management)
- Jedis 4.4.3 (Redis client)

### Development Tools
- SBT (Build tool)
- Docker & Docker Compose
- PgAdmin 4 (PostgreSQL management)
- Redis Insight (Redis monitoring)
- Postman (API testing)

## Features
### API Endpoints
```http
# Products Resource
GET    /api/products      # Retrieve all products
POST   /api/products      # Create a new product
GET    /api/products/:id  # Retrieve a specific product
PUT    /api/products/:id  # Update a specific product
DELETE /api/products/:id  # Delete a specific product