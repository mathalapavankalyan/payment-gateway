Payment Gateway

A production-oriented payment gateway backend built with Java 17 and Spring Boot.

The project is being developed as a complete backend engineering exercise around the problems that make payment systems different from ordinary CRUD services: idempotency, transactional consistency, concurrent requests, payment state management, reliable event processing, security, observability, containerization, and cloud deployment.

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/MySQL-8-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>
  <img src="https://img.shields.io/badge/AWS-ECR%20%7C%20IAM%20%7C%20STS-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white" alt="AWS"/>
  <img src="https://img.shields.io/badge/GitHub%20Actions-CI%2FCD-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" alt="GitHub Actions"/>
</p>

1. Project Overview

The service exposes REST APIs for order and payment operations and is designed around the principle that a payment request must be safe to retry.

A simplified flow is:

Client
  |
  | Create Order
  v
Order Service
  |
  | Order CREATED
  v
Payment API
  |
  | Idempotency + Validation + Transaction
  v
Payment Service
  |
  +--------------------+
  |                    |
  v                    v
Order              Payment
  |                    |
  +---------+----------+
            |
            v
        Persistence

The project is intentionally built incrementally. Each production concern is introduced because of a concrete requirement rather than simply adding technologies to the stack.

2. Key Engineering Features

The final system is designed around the following capabilities:

RESTful payment APIs

Order lifecycle management

Payment lifecycle management

Idempotent payment creation

Idempotency-key reuse protection

Request hashing with SHA-256

Transactional payment processing

Database-level uniqueness constraints

Concurrent request protection

Domain-specific exception handling

Payment provider abstraction

Reliable event publishing

Transactional Outbox pattern

Event-driven processing

Redis-based capabilities where appropriate

Structured logging

Correlation IDs

Metrics and health checks

Integration testing

Testcontainers

Docker containerization

GitHub Actions CI/CD

AWS OIDC authentication

Amazon ECR

Infrastructure as Code

Automated cloud deployment

3. Architecture

High-Level Architecture

                         +----------------+
                         |     Client     |
                         +-------+--------+
                                 |
                                 v
                         +----------------+
                         |   API Gateway  |
                         +-------+--------+
                                 |
                                 v
                    +---------------------------+
                    |      Payment Service       |
                    |                           |
                    |  REST Controllers         |
                    |  Business Services        |
                    |  Validation               |
                    |  Transactions             |
                    |  Idempotency              |
                    |  State Management          |
                    +-------------+-------------+
                                  |
              +-------------------+-------------------+
              |                   |                   |
              v                   v                   v
         +---------+         +---------+         +---------+
         |  MySQL  |         |  Redis  |         |  Kafka  |
         +---------+         +---------+         +----+----+
                                                    |
                                                    v
                                           +----------------+
                                           | Event Consumers|
                                           +----------------+

The exact infrastructure topology is kept separate from the core business domain so that persistence, messaging, and deployment concerns can evolve independently.

4. Payment Processing Flow

POST /api/v1/payments
          |
          v
Validate Request
          |
          v
Read Idempotency-Key
          |
          v
Generate Request Hash
          |
          v
Check Existing Idempotency Record
          |
     +----+----+
     |         |
   Exists     New
     |         |
     v         v
Validate    Claim Key
Hash        as PROCESSING
     |         |
     |         v
     |     Load Order
     |         |
     |         v
     |     Validate Order State
     |         |
     |         v
     |    Create Payment
     |         |
     |         v
     |    Update Order
     |         |
     |         v
     |    Persist Changes
     |         |
     |         v
     |   Mark COMPLETED
     |         |
     +----+----+
          |
          v
   Return Payment

5. Idempotency

Payment APIs must tolerate retries.

A client may retry a request because of:

Network failures

Connection timeouts

Client-side retries

Load balancer retries

Temporary service failures

The API therefore requires an idempotency key:

POST /api/v1/payments
Idempotency-Key: payment-request-001

Same request, same key

Request #1
    |
    v
Process Payment
    |
    v
Store Result

Request #2
    |
    v
Find Existing Key
    |
    v
Return Existing Payment

The same payment is returned instead of creating another payment.

Same key, different request

The service generates a SHA-256 request hash.

Request
  |
  v
Relevant Request Fields
  |
  v
SHA-256
  |
  v
Request Hash

If an existing idempotency key is reused with different request data, the request is rejected.

This prevents a client from accidentally using one key for multiple payment operations.

6. Concurrency

An application-level existence check is not enough.

Two concurrent requests could both observe:

Idempotency key does not exist

before either request inserts the record.

The database therefore provides the final uniqueness guarantee:

idempotency_key UNIQUE

Conceptually:

Request A                    Request B
    |                            |
    | Check key                  | Check key
    |                            |
    +------------+---------------+
                 |
                 v
          Both attempt INSERT
                 |
          +------+------+
          |             |
          v             v
       Success        Constraint
                      violation

The application handles the database constraint violation and treats the losing request as an already-processing operation.

Locking is introduced only where the business requirements actually justify it.

7. Transaction Management

Payment creation modifies multiple pieces of state:

Idempotency Record
        |
        +--> PROCESSING
        |
        +--> COMPLETED

Order
        |
        +--> PAYMENT_PENDING

Payment
        |
        +--> INITIATED

These changes are coordinated inside a Spring transaction.

The goal is to prevent states such as:

Payment saved
    +
Order not updated

or:

Order updated
    +
Payment not saved

The transaction boundary therefore sits at the service layer around the business operation.

8. Domain Model

Order

Order
├── orderId
├── quantity
├── amount
├── currency
├── orderStatus
└── payments

Order states:

CREATED
   |
   v
PAYMENT_PENDING
   |
   v
PAID

CANCELLED

Payment processing is allowed only when the order is in a valid state.

Payment

Payment
├── paymentId
├── order
├── paymentType
├── paymentStatus
├── paymentAmount
└── currency

Supported payment types:

UPI
CARD
NET_BANKING

The payment lifecycle is designed to evolve toward:

INITIATED
    |
    v
PROCESSING
   / \
  v   v
SUCCESS  FAILED

Idempotency Record

IdempotencyRecord
├── id
├── idempotencyKey
├── requestHash
├── paymentId
├── status
└── createdAt

9. Payment Provider Abstraction

The business service should not be tightly coupled to a particular payment provider.

The target abstraction is:

                 PaymentService
                       |
                       v
                PaymentProvider
                 /      |      \
                /       |       \
               v        v        v
             UPI      Card    Net Banking
           Provider  Provider   Provider

This allows external payment providers to be integrated without changing the core payment domain.

Provider failures, retries, timeouts, and response mapping are handled at the integration boundary.

10. Reliable Event Processing

As the system evolves toward an event-driven architecture, payment state changes need reliable event publication.

The target flow is:

Payment Transaction
        |
        +------------------+
        |                  |
        v                  v
     Payment          Outbox Event
                           |
                           v
                       Publisher
                           |
                           v
                         Kafka
                           |
             +-------------+-------------+
             |             |             |
             v             v             v
       Notification     Audit        Other Services

The Transactional Outbox pattern prevents the classic failure scenario:

Database transaction succeeds
            +
Message publish fails
            =
State changed but event missing

Instead, the payment change and outbox record are committed together.

11. API Documentation

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

12. Persistence

The application uses MySQL with Spring Data JPA and Hibernate.

Core persistence concepts include:

Entity relationships

Enum persistence

Transaction boundaries

Unique constraints

Repository abstraction

Database-generated identifiers

Consistent state transitions

Sensitive database credentials are supplied through environment variables.

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/payment_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

Credentials are not committed to source control.

13. Testing Strategy

The project uses multiple levels of testing.

                 Test Pyramid

                    /\
                   /  \
                  / E2E\
                 /------\
                /  INT   \
               /----------\
              /   UNIT     \
             /--------------\

Unit Tests

Focus on business behavior such as:

Idempotency decisions

Order-state validation

Payment state transitions

Exception scenarios

Request-hash behavior

Integration Tests

Verify interactions between:

Spring context

JPA

MySQL

Repositories

Transactions

Testcontainers

Integration tests use containerized infrastructure where appropriate so tests execute against realistic dependencies instead of relying entirely on mocked databases.

14. Error Handling

Domain-specific exceptions are used for business failures.

Examples:

OrderNotFoundException
PaymentNotFoundException
InvalidOrderStateException
IdempotencyKeyReuseException
PaymentProcessingException

The objective is to keep business errors explicit and provide consistent HTTP responses through centralized exception handling.

15. Observability

Production operation requires more than successful requests.

The service is designed to expose:

Structured application logs

Correlation IDs

Request tracing

Application metrics

Health checks

Database health

External provider health

Payment-processing metrics

A typical request should be traceable across the system:

Request
  |
  | correlation-id
  v
API
  |
  v
Payment Service
  |
  +---- Database
  |
  +---- Payment Provider
  |
  +---- Kafka

16. Security

Security considerations include:

Environment-based secrets

No credentials committed to Git

IAM least privilege

GitHub OIDC instead of long-lived AWS access keys

Restricted IAM trust policies

ECR permissions scoped to the repository

Input validation

Idempotency-key validation

Secure external-provider communication

Authentication and authorization at the API boundary

17. Docker

The application is packaged as a Docker image.

Source
  |
  v
Maven Build
  |
  v
Spring Boot JAR
  |
  v
Docker Image
  |
  v
Amazon ECR

Build locally:

docker build -t payment-gateway .

Run locally:

docker run -p 8080:8080 \
  -e DB_USERNAME=your_username \
  -e DB_PASSWORD=your_password \
  payment-gateway

18. CI/CD

GitHub Actions automates the build and container publishing process.

Git Push
   |
   v
Checkout
   |
   v
Java 17
   |
   v
Maven Tests
   |
   v
Maven Package
   |
   v
Docker Build
   |
   v
GitHub OIDC
   |
   v
AWS STS
   |
   v
IAM Role
   |
   v
Amazon ECR
   |
   v
Docker Image

The pipeline:

Checks out the repository.

Configures Java 17.

Runs tests.

Builds the application.

Authenticates with AWS through GitHub OIDC.

