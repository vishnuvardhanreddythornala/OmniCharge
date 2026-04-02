# OmniCharge Microservices Ecosystem

OmniCharge is a robust, cloud-native telecom recharge and payment processing platform. Built using Spring Boot and following a strict microservices architecture, it leverages Netflix Eureka for service discovery, Spring Cloud Config for centralized configurations, RabbitMQ for asynchronous event-driven sagas, and the ELK stack for enterprise-grade observability.

---

## 🏛️ Architecture Overview

The system is split into independent domains, securely communicating via an API Gateway and internally executing cross-domain workflows via sagas.

### Core Services
- **API Gateway (`:8080`)**: Entry point for all clients. Implements reactive routing, JWT authentication filtering, and CORS policies.
- **Config Server (`:8888`)**: Centralized configuration management reading from `file:./config-repo`.
- **Discovery Server (Eureka) (`:8761`)**: Service registry for dynamic load balancing and resolution.
- **User Service**: Manages customer profiles, registration, and JWT token generation (including Google Auth OAuth2 integrations).
- **Operator Service**: Maintains telecom operators and dynamically handles complex plan caching using Redis.
- **Recharge Service**: Initiates the telecom recharge saga flow.
- **Payment Service**: Validates payments uniquely identifying Razorpay transactions before validating the recharge saga.
- **Notification Service**: Listens to RabbitMQ events to dispatch Email/SMS notifications asynchronously.
- **Logging Service**: Listens to RabbitMQ for raw service logs and pipes complex logging events into Logstash for ELK monitoring.

### Infrastructure (Dockerized)
- **MySQL (8.0)**: Relational data store shared systematically across independent logical schemas.
- **Redis (7.0)**: Used heavily for operator plan caching and distributed state.
- **RabbitMQ**: The messaging backbone handling saga logic and log streaming.
- **ELK Stack**: Elasticsearch, Logstash, and Kibana for centralized monitoring and observability.
- **Zipkin & Prometheus/Grafana**: Distributed tracing and metrics infrastructure.

---

## 🚀 Local Development Setup

### Prerequisites
- Docker & Docker Compose (Ensure Docker Desktop allocates **8GB - 12GB of RAM**).
- Maven 3.8+ (For manual compilation).
- Java 17.

### 1. Configure the Environment
An example environment file is provided. You **must** duplicate it to `.env` and fill out your specific credentials before spinning up.
```bash
cp .env.example .env
```

### 2. Startup via Docker
The entire system is orchestrated inside `docker-compose.yml`. You can cleanly start everything using:
```bash
docker-compose up -d --build
```
*Note: The Config Server maps to `./config-repo`. All changes to properties take effect dynamically without container rebuilds.*

## 🧪 Postman Automation
The full end-to-end user flows, admin operations, and payment triggers are mapped in the `docs/Omnicharge_postman_collection.json` file.
1. Import into Postman.
2. Ensure your Environment has `baseUrl` set to `http://localhost:8080`.
3. Simply execute **Register** then **Login**. The JWT tokens are natively extracted and utilized across all subsequent requests via automated scripts.

## 🔐 Security Information
- All internal operational ports (DBs, ELK, Config, Eureka) are **strictly bound to 127.0.0.1** to prevent external network brute-forcing. They form an isolated internal docker network (`omnicharge-net`).
- API Gateway strictly validates inbound headers and destroys malicious injections natively.

---
*Built organically for highly concurrent telecom operations.*
