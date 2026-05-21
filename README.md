# Polyglot Distributed SMS Service

This repository contains two microservices to handle an SMS Notification flow, demonstrating a Polyglot Distributed architecture.

## Architecture

1. **SMS Sender (Java / Spring Boot)**: The gateway service that receives requests, makes a (mocked) synchronous call to a 3rd party SMS vendor, and produces an SMS event into Kafka.
2. **SMS Store (GoLang)**: Listens for Kafka events to persist the records in MongoDB, and provides an API to fetch a user's SMS history.

### Pre-requisites
- **Java 17+** and Maven
- **Go 1.20+**
- **Docker & Docker Compose** (for Kafka, Redis, and MongoDB)

## Getting Started

### 1. Start Infrastructure Dependencies
There is a `docker-compose.yml` to spin up Kafka, Redis, and MongoDB.
```bash
docker-compose up -d
```
*Wait a minute for Kafka to initialize.*

### 2. Run the Java SMS Sender Service
Navigate to the `java-sms-sender` directory and start the app natively using Maven. The project has been configured to build and run seamlessly with Java 1.8:

```bash
cd java-sms-sender
mvn spring-boot:run
```
Service runs on port `8080`.

### 3. Run the GoLang SMS Store Service
Navigate to the `go-sms-store` directory and run:
```bash
cd go-sms-store
go run main.go
```
Service runs on port `8081`.

## Core APIs

### 1. Send SMS (Java)
**Endpoint**: `POST http://localhost:8080/v1/sms/send`

**Example (PowerShell)**:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/v1/sms/send" -Method Post -Headers @{"Content-Type"="application/json"} -Body '{"userId":"user123","phoneNumber":"+1234567890","message":"Hello from Polyglot Architecture!"}'
```

**Example (cURL)**:
```bash
curl -X POST http://localhost:8080/v1/sms/send -H "Content-Type: application/json" -d "{\"userId\":\"user123\",\"phoneNumber\":\"+1234567890\",\"message\":\"Hello from Polyglot Architecture!\"}"
```

### 2. Get SMS History (GoLang)
**Endpoint**: `GET http://localhost:8081/v1/user/{userId}/messages`

**Example (PowerShell)**:
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/v1/user/user123/messages" -Method Get
```

**Example (cURL)**:
```bash
curl http://localhost:8081/v1/user/user123/messages
```

## Demonstration Flow
1. Start infrastructure and both services.
2. Hit the `POST /v1/sms/send` API on port `8080`.
3. Check the GoLang logs. You should see `Successfully saved SMS to DB for user: user123`.
4. Hit the `GET /v1/user/user123/messages` API on port `8081` to view your persisted SMS record.