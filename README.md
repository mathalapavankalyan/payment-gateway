Payment Gateway

A production-oriented payment gateway backend built with Java 17, Spring Boot, MySQL, Docker, and AWS.

The project focuses on the engineering problems that make payment systems different from ordinary CRUD applications: idempotency, transactional consistency, concurrency, state management, reliable event processing, security, observability, CI/CD, and cloud deployment.

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/MySQL-8-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker">
  <img src="https://img.shields.io/badge/AWS-ECR%20%7C%20IAM-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white" alt="AWS">
  <img src="https://img.shields.io/badge/GitHub%20Actions-CI%2FCD-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" alt="GitHub Actions">
</p>

1. Overview

The service provides REST APIs for order and payment operations.

The core design principle is:

A payment request must be safe to retry without accidentally creating a second payment.

The system is being developed incrementally, with each infrastructure or distributed-systems concept introduced because of a concrete engineering requirement.

2. Key Features

Payment Domain

Order creation and lifecycle management

Payment creation and retrieval

Payment state management

Supported payment types:

UPI

CARD

NET_BANKING

Order-to-payment relationship

Reliability

Idempotency keys

Request hashing using SHA-256

Database uniqueness constraints

Transactional payment processing

Concurrent request protection

Domain-specific exception handling

Retry-safe payment operations

Production Engineering

Payment provider abstraction

Transactional Outbox pattern

Event-driven architecture

Kafka integration

Redis where justified

Structured logging

Correlation IDs

Metrics

Health checks

Integration testing

Testcontainers

Delivery & Cloud

Docker containerization

GitHub Actions CI/CD

GitHub OIDC authentication

AWS IAM

Amazon ECR

Terraform

AWS container deployment

3. Architecture

flowchart LR
    Client["Client"] --> Gateway["API Gateway"]
    Gateway --> Payment["Payment Service"]

    Payment --> MySQL[("MySQL")]
    Payment --> Redis[("Redis")]
    Payment --> Outbox["Transactional Outbox"]

    Outbox --> Kafka["Kafka"]

    Kafka --> Notification["Notification Service"]
    Kafka --> Audit["Audit / Analytics"]

The core payment service remains responsible for payment-domain consistency, while asynchronous operations are moved toward event-driven processing where eventual consistency is acceptable.

4. Payment Processing Flow

The payment creation flow is designed to make payment requests safe to retry while maintaining consistent order and payment state.

flowchart TD
    A["POST /api/v1/payments"] --> B["Validate Request"]
    B --> C["Read Idempotency-Key"]
    C --> D["Generate Request Hash"]
    D --> E{"Idempotency Key Exists?"}

    E -->|Yes| F["Compare Request Hash"]
    E -->|No| G["Create Idempotency Record<br/>PROCESSING"]

    F --> H{"Hash Matches?"}
    H -->|No| I["Reject Request<br/>IdempotencyKeyReuseException"]
    H -->|Yes| J{"Current Status"}

    J -->|COMPLETED| K["Return Existing Payment"]
    J -->|PROCESSING| L["Reject Duplicate Request"]

    G --> M["Load Order"]
    M --> N{"Order Can Accept Payment?"}

    N -->|No| O["Reject Request<br/>InvalidOrderStateException"]
    N -->|Yes| P["Create Payment"]

    P --> Q["Set Payment Status<br/>INITIATED"]
    Q --> R["Move Order to<br/>PAYMENT_PENDING"]
    R --> S["Persist Payment"]
    S --> T["Mark Idempotency Record<br/>COMPLETED"]
    T --> U["Return Payment Response"]

Flow Summary

The client sends a payment request with an Idempotency-Key.

The service generates a deterministic SHA-256 request hash.

The idempotency record is checked.

If the key already exists, the request hash and current processing state are evaluated.

A new request claims the key with PROCESSING status.

The order is loaded and its current state is validated.

A payment is created and associated with the order.

The order moves to PAYMENT_PENDING.

The payment is persisted.

The idempotency operation is marked COMPLETED.

The payment response is returned.

5. Idempotency

Payment clients can retry requests because of network failures, timeouts, client retries, or infrastructure failures.

The API therefore requires an idempotency key:

POST /api/v1/payments
Idempotency-Key: payment-request-001

Same request + same key

sequenceDiagram
    participant C as Client
    participant P as Payment Service
    participant DB as MySQL

    C->>P: Payment + Key A
    P->>DB: Check Key A
    DB-->>P: Not Found
    P->>DB: Create Key A
    P->>DB: Create Payment
    P->>DB: Mark Key A COMPLETED
    P-->>C: Payment #1

    C->>P: Same Payment + Key A
    P->>DB: Check Key A
    DB-->>P: Key A / Payment #1
    P-->>C: Payment #1

The retry returns the previously created payment instead of creating another payment.

