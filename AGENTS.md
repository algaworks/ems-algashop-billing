# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is the **Billing Service**, a Spring Boot microservice responsible for invoice generation, credit card management, and payment processing for the Algashop platform. It integrates with the Fastpay payment gateway and receives payment status callbacks via webhooks.

**Key Responsibilities:**
- Invoice generation (triggered by ordering service via M2M call)
- Payment processing (credit card and gateway balance via Fastpay)
- Credit card tokenization and management
- Webhook handling for asynchronous payment status updates from Fastpay

**Technology Stack:**
- Java 25, Gradle 9.2.1, Spring Boot 4.0.x
- PostgreSQL (with Flyway migrations)
- Spring Security OAuth2 (Resource Server)
- Spring Cloud Circuit Breaker (resilience for Fastpay calls)
- TestContainers (integration test databases)
- WireMock (Fastpay API stubbing in tests)

## Architecture

### Layered Clean Architecture

The codebase uses a clean/layered architecture with four explicit packages:

```
domain/          → Pure business logic (aggregates, domain services, outbound port interfaces)
application/     → Use cases (application services, input/output DTOs, SecurityChecks port)
infrastructure/  → Adapter implementations (persistence, Fastpay HTTP clients, security config)
presentation/    → REST controllers and exception handling
```

**Key Layers:**

1. **Domain Layer** (`domain/`)
    - **Aggregates:**
        - `Invoice` — Aggregate root. Factory method `Invoice.issue(...)`. State machine: UNPAID → PAID / CANCELED. Registers domain events (`InvoiceIssuedEvent`, `InvoicePaidEvent`, `InvoiceCanceledEvent`).
        - `CreditCard` — Aggregate root. Factory method `CreditCard.brandNew(...)`. Stores last 4 digits, brand, expiration, and Fastpay gateway token.
    - **Value Objects / Embeddables:** `Payer`, `Address`, `LineItem`, `PaymentSettings`
    - **Domain Services:**
        - `InvoicingService` — Enforces duplicate-order guard and payment assignment
    - **Outbound Port Interfaces (defined in domain):**
        - `PaymentGatewayService` — Capture payment, find by gateway code
        - `CreditCardProviderService` — Register, find by ID, delete
        - `InvoiceRepository`, `CreditCardRepository` — Spring Data JPA interfaces owned by domain
    - **Domain Events:** `InvoiceIssuedEvent`, `InvoicePaidEvent`, `InvoiceCanceledEvent`
    - **Enums:** `InvoiceStatus` (UNPAID, PAID, CANCELED), `PaymentMethod` (CREDIT_CARD, GATEWAY_BALANCE)
    - **Base class:** `AbstractAuditableAggregateRoot` — `@MappedSuperclass` with auditing fields (`@CreatedBy`, `@CreatedDate`, `@LastModifiedBy`, `@LastModifiedDate`, `@Version`)

2. **Application Layer** (`application/`)
    - **Application Services:**
        - `InvoiceManagementApplicationService` — `generate`, `processPayment`, `updatePaymentStatus`
        - `CreditCardManagementService` — `register` (via provider), `delete`
    - **Query Services:**
        - `InvoiceQueryService` — `findByOrderId`, `findByOrderIdAndCustomerId`
        - `CreditCardQueryService` — list and find credit cards
    - **Port Interface:** `SecurityChecks` — `getAuthenticatedUserId()`, `isAuthenticated()`, `isMachineAuthenticated()`
    - **DTOs:** `GenerateInvoiceInput`, `TokenizedCreditCardInput`, `InvoiceOutput`, `CreditCardOutput`, etc.

