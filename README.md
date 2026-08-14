Payment Gateway

A production-oriented payment gateway backend built with Java and Spring Boot, focusing on reliable payment processing, transactional consistency, idempotency, REST API design, containerization, and CI/CD automation.

The project is designed to demonstrate practical backend engineering concepts used in payment and financial systems.

Technology Stack

<p align="left">
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/java/java-original.svg" width="42" height="42" alt="Java" title="Java"/>
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/spring/spring-original.svg" width="42" height="42" alt="Spring Boot" title="Spring Boot"/>
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mysql/mysql-original.svg" width="42" height="42" alt="MySQL" title="MySQL"/>
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/docker/docker-original.svg" width="42" height="42" alt="Docker" title="Docker"/>
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/maven/maven-original.svg" width="42" height="42" alt="Maven" title="Maven"/>
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/githubactions/githubactions-original.svg" width="42" height="42" alt="GitHub Actions" title="GitHub Actions"/>
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/amazonwebservices/amazonwebservices-original-wordmark.svg" width="55" height="42" alt="AWS" title="AWS"/>
</p>

Backend: Java 17, Spring Boot, Spring Web, Spring Data JPA, Hibernate, Jakarta Persistence

Database: MySQL 8

Build: Maven

Containerization: Docker

Cloud: AWS, Amazon ECR, AWS IAM, AWS STS

CI/CD: GitHub Actions, GitHub OIDC

Overview

The Payment Gateway provides APIs for creating orders, initiating payments, and retrieving payment information.

The system is designed around the principle that payment operations must be:

Reliable

Idempotent

Transactionally consistent

Safe against duplicate requests

Maintainable

Deployable through an automated CI/CD pipeline

The current implementation focuses on the core payment-processing domain while establishing a foundation that can be extended toward a distributed production architecture.

Architecture

                    Client
                      |
                      v
              REST API / Controller
                      |
                      v
                Service Layer
                      |
          +-----------+-----------+
          |                       |
          v                       v
    Order Repository       Payment Repository
          |                       |
          +-----------+-----------+
                      |
                      v
                   MySQL
                      |
                      |
              Idempotency Records

The application is packaged as a Docker container and the CI/CD pipeline builds and publishes the container image to Amazon ECR.

Developer
   |
   | git push
   v
GitHub Repository
   |
   v
GitHub Actions
   |
   +---- Run Tests
   |
   +---- Build Application
   |
   +---- Build Docker Image
   |
   +---- Authenticate using AWS OIDC
   |
   +---- Push Image
   |
   v
Amazon ECR

Core Features

Order Management

The system supports creation and management of orders.

An order contains:

Order ID

Quantity

Amount

Currency

Order status

Supported order states include:

CREATED
   |
   v
PAYMENT_PENDING
   |
   v
PAID

An order can also transition to:

CANCELLED

The domain model prevents payment processing when an order is not in a valid state.

Payment Processing

The payment API accepts an order ID and payment type.

Supported payment types currently include:

UPI
CARD
NET_BANKING

A payment is initially created with:

INITIATED

The payment is associated with the corresponding order.

Idempotency

Payment APIs must handle duplicate requests safely.

For example:

POST /api/v1/payments
Idempotency-Key: payment-request-123

If a client retries the same request with the same idempotency key, the system returns the previously created payment instead of creating a duplicate payment.

Idempotency Flow

Request
   |
   v
Extract Idempotency-Key
   |
   v
Generate Request Hash
   |
   v
Check Idempotency Record
   |
   +-----------------------------+
   |                             |
Existing                       New
   |                             |
   v                             v
Validate Hash             Create PROCESSING record
   |                             |
   +-------------+---------------+
                 |
                 v
          Process Payment
                 |
                 v
          Save Payment
                 |
                 v
       Mark operation COMPLETED
                 |
                 v
          Return Payment

Key Reuse Protection

An idempotency key cannot safely be reused for a different request.

The implementation generates a SHA-256 hash from relevant request attributes.

If an existing idempotency key is used with different request data, the request is rejected.

Transaction Management

Payment creation is executed within a database transaction.