Same key + different request

The request is hashed before processing.

Request fields
      |
      v
Canonical request representation
      |
      v
SHA-256
      |
      v
Request hash

If the same idempotency key is reused with a different request hash, the request is rejected.

6. Concurrency Protection

Application-level checks alone are not sufficient.

Two concurrent requests could both observe that an idempotency key does not exist before either request inserts it.

The database therefore enforces:

idempotency_key UNIQUE

sequenceDiagram
    participant A as Request A
    participant B as Request B
    participant DB as MySQL

    A->>DB: INSERT idempotency key
    B->>DB: INSERT same key

    DB-->>A: Insert succeeds
    DB-->>B: Unique constraint violation

    A->>DB: Process payment
    B-->>B: Treat as duplicate request

The database constraint is the final correctness boundary.

Locking is introduced only when a concrete business requirement demonstrates that it is necessary.

7. Transaction Management

Payment creation modifies multiple related pieces of state:

flowchart LR
    T["Transaction"] --> I["Idempotency Record"]
    T --> O["Order"]
    T --> P["Payment"]

    I --> I1["PROCESSING"]
    I1 --> I2["COMPLETED"]

    O --> O1["PAYMENT_PENDING"]

    P --> P1["INITIATED"]

The business operation is coordinated using a Spring transaction at the service layer.

The objective is to avoid partial state changes such as:

Payment saved
+
Order update failed

or:

Order updated
+
Payment save failed

8. Domain Model

Order

Order
├── orderId
├── quantity
├── amount
├── currency
├── orderStatus
└── payments

Order lifecycle:

stateDiagram-v2
    [*] --> CREATED
    CREATED --> PAYMENT_PENDING
    PAYMENT_PENDING --> PAID
    CREATED --> CANCELLED
    PAYMENT_PENDING --> CANCELLED

Payment

Payment
├── paymentId
├── order
├── paymentType
├── paymentStatus
├── paymentAmount
└── currency

Target payment lifecycle:

stateDiagram-v2
    [*] --> INITIATED
    INITIATED --> PROCESSING
    PROCESSING --> SUCCESS
    PROCESSING --> FAILED

Idempotency Record

IdempotencyRecord
├── id
├── idempotencyKey
├── requestHash
├── paymentId
├── status
└── createdAt

9. Payment Provider Abstraction

The core payment service should not be tightly coupled to a specific external payment provider.

classDiagram
    class PaymentService
    class PaymentProvider {
        <<interface>>
        +processPayment()
        +getPaymentStatus()
    }

    class UpiProvider
    class CardProvider
    class NetBankingProvider

    PaymentService --> PaymentProvider
    PaymentProvider <|.. UpiProvider
    PaymentProvider <|.. CardProvider
    PaymentProvider <|.. NetBankingProvider

Provider-specific concerns such as request mapping, response mapping, timeouts, retries, and provider errors remain at the integration boundary.

10. Reliable Event Processing

Asynchronous operations should not compromise the consistency of the payment transaction.

The target event flow is:

flowchart LR
    Payment["Payment Transaction"] --> DB[("MySQL")]
    Payment --> Outbox["Outbox Event"]
    Outbox --> Publisher["Event Publisher"]
    Publisher --> Kafka["Kafka"]

    Kafka --> Notification["Notification"]
    Kafka --> Audit["Audit / Analytics"]
    Kafka --> Other["Other Consumers"]

The Transactional Outbox pattern addresses the dual-write problem.

Without an outbox:

Database transaction succeeds
        +
Message publishing fails
        =
Database state changed
but event was lost

With an outbox, the domain change and event record are persisted as part of the same database transaction.

11. Event-Driven Architecture

Payment state changes can produce events such as:

PaymentInitiated
PaymentProcessing
PaymentSucceeded
PaymentFailed

Consumers must be designed to tolerate:

Retries

Duplicate delivery

Temporary downstream failures

Out-of-order processing where applicable

This keeps asynchronous consumers independent from the synchronous payment transaction.

12. Persistence

The application uses:

MySQL

Spring Data JPA

Hibernate

Persistence responsibilities include:

Entity relationships

Enum persistence

Transaction boundaries

Unique constraints

Repository abstraction

Database-generated identifiers

Domain state persistence

Example configuration:

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/payment_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

Credentials are supplied through environment variables and are not committed to source control.

13. API Documentation

Create Order

POST /api/v1/orders
Content-Type: application/json

Request:

{
  "quantity": 2,
  "amount": 1000.00,
  "currency": "INR"
}

Response:

{
  "orderId": 1,
  "quantity": 2,
  "amount": 1000.00,
  "currency": "INR",
  "orderStatus": "CREATED"
}

Create Payment

POST /api/v1/payments
Content-Type: application/json
Idempotency-Key: payment-request-001