3. **Infrastructure Layer** (`infrastructure/`)
    - **Persistence:** `InvoiceQueryServiceImpl`, `CreditCardQueryServiceImpl` — implement query service interfaces via `InvoiceRepository`; `SpringDataAuditingConfig`, `HibernateNamingConfiguration`
    - **Payment Gateway:**
        - `PaymentGatewayServiceFastpayImpl` — implements `PaymentGatewayService` using Fastpay
        - `PaymentGatewayServiceFakeImpl` — stub for local development
        - `ResilientFastpayPaymentAPIClient` — circuit breaker + `@ConcurrencyLimit(10)` wrapper
        - `FastpayWebhookHandler` — converts Fastpay webhook events → `updatePaymentStatus` call
        - Selected via `@ConditionalOnProperty(algashop.integrations.payment.provider)`
    - **Credit Card Provider:**
        - `CreditCardProviderServiceFastpayImpl` — implements `CreditCardProviderService` via Fastpay
        - `CreditCardProviderServiceFakeImpl` — stub for local development
    - **Resilience:** `SpringCircuitBreakerConfig` — 3 retries, exponential backoff (2x, 3s delay, 30s open, 60s reset); excludes `FastpayPaymentCaptureFailed`
    - **Security:** `BillingSecurityConfig` (stateless JWT), `SecurityAnnotations`, `OAuth2SecurityChecksImpl`
    - **JWT:** `JwtAuthenticationConverterConfig`, `JwtGrantedAuthoritiesDelegatingConverter`
    - **Events:** `InvoiceEventListener` — Spring event listener for domain events
    - **Mapper:** `ModelMapperConfig`

4. **Presentation Layer** (`presentation/`)
    - `InvoiceController` — `POST/GET /api/v1/orders/{orderId}/invoice` (M2M: generate + payment capture)
    - `MyInvoiceController` — `GET /api/v1/customers/me/orders/{orderId}/invoice` (customer-scoped)
    - `MyCreditCardController` — `POST/GET/DELETE /api/v1/customers/me/credit-cards` (customer-scoped)
    - `FastpayWebhookController` — `POST /api/v1/webhooks/fastpay` (public, no auth)
    - `ApiExceptionHandler` — RFC 9457 `ProblemDetail` responses

### Security Model

- **OAuth2 Resource Server:** Validates incoming JWT tokens from authorization-server
- **Scope-based access with meta-annotations:**
    - `@CanGenerateInvoices` — `SCOPE_invoices:write` + machine-authenticated (no ROLE_CUSTOMER)
    - `@CanReadInvoices` — `SCOPE_invoices:read` + not ROLE_CUSTOMER
    - `@CanReadMyInvoices` — `SCOPE_invoices:read` + ROLE_CUSTOMER
    - `@CanReadMyCreditCards` — `SCOPE_credit-cards:read` + ROLE_CUSTOMER
    - `@CanWriteMyCreditCards` — `SCOPE_credit-cards:write` + ROLE_CUSTOMER

### Spring Profiles

The application uses layered profiles:
- `base` — Common configuration
- `development-env` — Local development overrides (port 8082, localhost DB, Fastpay on port 9995)
- `docker-env` — Docker Compose overrides (DB URLs, Fastpay host)
- `production-env` — Production settings (env-var-driven)

Activate via `SPRING_PROFILES_ACTIVE=docker` or in `application.yml`.

## Build Commands

```bash
cd microservices/billing

# Compile and run all tests (unit + integration)
./gradlew build

# Compile only
./gradlew classes

# Run unit tests only (*Test.java)
./gradlew test

# Run integration tests (*IT.java) — uses TestContainers
./gradlew integrationTest

# Run all test types
./gradlew check

# Build runnable JAR
./gradlew bootJar

# Build multi-platform Docker image (linux/arm64, linux/amd64)
./gradlew dockerBuild

# Run a single test class
./gradlew test --tests "com.algaworks.algashop.billing.domain.model.invoice.InvoiceTest"
```

## Running Locally

**Start infrastructure (Postgres, WireMock, Fastpay mock, etc.):**
```bash
cd ../..  # Go to monorepo root
docker compose -f docker-compose.tools.yml up -d
```

**Run the application:**
```bash
# From billing directory
./gradlew bootRun

# With specific profile
SPRING_PROFILES_ACTIVE=docker ./gradlew bootRun
```

The server starts on **port 8082** (configured in `application-development-env.yml`).