The operation involves multiple state changes:

1. Create idempotency record
2. Retrieve order
3. Validate order state
4. Create payment
5. Update order state
6. Persist payment
7. Mark idempotency operation as completed

These operations are handled as a transactional unit using Spring's transaction management.

Concurrency Handling

The idempotency_key column has a database-level unique constraint.

idempotency_records
-------------------------
id
idempotency_key UNIQUE
request_hash
payment_id
status
created_at

If concurrent requests attempt to claim the same idempotency key, the database constraint prevents both requests from successfully creating the record.

The application handles the resulting DataIntegrityViolationException.

REST API

Create Order

POST /api/v1/orders
Content-Type: application/json

Example request:

{
  "quantity": 2,
  "amount": 1000.00,
  "currency": "INR"
}

Example response:

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

Example request:

{
  "orderId": 1,
  "paymentType": "UPI"
}

Example response:

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

Domain Model

Order

Order
--------------------------------
orderId
quantity
amount
currency
orderStatus
payments

Payment

Payment
--------------------------------
paymentId
order
paymentType
paymentStatus
paymentAmount
currency

Idempotency Record

IdempotencyRecord
--------------------------------
id
idempotencyKey
requestHash
paymentId
status
createdAt

Project Structure

src
├── main
│   ├── java
│   │   └── com.pk.payment
│   │       ├── controller
│   │       ├── dto
│   │       ├── entity
│   │       ├── enums
│   │       ├── exception
│   │       ├── mapper
│   │       ├── repository
│   │       └── service
│   │           └── impl
│   │
│   └── resources
│       └── application.yml
│
├── test
│   └── java
│       └── com.pk.payment
│
├── Dockerfile
├── pom.xml
└── .github
    └── workflows
        └── payment-gateway-ci.yml

Configuration

Database credentials are supplied through environment variables rather than being committed to source control.

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/payment_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

Example environment variables:

DB_USERNAME=<database-username>
DB_PASSWORD=<database-password>

Sensitive credentials should never be committed to GitHub.

Running Locally

Prerequisites

Java 17

MySQL 8

Docker (optional)

Create Database

CREATE DATABASE payment_db;

Configure the required environment variables.

Windows PowerShell:

$env:DB_USERNAME="your_username"
$env:DB_PASSWORD="your_password"

Run Application

Linux/macOS:

./mvnw spring-boot:run

Windows:

mvnw.cmd spring-boot:run

The application runs on:

http://localhost:8080

Docker

Build the image:

docker build -t payment-gateway .

Run the container:

docker run -p 8080:8080 \
  -e DB_USERNAME=your_username \
  -e DB_PASSWORD=your_password \
  payment-gateway

The MySQL instance must be accessible from the container.

CI/CD Pipeline

The project uses GitHub Actions to automate the build and container publishing workflow.

Git Push
   |
   v
Checkout Repository
   |
   v
Setup Java 17
   |
   v
Run Maven Tests
   |
   v
Build Application
   |
   v
Authenticate with AWS
   |
   | GitHub OIDC
   v
AWS IAM Role
   |
   v
Amazon ECR Authentication
   |
   v
Build Docker Image
   |
   v
Push Docker Image to ECR

The workflow:

Checks out the source code.

Configures Java 17.

Runs the test suite.

Builds the Spring Boot application.

Authenticates with AWS using GitHub OIDC.

Logs into Amazon ECR.

Builds the Docker image.

Pushes the image to ECR.

Docker images are tagged using the Git commit SHA:

payment-gateway:<commit-sha>

This provides immutable image identification and allows an image to be traced back to the exact source revision that produced it.

AWS Integration

The project currently uses Amazon ECR as the container image registry.

<aws-account-id>.dkr.ecr.<region>.amazonaws.com/payment-gateway

GitHub OIDC Authentication

The CI/CD workflow uses short-lived AWS credentials through GitHub's OpenID Connect integration.

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

The IAM trust policy restricts the role to the project's GitHub repository and main branch.

The IAM role is granted only the ECR permissions required by the pipeline.

No long-lived AWS access keys are stored in GitHub.