Request:

{
  "orderId": 1,
  "paymentType": "UPI"
}

Response:

{
  "paymentId": 1,
  "orderId": 1,
  "paymentType": "UPI",
  "paymentStatus": "INITIATED",
  "amount": 1000.00,
  "currency": "INR"
}

Get Payment

GET /api/v1/payments/{paymentId}

Example:

GET /api/v1/payments/1

14. Error Handling

The service uses domain-specific exceptions rather than exposing persistence or framework errors directly.

Examples:

OrderNotFoundException
PaymentNotFoundException
InvalidOrderStateException
IdempotencyKeyReuseException
PaymentProcessingException

A centralized exception handler provides consistent HTTP responses.

Typical categories include:

HTTP Status

Meaning

400

Invalid request or business state

404

Order or payment not found

409

Conflicting/idempotency request

500

Unexpected server failure

15. Testing Strategy

Testing follows multiple levels.

flowchart TD
    Unit["Unit Tests"] --> Integration["Integration Tests"]
    Integration --> Container["Testcontainers"]
    Container --> CI["CI Pipeline"]

Unit Tests

Business behavior is tested independently, including:

Idempotency decisions

Order-state validation

Payment state transitions

Exception scenarios

Request hashing

Provider behavior

Integration Tests

Integration tests verify real interactions between:

Spring context

JPA

Repositories

Database

Transactions

Testcontainers

Testcontainers provides disposable infrastructure for integration tests where realistic dependencies are required.

16. Observability

Production systems require visibility into both successful and failed operations.

The service is designed to provide:

Structured logging

Correlation IDs

Request tracing

Application metrics

Health checks

Database health

External provider health

Payment-processing metrics

Request tracing follows the operation across components:

flowchart LR
    Request["Incoming Request"] --> API["API"]
    API --> Service["Payment Service"]
    Service --> DB[("Database")]
    Service --> Provider["Payment Provider"]
    Service --> Events["Event Pipeline"]

17. Security

Security considerations include:

Environment-based secrets

No credentials in Git

IAM least privilege

GitHub OIDC instead of long-lived AWS access keys

Restricted IAM trust policies

ECR permissions scoped to the required repository

Request validation

Idempotency-key validation

Secure external-provider communication

Authentication and authorization at the API boundary

18. Docker

The application is packaged as a Docker image.

flowchart LR
    Source["Source Code"] --> Maven["Maven Build"]
    Maven --> Jar["Spring Boot JAR"]
    Jar --> Docker["Docker Image"]
    Docker --> ECR["Amazon ECR"]

Build locally:

docker build -t payment-gateway .

Run locally:

docker run -p 8080:8080 \
  -e DB_USERNAME=your_username \
  -e DB_PASSWORD=your_password \
  payment-gateway

19. CI/CD

GitHub Actions automates application validation and Docker image publishing.

flowchart LR
    Push["Git Push"] --> Checkout["Checkout"]
    Checkout --> Java["Java 17"]
    Java --> Test["Run Tests"]
    Test --> Build["Build Application"]
    Build --> Docker["Build Docker Image"]
    Docker --> OIDC["GitHub OIDC"]
    OIDC --> STS["AWS STS"]
    STS --> IAM["IAM Role"]
    IAM --> ECR["Amazon ECR"]

The pipeline performs:

Source checkout

Java 17 setup

Maven tests

Maven package

AWS authentication through GitHub OIDC

ECR authentication

Docker image build

Docker image push

Images are tagged using the Git commit SHA:

payment-gateway:<commit-sha>

This makes each published image traceable to an exact source revision.

20. AWS Authentication

The CI/CD workflow uses GitHub's OIDC identity token to assume an AWS IAM role.

sequenceDiagram
    participant G as GitHub Actions
    participant STS as AWS STS
    participant IAM as IAM Role
    participant ECR as Amazon ECR

    G->>STS: OIDC token
    STS->>IAM: AssumeRoleWithWebIdentity
    IAM-->>STS: Temporary credentials
    STS-->>G: Temporary AWS credentials
    G->>ECR: Authenticate and push image

No long-lived AWS access keys are required in GitHub repository secrets.

The IAM trust policy is restricted to the intended repository and branch.

21. AWS Deployment

The target deployment architecture is:

flowchart TB
    GitHub["GitHub Repository"] --> Actions["GitHub Actions"]
    Actions --> ECR["Amazon ECR"]
    ECR --> ECS["ECS / Fargate"]

    ECS --> ALB["Application Load Balancer"]
    ECS --> MySQL[("MySQL")]
    ECS --> Redis[("Redis")]
    ECS --> Kafka["Kafka / Event Platform"]

The application is containerized so the same image can be promoted across environments.

22. Infrastructure as Code

Terraform is used to make infrastructure reproducible and version controlled.