**Fastpay integration config (development):**
- Fastpay hostname: `http://localhost:9995`
- Webhook callback URL: `http://host.docker.internal:8082/api/v1/webhooks/fastpay`
- Private token: configured in `application-development-env.yml`

## Project Structure

```
src/main/java/com/algaworks/algashop/billing/
├── BillingApplication.java
├── domain/
│   ├── model/
│   │   ├── AbstractAuditableAggregateRoot.java  # Base class with auditing + version
│   │   ├── DomainException.java
│   │   ├── DomainEntityNotFoundException.java
│   │   ├── FieldValidations.java
│   │   ├── IdGenerator.java
│   │   ├── creditcard/
│   │   │   ├── CreditCard.java                  # Aggregate root
│   │   │   ├── CreditCardRepository.java        # Spring Data JPA port (owned by domain)
│   │   │   ├── CreditCardProviderService.java   # Outbound port (register/find/delete)
│   │   │   ├── LimitedCreditCard.java
│   │   │   └── CreditCardNotFoundException.java
│   │   └── invoice/
│   │       ├── Invoice.java                     # Aggregate root
│   │       ├── InvoiceRepository.java           # Spring Data JPA port (owned by domain)
│   │       ├── InvoicingService.java            # Domain service
│   │       ├── InvoiceStatus.java               # Enum: UNPAID, PAID, CANCELED
│   │       ├── PaymentMethod.java               # Enum: CREDIT_CARD, GATEWAY_BALANCE
│   │       ├── PaymentSettings.java
│   │       ├── LineItem.java
│   │       ├── Payer.java
│   │       ├── Address.java
│   │       ├── InvoiceIssuedEvent.java
│   │       ├── InvoicePaidEvent.java
│   │       ├── InvoiceCanceledEvent.java
│   │       ├── InvoiceNotFoundException.java
│   │       └── payment/
│   │           ├── Payment.java
│   │           ├── PaymentGatewayService.java   # Outbound port (capture/findByCode)
│   │           ├── PaymentRequest.java
│   │           └── PaymentStatus.java
├── application/
│   ├── creditcard/
│   │   ├── management/
│   │   │   ├── CreditCardManagementService.java
│   │   │   └── TokenizedCreditCardInput.java
│   │   └── query/
│   │       ├── CreditCardQueryService.java
│   │       └── CreditCardOutput.java
│   ├── invoice/
│   │   ├── management/
│   │   │   ├── InvoiceManagementApplicationService.java
│   │   │   ├── GenerateInvoiceInput.java
│   │   │   ├── LineItemInput.java
│   │   │   ├── PayerData.java
│   │   │   ├── AddressData.java
│   │   │   └── PaymentSettingsInput.java
│   │   └── query/
│   │       ├── InvoiceQueryService.java
│   │       ├── InvoiceOutput.java
│   │       ├── LineItemOutput.java
│   │       └── PaymentSettingsOutput.java
│   ├── security/
│   │   └── SecurityChecks.java                  # Port: getAuthenticatedUserId, isMachineAuthenticated
│   └── utility/
│       └── Mapper.java
├── infrastructure/
│   ├── creditcard/
│   │   ├── fake/ CreditCardProviderServiceFakeImpl.java
│   │   └── fastpay/
│   │       ├── CreditCardProviderServiceFastpayImpl.java
│   │       ├── FastpayCreditCardAPIClient.java
│   │       └── FastpayCreditCardAPIClientConfig.java
│   ├── listener/
│   │   └── InvoiceEventListener.java
│   ├── locale/
│   │   └── FixedLocaleConfig.java
│   ├── payment/
│   │   ├── AlgaShopPaymentProperties.java
│   │   ├── fake/ PaymentGatewayServiceFakeImpl.java
│   │   └── fastpay/
│   │       ├── PaymentGatewayServiceFastpayImpl.java
│   │       ├── ResilientFastpayPaymentAPIClient.java    # Circuit breaker + @ConcurrencyLimit(10)
│   │       ├── FastpayPaymentAPIClient.java
│   │       └── webhook/
│   │           ├── FastpayWebhookController.java
│   │           ├── FastpayWebhookHandler.java
│   │           └── FastpayPaymentWebhookEvent.java
│   ├── persistence/
│   │   ├── HibernateNamingConfiguration.java
│   │   ├── SpringDataAuditingConfig.java
│   │   ├── creditcard/ CreditCardQueryServiceImpl.java
│   │   └── invoice/    InvoiceQueryServiceImpl.java
│   ├── resilience/
│   │   └── SpringCircuitBreakerConfig.java
│   ├── security/
│   │   ├── BillingSecurityConfig.java
│   │   ├── SecurityAnnotations.java              # Meta-annotations: @CanGenerateInvoices, etc.
│   │   └── check/ OAuth2SecurityChecksImpl.java
│   ├── token/
│   │   ├── JwtAuthenticationConverterConfig.java
│   │   └── JwtGrantedAuthoritiesDelegatingConverter.java
│   └── utility/
│       └── mapper/ ModelMapperConfig.java
└── presentation/
    ├── InvoiceController.java          # POST/GET /api/v1/orders/{orderId}/invoice (M2M)
    ├── MyInvoiceController.java        # GET /api/v1/customers/me/orders/{orderId}/invoice
    ├── MyCreditCardController.java     # POST/GET/DELETE /api/v1/customers/me/credit-cards
    ├── ApiExceptionHandler.java
    ├── BadGatewayException.java
    ├── GatewayTimeoutException.java
    └── UnprocessableEntityException.java
```