Security and Reliability

The project currently implements:

Environment-based database credentials

GitHub OIDC authentication

Restricted AWS IAM permissions

Idempotent payment creation

SHA-256 request hashing

Database-level unique constraints

Transactional payment processing

Domain-specific exception handling

Dockerized application delivery

Error Handling

The application uses domain-specific exceptions for common failure scenarios:

OrderNotFoundException
PaymentNotFoundException
InvalidOrderStateException
IdempotencyKeyReuseException
PaymentProcessingException

This separates business failures from infrastructure-level exceptions and allows the REST layer to provide appropriate HTTP responses.

Design Decisions

Why Idempotency?

Payment requests can be retried because of:

Network failures

Client retries

Timeout responses

Load balancer retries

Mobile application retries

A payment API should not assume that every incoming request represents a new payment operation.

Why a Database Constraint?

Application-level checks alone are not sufficient under concurrency.

Two requests could both observe that an idempotency key does not exist before either request inserts it.

The unique database constraint provides the final consistency boundary:

idempotency_key UNIQUE

Why Transactions?

Creating a payment changes multiple pieces of state.

Order
   |
   +--> PAYMENT_PENDING

Payment
   |
   +--> INITIATED

Idempotency Record
   |
   +--> COMPLETED

These changes should not leave the system in an inconsistent state if an operation fails midway.

Future Engineering Roadmap

The project is intentionally structured so production concerns can be introduced incrementally.

Payment Provider Abstraction

Introduce a provider abstraction:

PaymentService
      |
      v
PaymentProvider
      |
      +---- UPI Provider
      |
      +---- Card Provider
      |
      +---- Net Banking Provider

Payment State Machine

Introduce stricter payment state transitions:

INITIATED
    |
    v
PROCESSING
   / \
  v   v
SUCCESS  FAILED

Transactional Outbox

Introduce the transactional outbox pattern for reliable event publishing.

Payment Transaction
       |
       +---- Payment
       |
       +---- Outbox Event
                    |
                    v
                  Kafka

Event-Driven Architecture

Introduce asynchronous payment events:

Payment Service
      |
      v
     Kafka
      |
      +---- Notification Service
      |
      +---- Order Service
      |
      +---- Audit Service

Redis

Potential use cases:

Short-lived idempotency lookups

Distributed caching

Rate limiting

The database remains the authoritative source for payment state.

Observability

Introduce:

Structured logging

Correlation IDs

Metrics

Distributed tracing

Health checks

Integration Testing

Introduce Testcontainers for realistic MySQL integration testing.

Infrastructure as Code

Move AWS infrastructure management to Terraform:

Terraform
   |
   +---- ECR
   |
   +---- IAM
   |
   +---- VPC
   |
   +---- RDS
   |
   +---- ECS / Fargate

Deployment

Extend the current pipeline:

GitHub
   |
   v
GitHub Actions
   |
   v
Amazon ECR
   |
   v
AWS ECS / Fargate
   |
   v
Payment Gateway

Engineering Concepts Demonstrated

Java 17

Spring Boot

REST API design

Layered architecture

Domain modeling

Spring Data JPA

Hibernate

Transaction management

Database constraints

Idempotency

Concurrent request handling

SHA-256 request hashing

Exception handling

Docker

CI/CD

GitHub Actions

AWS IAM

AWS STS

GitHub OIDC

Amazon ECR

Secure credential management

Development Approach

The project is being developed incrementally, introducing production concepts based on actual system requirements rather than adding infrastructure prematurely.

REST API
   |
   v
Domain Model
   |
   v
Transactions
   |
   v
Idempotency
   |
   v
Concurrency
   |
   v
Containerization
   |
   v
CI/CD
   |
   v
AWS
   |
   v
Event-Driven Architecture
   |
   v
Production Deployment

Repository

GitHub: https://github.com/mathalapavankalyan/payment-gateway

Author

Pavan Kalyan Mathala

Backend / Software Engineer

Primary focus:

Java
Spring Boot
Microservices
REST APIs
AWS
Docker
Distributed Systems
Generative AI