Target infrastructure includes:

flowchart TB
    Terraform["Terraform"]

    Terraform --> IAM["IAM"]
    Terraform --> ECR["ECR"]
    Terraform --> VPC["VPC"]
    Terraform --> SG["Security Groups"]
    Terraform --> DB["Database"]
    Terraform --> ECS["ECS / Fargate"]
    Terraform --> ALB["Load Balancer"]
    Terraform --> Monitoring["Monitoring"]

Application code and infrastructure code remain logically separated while both are maintained under version control.

23. Redis

Redis is introduced only where low-latency shared state provides a clear engineering benefit.

Potential use cases:

Rate limiting

Short-lived caching

Distributed coordination where required

High-frequency read paths

The relational database remains authoritative for payment state.

24. Project Structure

payment-gateway/
│
├── .github/
│   └── workflows/
│       └── payment-gateway-ci.yml
│
├── src/
│   ├── main/
│   │   ├── java/com/pk/payment/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── enums/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   │       └── impl/
│   │   │
│   │   └── resources/
│   │       └── application.yml
│   │
│   └── test/
│       └── java/com/pk/payment/
│
├── Dockerfile
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md

25. Local Development

Prerequisites

Java 17

MySQL 8

Docker

Git

Create Database

CREATE DATABASE payment_db;

Configure Environment

Windows PowerShell

$env:DB_USERNAME="your_username"
$env:DB_PASSWORD="your_password"

Linux / macOS

export DB_USERNAME="your_username"
export DB_PASSWORD="your_password"

Run Application

Windows

mvnw.cmd spring-boot:run

Linux / macOS

./mvnw spring-boot:run

Application:

http://localhost:8080

26. Engineering Decisions

Idempotency uses both application logic and database constraints

The application validates the idempotency key and request hash, while the database provides the final uniqueness guarantee.

Transactions belong around business operations

The service layer owns the transaction because it represents the complete payment operation.

Database constraints are part of correctness

Database constraints protect business invariants even when multiple application instances process requests concurrently.

Locking is not added by default

Optimistic or pessimistic locking is introduced only when the domain demonstrates a real concurrent-update requirement.

External providers are isolated

Provider-specific integration code stays behind an abstraction so the payment domain does not become tightly coupled to a vendor.

Asynchronous operations use eventual consistency where appropriate

Notifications, analytics, and other downstream processing can be asynchronous without weakening the consistency of the core payment transaction.

Immutable image tags are preferred

Docker images are tagged with Git commit SHA values so a deployed image can always be traced back to source code.

27. Production Readiness Checklist

The project is being hardened across the following areas:

Core Payment

Order API

Payment API

Payment retrieval

Payment state management

Idempotency

Request hashing

Database uniqueness

Transaction management

Domain exceptions

Reliability

Payment provider integration

Retry strategy

Timeout handling

Provider failure handling

Transactional Outbox

Kafka event publishing

Consumer idempotency

Dead-letter handling

Testing

Unit test coverage

Integration tests

Testcontainers

Concurrency tests

End-to-end tests

Observability

Structured logging

Correlation IDs

Metrics

Health checks

Distributed tracing

Dashboards and alerts

Security

Environment-based credentials

No secrets committed to Git

IAM least privilege

GitHub OIDC

API authentication

API authorization

Secret management service

Infrastructure

Docker

GitHub Actions

Amazon ECR

Terraform

VPC

Load Balancer

ECS / Fargate

Managed database

Production monitoring

28. Development Approach

The project follows a problem-driven engineering approach:

flowchart LR
    Requirement["Requirement"] --> Design["Design"]
    Design --> Implementation["Implementation"]
    Implementation --> Test["Test"]
    Test --> Failure["Failure Analysis"]
    Failure --> Harden["Production Hardening"]
    Harden --> Requirement

Technologies are introduced because they solve specific engineering problems.

Problem

Engineering Solution

Duplicate payment requests

Idempotency

Concurrent key creation

Database uniqueness

Partial state changes

Transactions

Provider failures

Timeout / retry strategy

Reliable event delivery

Transactional Outbox

Asynchronous processing

Kafka

Low-latency shared state

Redis

Repeatable infrastructure

Terraform

Container distribution

Amazon ECR

Automated delivery

GitHub Actions

Secure CI/CD authentication

GitHub OIDC

29. Repository

GitHub:

https://github.com/mathalapavankalyan/payment-gateway

30. Author

Pavan Kalyan Mathala

Software Engineer | Backend Engineering

Primary areas of interest:

Java
Spring Boot
Microservices
REST APIs
AWS
Docker
Distributed Systems
Generative AI

Project Status

This repository is being developed as a complete production-oriented payment gateway.

The architecture and engineering decisions documented here represent the target final system. Features are implemented incrementally, tested, and hardened as the project progresses.
