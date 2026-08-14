Payment Gateway

A backend payment-processing service built with Java 17 and Spring Boot, designed to demonstrate production-oriented backend engineering practices around idempotency, transaction management, concurrency, REST APIs, Docker, AWS, and CI/CD.

The project is intentionally being evolved from a working payment service toward a production-style distributed system.

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/MySQL-8-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Docker-Containerized-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker"/>
  <img src="https://img.shields.io/badge/AWS-ECR%20%7C%20IAM%20%7C%20STS-232F3E?style=flat-square&logo=amazonaws&logoColor=white" alt="AWS"/>
  <img src="https://img.shields.io/badge/GitHub%20Actions-CI%2FCD-2088FF?style=flat-square&logo=githubactions&logoColor=white" alt="GitHub Actions"/>
</p>

What this project demonstrates

This is not just a CRUD application. The implementation focuses on problems that matter in payment systems:

Idempotent payment creation

Duplicate-request protection

Transactional state changes

Database-level uniqueness guarantees

Order-state validation

Domain-specific exception handling

SHA-256 request hashing

Dockerized application delivery

GitHub Actions CI/CD

GitHub OIDC authentication with AWS

Least-privilege ECR access

Immutable Docker image tagging using Git commit SHA

Architecture

                         Client
                           |
                           | HTTP
                           v
                +----------------------+
                |  Payment Controller  |
                +----------+-----------+
                           |
                           v
                +----------------------+
                |   Payment Service    |
                +----------+-----------+
                           |
             +-------------+-------------+
             |             |             |
             v             v             v
        Order Repo    Payment Repo   Idempotency
             |             |          Repository
             +-------------+-------------+
                           |
                           v
                        MySQL

Payment creation flow

POST /api/v1/payments
        |
        v
Validate Idempotency-Key
        |
        v
Generate request hash
        |
        v
Check idempotency record
        |
        +-----------------------------+
        |                             |
        | Existing key                | New key
        v                             v
 Validate request hash       Create PROCESSING record
        |                             |
        |                             v
        |                      Validate order
        |                             |
        |                             v
        |                       Create payment
        |                             |
        |                             v
        |                     Update order state
        |                             |
        |                             v
        |                       Save payment
        |                             |
        |                             v
        |                    Mark COMPLETED
        |                             |
        +-------------+---------------+
                      |
                      v
                Payment response

Core domain

Order

Order
├── orderId
├── quantity
├── amount
├── currency
├── orderStatus
└── payments

Order lifecycle:

CREATED
   |
   v
PAYMENT_PENDING
   |
   v
PAID

or

CREATED / PAYMENT_PENDING
   |
   v
CANCELLED

Payment processing is allowed only when the order is in an acceptable state.

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

Idempotency record

IdempotencyRecord
├── id
├── idempotencyKey      UNIQUE
├── requestHash
├── paymentId
├── status
└── createdAt

Idempotency

Payment APIs cannot assume that one HTTP request equals one payment attempt.

A client may retry because of:

Network failures

Request timeouts

Client retries

Load balancer retries

Temporary service failures

The API therefore accepts an idempotency key:

POST /api/v1/payments
Idempotency-Key: payment-request-001

For the same key and same request:

First request
    |
    v
Create payment
    |
    v
Store result

Retry
    |
    v
Find existing key
    |
    v
Return existing payment

This prevents accidental duplicate payment creation.

Request reuse protection

The request is hashed using SHA-256.

Idempotency-Key
       +
Request data
       |
       v
Request Hash

If the same idempotency key is later used with different request data, the request is rejected instead of silently being treated as the original operation.

Transaction management

Payment creation changes multiple pieces of state:

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

These changes are performed inside a Spring transaction so a failure does not leave the database in a partially updated state.

The current implementation uses @Transactional at the service boundary.

Concurrency protection

Application-level checks alone are not sufficient for concurrent requests.

For example:

Request A                  Request B
    |                          |
    | Check key                | Check key
    | -> Not found             | -> Not found
    |                          |
    +----------+---------------+
               |
          Both attempt insert

The database provides the final uniqueness guarantee:

idempotency_key UNIQUE

If another request has already claimed the key, the insert fails with a database constraint violation and the application handles the resulting DataIntegrityViolationException.

This is intentionally implemented using a database constraint rather than adding locking mechanisms without a demonstrated business requirement.

REST API

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

Technology

Area

Technology

Language

Java 17

Framework

Spring Boot

Web

Spring Web / REST

Persistence

Spring Data JPA

ORM

Hibernate

Database

MySQL 8

Build

Maven

Containerization

Docker

CI/CD

GitHub Actions

Cloud

AWS

Container Registry

Amazon ECR

Authentication

GitHub OIDC + AWS STS

Project structure

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
└── README.md

Local development

Prerequisites

Java 17

MySQL 8

Maven Wrapper

Docker (optional)

Create the database

CREATE DATABASE payment_db;

Configure the application using environment variables:

DB_USERNAME=<database-username>
DB_PASSWORD=<database-password>

Windows PowerShell

$env:DB_USERNAME="your_username"
$env:DB_PASSWORD="your_password"

Run

Linux/macOS:

./mvnw spring-boot:run

Windows:

mvnw.cmd spring-boot:run

Application:

http://localhost:8080

Docker

Build:

docker build -t payment-gateway .

Run:

docker run -p 8080:8080 \
  -e DB_USERNAME=your_username \
  -e DB_PASSWORD=your_password \
  payment-gateway

The container must be able to reach the MySQL instance configured by the application.

CI/CD

The project uses GitHub Actions to validate and package every push to main.

                 Git Push
                    |
                    v
             GitHub Actions
                    |
          +---------+---------+
          |                   |
          v                   v
      Maven Tests        Maven Package
          |                   |
          +---------+---------+
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

The pipeline performs:

Checkout source code

Configure Java 17

Run Maven tests

Package the Spring Boot application

Authenticate to AWS using GitHub OIDC

Authenticate Docker with Amazon ECR

Build the Docker image

Push the image to ECR

Images are tagged using the Git commit SHA:

payment-gateway:<git-commit-sha>

This makes every image traceable to the source revision that produced it.

AWS authentication

The CI pipeline does not use long-lived AWS access keys.

Instead:

GitHub Actions
      |
      | OIDC token
      v
AWS STS
      |
      | AssumeRoleWithWebIdentity
      v
IAM Role
      |
      v
Amazon ECR

The IAM trust policy restricts which GitHub repository and branch can assume the role.

The role is granted only the ECR permissions required by the pipeline.

This keeps AWS credentials out of GitHub repository secrets and uses short-lived credentials for the workflow.

Security considerations

Current implementation includes:

Environment-based database credentials

No database passwords committed to source control

GitHub OIDC authentication

Restricted IAM trust policy

ECR permissions scoped to the payment-gateway repository

Idempotency-key uniqueness at the database level

Request-hash validation

Transactional payment processing

Domain-specific exceptions

Error handling

The service uses domain-specific exceptions including:

OrderNotFoundException
PaymentNotFoundException
InvalidOrderStateException
IdempotencyKeyReuseException
PaymentProcessingException

This keeps business failures explicit and separates domain behavior from infrastructure exceptions.

Engineering decisions

Why idempotency?

A payment endpoint must tolerate retries without creating duplicate payments.

Why SHA-256?

The request hash allows the service to detect an idempotency key being reused for a different request.

Why a database unique constraint?

Concurrent requests can pass an application-level existence check at the same time. The database constraint provides the final uniqueness guarantee.

Why transactions?

Payment creation modifies multiple related records. Transaction boundaries prevent partial state updates.

Why not add locking immediately?

Locking should solve a demonstrated concurrency problem. The current idempotency design already uses a database uniqueness constraint for duplicate-key protection, so additional locking is not introduced without a requirement.

Roadmap

The current implementation provides the foundation for the following production-oriented extensions.

Current
  |
  +--> Payment Provider abstraction
  |
  +--> Payment state machine
  |
  +--> Transactional Outbox
  |
  +--> Kafka event publishing
  |
  +--> Redis
  |
  +--> Observability
  |
  +--> Testcontainers
  |
  +--> Terraform
  |
  +--> AWS ECS / Fargate

Planned improvements

Payment provider abstraction

PaymentService
      |
      v
PaymentProvider
   +-- UPI
   +-- Card
   +-- Net Banking

Stronger payment state machine

INITIATED
    |
    v
PROCESSING
   / \
  v   v
SUCCESS  FAILED

Transactional Outbox

Payment Transaction
        |
        +--> Payment
        |
        +--> Outbox Event
                    |
                    v
                  Kafka

Observability

Structured logging

Correlation IDs

Metrics

Distributed tracing

Health checks

Testing

Introduce Testcontainers for integration tests against a real MySQL container.

Infrastructure as Code

Move manually created AWS infrastructure to Terraform:

Terraform
   |
   +--> IAM
   +--> ECR
   +--> VPC
   +--> RDS
   +--> ECS / Fargate

Deployment

Extend the pipeline from:

GitHub
  |
  v
GitHub Actions
  |
  v
Amazon ECR

to:

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
  v
Payment Gateway

What I am building with this project

The goal is to evolve the service incrementally from a clean Spring Boot backend into a production-style payment system.

REST APIs
    ↓
Domain Modeling
    ↓
Transactions
    ↓
Idempotency
    ↓
Concurrency
    ↓
Docker
    ↓
CI/CD
    ↓
AWS
    ↓
Event-Driven Architecture
    ↓
Production Deployment

The focus is on understanding why each engineering decision is required, rather than adding technologies only for the sake of the stack.

Repository

GitHub Repository

Author

Pavan Kalyan Mathala

Backend / Software Engineer

Focus: Java | Spring Boot | Microservices | REST APIs | AWS | Docker | Distributed Systems | Generative AI
