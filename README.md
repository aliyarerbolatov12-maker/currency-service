````markdown
# Currency Service

A Spring Boot backend service for collecting, storing, caching, and querying exchange rates from external providers.

The service supports:

- Exchange rate synchronization from external providers
- Historical exchange rate queries
- Cross-currency rate calculation
- Latest exchange rate queries
- Redis caching
- PostgreSQL persistence
- Scheduled provider updates
- Resilience4j Circuit Breaker and Retry
- Request validation
- Global exception handling using RFC 9457 `ProblemDetail`
- Flyway database migrations
- Unit and integration testing
- JaCoCo code coverage

---

## Tech Stack

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Data JPA / Hibernate
- PostgreSQL 15
- Redis 7
- Flyway
- Spring Cache
- Spring AOP
- Spring Cloud OpenFeign
- Resilience4j
- MapStruct
- Lombok
- Maven
- Docker / Docker Compose
- JUnit 5 / Mockito
- JaCoCo
- Swagger / OpenAPI

---

## Architecture

The application follows a layered architecture:

```text
Controller
    ↓
Service
    ↓
Reader / Write Services
    ↓
Repository
    ↓
PostgreSQL

External Providers
    ↓
Provider Layer
    ↓
ExchangeRateService
    ↓
PostgreSQL

ExchangeRateReaderService
    ↓
Redis Cache
    ↓
PostgreSQL
````

The application separates read and write operations:

* `ExchangeRateService` — application/business logic
* `ExchangeRateReaderService` — read operations and caching
* `ExchangeRateWriteService` — transactional persistence
* `ExchangeRateRepository` — database access
* `ExchangeRateProvider` — external exchange-rate providers

---

# Features

## Exchange Rate Providers

The service supports exchange-rate providers through the `ExchangeRateProvider` abstraction.

Currently configured providers include:

* Frankfurter
* Coinbase

Providers can be enabled or disabled through configuration.

Example:

```yaml
exchange-rate:
  providers:
    frankfurter:
      cron: "0 0 20 * * *"
      enabled: true
```

Each enabled provider has its own cron schedule.

---

## Scheduled Updates

Exchange rates are automatically updated using Spring's scheduling infrastructure.

The scheduler dynamically registers tasks based on provider configuration.

Example:

```yaml
exchange-rate:
  providers:
    frankfurter:
      cron: "0 0 20 * * *"
      enabled: true
```

This allows provider schedules to be configured without changing the scheduler implementation.

---

# Cross Rates

The service can calculate exchange rates between currencies when a direct pair is not available.

USD is used as the bridge currency.

For example, if the database contains:

```text
USD → EUR = 0.85
USD → AUD = 1.42
```

the service can calculate:

```text
EUR → AUD = 1.42 / 0.85
```

The result is returned as a computed exchange rate.

---

# Caching

Redis is used to cache frequently requested exchange rates.

Cached operations include:

* Latest rate for a currency pair
* Most recent exchange rate

Cache keys are defined in:

```text
CacheNames
```

Example cache names:

```text
exchange-rate:latest
exchange-rate:most-recent
```

Spring Cache is used together with Redis.

---

# Resilience

External provider calls use Resilience4j.

The application provides:

### Circuit Breaker

The default Circuit Breaker configuration includes:

```yaml
slidingWindowSize: 10
failureRateThreshold: 50
waitDurationInOpenState: 10s
permittedNumberOfCallsInHalfOpenState: 3
```

### Retry

Retry configuration includes:

```yaml
maxAttempts: 3
waitDuration: 300ms
enableExponentialBackoff: true
exponentialBackoffMultiplier: 2
```

Retries are configured for transient errors such as:

* `IOException`
* `SocketTimeoutException`
* `TimeoutException`
* `ResourceAccessException`
* `HttpServerErrorException`

---

# Error Handling

The application uses a global `@RestControllerAdvice`.

Errors are returned using Spring's `ProblemDetail`.

Example:

```json
{
  "type": "about:blank",
  "title": "Exchange rate not found",
  "status": 404,
  "detail": "Exchange rate not found for EUR/AUD",
  "path": "/api/v1/rates/latest/pair",
  "timestamp": "2026-09-28T10:00:00Z"
}
```

Validation errors contain an additional `errors` object:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more request fields are invalid.",
  "path": "/api/v1/rates/latest/pair",
  "timestamp": "2026-09-28T10:00:00Z",
  "errors": {
    "base": "must not be blank",
    "quote": "must not be blank"
  }
}
```

---

# API

The complete API documentation is available through Swagger UI.

After starting the application:

```text
http://localhost:8080/swagger-ui/index.html
```

The main API base path is:

```text
/api/v1/rates
```