## Database

Migrations run automatically on startup via **Flyway**.

**Current migrations:**
- V001: Creates `invoice`, `invoice_line_item`, `payment_settings`, and `credit_card` tables. All PKs are UUID. Indexes on `customer_id` and `order_id` on `invoice`.

**To add a migration:**
1. Create `src/main/resources/db/migration/V{n}__description.sql`
2. Use snake_case for table/column names
3. Test locally: restart the application (Flyway executes on startup)
4. Ensure migrations are backward-compatible (avoid dropping columns)

## Testing

Test naming convention enforced by Gradle: `*Test.java` = unit tests, `*IT.java` = integration tests.

```
src/test/java/com/algaworks/algashop/billing/
├── domain/model/invoice/
│   └── InvoiceTest.java                          # Unit tests for Invoice aggregate (10 tests)
├── application/invoice/
│   ├── InvoiceManagementApplicationServiceIT.java  # Full Spring context + Testcontainers + WireMock
│   └── InvoiceQueryServiceIT.java
├── infrastructure/creditcard/
│   └── CreditCardProviderServiceFastpayImplIT.java # WireMock stubs for Fastpay
└── infrastructure/payment/
    └── PaymentGatewayServiceFastpayImplIT.java     # WireMock stubs for Fastpay
```

### Test Types

- **Unit tests** (`*Test.java`): Pure domain logic, no Spring context needed
    - Example: `InvoiceTest` tests aggregate state transitions directly

- **Integration tests** (`*IT.java`): Full or partial Spring context with real Postgres via TestContainers
    - `AbstractApplicationIT` — base class: `@SpringBootTest(NONE)`, `@Transactional`, Testcontainers PostgreSQL (`postgres:17-alpine`)
    - Mock `JwtDecoder`, `SecurityChecks`, external gateway (`PaymentGatewayService`) when needed
    - `AbstractFastpayIT` — base class for Fastpay HTTP adapter tests: WireMock on port 8788

### WireMock Stubs

Fastpay API stubs are at `src/test/resources/wiremock/fastpay/mappings/fastpay.json`. They cover:
- Payment capture (credit card and gateway balance)
- Payment lookup
- Credit card tokenization, find by ID, list, delete
- Refund and cancel

## Key Concepts

### Invoice State Machine

```
UNPAID → PAID
UNPAID → CANCELED
```

Transitions are guarded methods on the `Invoice` aggregate. Once PAID or CANCELED, no further transitions are allowed.

### Domain Events