Logs into Amazon ECR.

Builds the Docker image.

Pushes the image to ECR.

Images are tagged using the Git commit SHA:

payment-gateway:<commit-sha>

This provides immutable and traceable image versions.

19. AWS Authentication

The CI/CD pipeline uses GitHub OIDC rather than storing long-lived AWS access keys.

GitHub Actions
      |
      | OIDC Token
      v
AWS STS
      |
      | AssumeRoleWithWebIdentity
      v
IAM Role
      |
      v
Amazon ECR

The IAM trust relationship is restricted to the intended GitHub repository and branch.

The IAM role contains only the permissions required for the CI/CD workflow.

20. AWS Deployment

The deployment architecture is designed to evolve toward:

                       GitHub
                          |
                          v
                   GitHub Actions
                          |
                          v
                       Amazon ECR
                          |
                          v
                     ECS / Fargate
                          |
                 +--------+--------+
                 |                 |
                 v                 v
               MySQL            Redis
                 |
                 |
                 +------ Kafka / Events

Infrastructure is intended to be managed through Terraform so environments can be reproduced consistently.

21. Infrastructure as Code

The target AWS infrastructure includes:

Terraform
   |
   +-- IAM
   |
   +-- ECR
   |
   +-- VPC
   |
   +-- Security Groups
   |
   +-- Database
   |
   +-- ECS / Fargate
   |
   +-- Load Balancer
   |
   +-- Monitoring

Infrastructure configuration is kept separate from application code while remaining version controlled.

22. Redis

Redis can be introduced for workloads where low-latency in-memory access provides a clear benefit.

Potential use cases include:

Short-lived idempotency lookups

Rate limiting

Caching

Distributed coordination where required

The relational database remains the authoritative source for payment state.

23. Kafka and Event-Driven Architecture

Payment state changes can produce domain events such as:

PaymentInitiated
PaymentProcessing
PaymentSucceeded
PaymentFailed

Target architecture:

Payment Service
       |
       v
Transactional Outbox
       |
       v
     Kafka
       |
       +------------------+
       |                  |
       v                  v
Notification         Audit / Analytics
Service                 Service

Consumers should be designed to tolerate retries and duplicate delivery.

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

Database

CREATE DATABASE payment_db;

Environment

Windows PowerShell:

$env:DB_USERNAME="your_username"
$env:DB_PASSWORD="your_password"

Linux/macOS:

export DB_USERNAME="your_username"
export DB_PASSWORD="your_password"

Run

Linux/macOS:

./mvnw spring-boot:run

Windows:

mvnw.cmd spring-boot:run

Application:

http://localhost:8080

26. Engineering Decisions

Idempotency is implemented at the application and database layers

The application validates the idempotency key and request hash, while the database provides the final uniqueness guarantee.

Transactions are placed at the service layer

The service layer represents the business operation and therefore owns the transaction boundary.

Database constraints are part of correctness

Constraints are not treated only as database implementation details. They protect business invariants under concurrency.

Locking is not added without a requirement

Optimistic or pessimistic locking should be introduced when a real concurrent-update problem requires it.

External payment providers are isolated

Provider-specific code should remain behind an abstraction so that the core payment domain is not coupled to one vendor.

Events are eventually consistent

Non-critical downstream operations should be moved to asynchronous event processing where appropriate, while the payment transaction itself remains strongly consistent.

27. Production Readiness Checklist

The final project is evaluated against the following areas:

API validation

Global exception handling

Idempotency

Transaction management

Concurrency handling

Payment state machine

Payment provider abstraction

Retry strategy

Timeout handling

Transactional Outbox

Kafka event processing

Consumer idempotency

Redis where justified

Unit tests

Integration tests

Testcontainers

Structured logging

Correlation IDs

Metrics

Health checks

API security

Secret management

Docker

GitHub Actions

GitHub OIDC

Amazon ECR

Terraform

AWS deployment

Production configuration

Documentation

28. Development Philosophy

The project follows an incremental engineering approach:

Requirement
    |
    v
Design
    |
    v
Implementation
    |
    v
Test
    |
    v
Failure Analysis
    |
    v
Production Hardening

Technologies are introduced based on the problem they solve.

For example:

Duplicate requests
        |
        v
Idempotency

Concurrent inserts
        |
        v
Database uniqueness

Multiple state changes
        |
        v
Transaction

Reliable event delivery
        |
        v
Transactional Outbox

Asynchronous processing
        |
        v
Kafka

Low-latency shared state
        |
        v
Redis

Repeatable infrastructure
        |
        v
Terraform

Container deployment
        |
        v
ECR + ECS/Fargate

This keeps the architecture understandable while progressively introducing production engineering concerns.

29. Repository

GitHub:

https://github.com/mathalapavankalyan/payment-gateway

30. Author

Pavan Kalyan Mathala

Backend / Software Engineer

Core focus

Java
Spring Boot
Microservices
REST APIs
AWS
Docker
Distributed Systems
Generative AI

Project Status

This repository is being developed as a complete production-oriented payment gateway implementation.

The README documents the target final architecture. Individual capabilities are implemented, tested, and hardened incrementally as the project evolves.