## Update Exchange Rates

Triggers an update from all enabled providers.

```http
POST /api/v1/rates/update
```

Example:

```bash
curl -X POST http://localhost:8080/api/v1/rates/update
```

---

## Get Historical Rates

Returns historical exchange rates for a currency within a specified date range.

```http
GET /api/v1/rates/history
```

Example:

```text
GET /api/v1/rates/history?currency=EUR&start=2026-09-01T00:00:00Z&end=2026-09-28T00:00:00Z
```

Optional pagination:

```text
?page=0&size=20
```

Example:

```text
GET /api/v1/rates/history?currency=EUR&start=2026-09-01T00:00:00Z&end=2026-09-28T00:00:00Z&page=0&size=20
```

The maximum allowed history period is 365 days.

---

## Get Cross-Currency History

Calculates historical exchange rates between two currencies using USD as a bridge currency.

```http
GET /api/v1/rates/history/cross
```

Example:

```text
GET /api/v1/rates/history/cross?base=EUR&quote=AUD&start=2026-09-01T00:00:00Z&end=2026-09-28T00:00:00Z
```

For example:

```text
USD → EUR
USD → AUD
```

are used to calculate:

```text
EUR → AUD
```

Pagination is also supported:

```text
?page=0&size=20
```

---

## Get Latest Rate for a Currency Pair

Returns the latest available rate for a currency pair.

```http
GET /api/v1/rates/latest/pair
```

Example:

```text
GET /api/v1/rates/latest/pair?base=EUR&quote=AUD
```

If a direct rate is unavailable, the service calculates the cross rate using USD.

Example response:

```json
{
  "id": null,
  "base": "EUR",
  "quote": "AUD",
  "rate": 1.670588,
  "effectiveAt": "2026-09-27T00:00:00Z",
  "providerName": "computed"
}
```

---

## Get Latest Rates by Base Currency

Returns the latest rates for a specified base currency.

```http
GET /api/v1/rates/latest/base/{base}
```

Example:

```text
GET /api/v1/rates/latest/base/USD?limit=10
```

The `limit` parameter must be between `1` and `100`.

Example:

```text
GET /api/v1/rates/latest/base/USD?limit=20
```

---

## Get Most Recent Rate

Returns the most recently stored exchange rate.

```http
GET /api/v1/rates/latest/recent
```

Example:

```text
GET /api/v1/rates/latest/recent
```

---

# API Summary

| Method | Endpoint                           | Description                             |
|--------|------------------------------------|-----------------------------------------|
| `POST` | `/api/v1/rates/update`             | Update rates from all enabled providers |
| `GET`  | `/api/v1/rates/history`            | Get historical rates for a currency     |
| `GET`  | `/api/v1/rates/history/cross`      | Get historical cross rates              |
| `GET`  | `/api/v1/rates/latest/pair`        | Get latest rate for a currency pair     |
| `GET`  | `/api/v1/rates/latest/base/{base}` | Get latest rates by base currency       |
| `GET`  | `/api/v1/rates/latest/recent`      | Get the most recently stored rate       |

For request parameters and validation rules, use Swagger UI.

---

# Running Locally

## Prerequisites

Make sure the following are installed:

* Java 21
* Maven 3.9+
* Docker
* Docker Compose

---

## Environment Variables

Create a `.env` file in the project root.

Example:

```env
DB_USER=postgres
DB_PASSWORD=postgres
DB_PORT=5432

REDIS_PASSWORD=redis
REDIS_PORT=6380

SERVER_PORT=8080
```

Do not commit the `.env` file to Git.

Add it to `.gitignore`:

```gitignore
.env
```

---

# Running with Docker Compose

The project includes PostgreSQL, Redis, and the Spring Boot application.

Start everything:

```bash
docker compose up -d --build
```

Check running containers:

```bash
docker compose ps
```

View application logs:

```bash
docker compose logs -f app
```

View PostgreSQL logs:

```bash
docker compose logs -f postgres
```

View Redis logs:

```bash
docker compose logs -f redis
```

Stop the application:

```bash
docker compose down
```

Stop and rebuild everything:

```bash
docker compose down
docker compose up -d --build
```

---

# Docker Services

Docker Compose starts three services:

| Service                 | Container        | Default Port |
|-------------------------|------------------|--------------|
| Spring Boot application | `currency-app`   | `8080`       |
| PostgreSQL              | `currency-db`    | `5432`       |
| Redis                   | `currency-redis` | `6379`       |

The application communicates with PostgreSQL and Redis through the Docker network.

Inside Docker, the application connects to:

```text
postgres:5432
redis:6379
```

---

# Database

PostgreSQL is used as the primary persistent storage.

Database configuration:

```yaml
POSTGRES_DB: currency_db
```

Database schema changes are managed using Flyway migrations.

Flyway runs automatically when the application starts.

---

# Redis

Redis is used as the cache layer.

The Redis container requires authentication:

```yaml
command: redis-server --requirepass ${REDIS_PASSWORD}
```

The application connects to Redis using the configured password.

---

# Running Without Docker

If you want to run the application directly with Maven, PostgreSQL and Redis must be running separately.

Run:

```bash
mvn spring-boot:run
```

Or build the application:

```bash
mvn clean package
```

Then run:

```bash
java -jar target/currency-service-0.0.1-SNAPSHOT.jar
```

---

# Testing

The project uses:

* JUnit 5
* Mockito
* Spring Boot Test
* Spring test support
* JaCoCo

Tests are located under:

```text
src/test/java
```

Current test classes include:

```text
src/test/java/
└── com/aliyar/currency_service/
    ├── CurrencyServiceApplicationTests.java
    ├── provider/
    │   └── FrankfurterProviderTest.java
    └── scheduler/
        └── DynamicExchangeRateSchedulerTest.java
```

Run all tests:

```bash
mvn test
```

---

# Code Coverage

JaCoCo is configured for test coverage reporting.

Generate the report:

```bash
mvn clean test
```

The HTML report is generated at:

```text
target/site/jacoco/index.html
```

The current project coverage is approximately:

```text
Instruction coverage: 91%
Branch coverage:      95%
Line coverage:        87%
Method coverage:      95%
Class coverage:       94%
```

Coverage is measured after excluding:

* DTO classes
* Entity classes
* Mapper classes
* `CurrencyServiceApplication`

This keeps the report focused on application logic rather than generated or data-holder code.

---

# Project Structure

```text
src
├── main
│   ├── java
│   │   └── com.aliyar.currency_service
│   │       ├── config
│   │       ├── common
│   │       │   └── exception
│   │       ├── modules
│   │       │   ├── controller
│   │       │   ├── dto
│   │       │   ├── entity
│   │       │   ├── exception
│   │       │   ├── mapper
│   │       │   ├── repository
│   │       │   └── service
│   │       ├── provider
│   │       └── scheduler
│   │
│   └── resources
│       ├── application.yml
│       └── db
│           └── migration
│
└── test
    └── java
        └── com.aliyar.currency_service
```

---

# Configuration

Provider schedules and availability are configured in `application.yml`.

Example:

```yaml
exchange-rate:
  providers:

    frankfurter:
      cron: "0 0 20 * * *"
      enabled: true
```

This allows providers to be enabled/disabled and scheduled independently.

---

# Build

Build the project:

```bash
mvn clean package
```

Build without running tests:

```bash
mvn clean package -DskipTests
```

The Docker build uses the same approach:

```dockerfile
RUN mvn clean package -DskipTests
```

The resulting JAR is then copied into a lightweight Java 21 runtime image.

---

# Health and Troubleshooting

Check whether the containers are running:

```bash
docker compose ps
```

Check application logs:

```bash
docker compose logs -f app
```

Check PostgreSQL:

```bash
docker compose logs -f postgres
```

Check Redis:

```bash
docker compose logs -f redis
```

If the application cannot connect to PostgreSQL or Redis, make sure the corresponding containers are healthy:

```bash
docker compose ps
```

The application is configured to wait for healthy PostgreSQL and Redis containers before starting.

---

# Example Workflow

Start the application:

```bash
docker compose up -d --build
```

Trigger an exchange-rate update:

```bash
curl -X POST http://localhost:8080/api/v1/rates/update
```

Get the latest USD rates:

```text
http://localhost:8080/api/v1/rates/latest/base/USD?limit=10
```

Get the latest EUR/AUD rate:

```text
http://localhost:8080/api/v1/rates/latest/pair?base=EUR&quote=AUD
```

Get historical EUR/AUD cross rates:

```text
http://localhost:8080/api/v1/rates/history/cross?base=EUR&quote=AUD&start=2026-09-01T00:00:00Z&end=2026-09-28T00:00:00Z
```

Open Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

---

# Design Highlights

The project demonstrates several backend engineering practices:

* Layered architecture
* Separation of read and write services
* Transaction boundaries with `@Transactional`
* Database persistence with Spring Data JPA
* Redis caching with Spring Cache
* External API integration
* Provider abstraction
* Dynamic scheduled tasks
* Circuit Breaker and Retry patterns
* Cross-rate calculation
* Input validation
* Centralized exception handling
* RFC 9457 `ProblemDetail` responses
* Database migrations with Flyway
* Unit and integration testing
* Automated code coverage with JaCoCo
* Containerized deployment with Docker Compose