Published by `Invoice` aggregate (extends Spring's `AbstractAggregateRoot`):
- `InvoiceIssuedEvent` — invoice created
- `InvoicePaidEvent` — payment captured successfully
- `InvoiceCanceledEvent` — payment failed, invoice canceled

`InvoiceEventListener` handles these events for side effects.

### Payment Flow

1. Ordering service calls `POST /api/v1/orders/{orderId}/invoice` (M2M with `SCOPE_invoices:write`)
2. `InvoiceManagementApplicationService.generate(...)` creates and issues the invoice
3. `processPayment(...)` is called immediately after: captures payment via Fastpay
4. If Fastpay responds asynchronously, `FastpayWebhookController` receives the callback
5. `updatePaymentStatus(...)` marks the invoice PAID or triggers cancellation

### Fastpay Integration

- **Conditional provider selection:** `@ConditionalOnProperty(name = "algashop.integrations.payment.provider", havingValue = "FASTPAY")` switches between fake and real implementations
- **Resilience:** `ResilientFastpayPaymentAPIClient` wraps `FastpayPaymentAPIClient` with circuit breaker (3 retries, exponential backoff). `FastpayPaymentCaptureFailed` is excluded from retry (business failure, not transient).
- **Webhook:** `POST /api/v1/webhooks/fastpay` is public (no auth). `FastpayWebhookHandler` maps Fastpay status codes to internal payment status.

## Dependencies & External Integrations

### Internal Services
- **Authorization Server** (port 8081) — OAuth2 token issuer; all requests validated against JWT tokens

### External Services (via HTTP clients)
- **Fastpay** (port 9995 in development, configured via `algashop.integrations.payment.fastpay.hostname`)
    - Credit card tokenization: `POST /tokenize`
    - Payment capture: `POST /payments`
    - Payment status: `GET /payments/{code}`
    - Webhook callback: `POST /api/v1/webhooks/fastpay` (inbound)
    - Circuit breaker + concurrency limit (max 10) configured

### Libraries
- **Spring Cloud Circuit Breaker + Framework Retry** — Resilience wrapper for Fastpay calls
- **TestContainers** — Real Postgres for integration tests
- **WireMock** — Fastpay HTTP stubbing in tests
- **ModelMapper** — DTO/Entity mapping
- **Lombok** — Boilerplate reduction
- **commons-lang3** — Utility helpers

## Common Tasks

### Generating an Invoice (via API)

```http
POST /api/v1/orders/{orderId}/invoice
Authorization: Bearer <machine-token with invoices:write>
Content-Type: application/json

{
  "customerId": "...",
  "payer": { ... },
  "lineItems": [ ... ],
  "paymentSettings": { "paymentMethod": "CREDIT_CARD", "creditCardId": "..." }
}
```

### Adding a New Invoice Endpoint
1. Add method to `InvoiceManagementApplicationService` or `InvoiceQueryService`
2. Create input/output DTOs in the appropriate `management/` or `query/` subpackage
3. Add endpoint to `InvoiceController` or `MyInvoiceController` with appropriate `@SecurityAnnotations`
4. Write unit test for domain logic if applicable
5. Write integration test in `InvoiceManagementApplicationServiceIT` for the full flow

### Adding a New Credit Card Operation
1. Add method to `CreditCardManagementService`
2. Update `CreditCardProviderService` port interface if a new Fastpay call is needed
3. Implement in `CreditCardProviderServiceFastpayImpl` and update `CreditCardProviderServiceFakeImpl`
4. Add WireMock stub to `src/test/resources/wiremock/fastpay/mappings/fastpay.json`
5. Add endpoint to `MyCreditCardController`

### Adding a Database Migration
1. Create `src/main/resources/db/migration/V{n}__description.sql`
2. In development, Flyway also runs `classpath:db/testdata` seed scripts (configured in `application-development-env.yml`)
3. In Docker/production, only `classpath:db/migration` is used

### Handling a New Fastpay Webhook Event
1. Add the new event type to `FastpayPaymentWebhookEvent`
2. Handle the mapping in `FastpayWebhookHandler`
3. Add corresponding WireMock stub for tests
4. Write integration test for the new event flow
