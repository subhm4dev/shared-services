# 🔧 Shared User Profile Service

Shared User Profile Management Service - Used across all domains (ecommerce, hospital, food delivery, etc.).

## Overview

This service manages user profile information including name, phone number, and avatar URL.

## Port

**8082**

## Features

- Create/Update user profile
- Get user profile by userId
- Publish ProfileUpdated events to Kafka

## Running Locally

```bash
mvn spring-boot:run
```

## Endpoints

- `POST /api/v1/profile` - Create or update profile
- `GET /api/v1/profile/{userId}` - Get profile by userId

## Dependencies

- PostgreSQL (via Docker Compose)
- Kafka (for event publishing)
- Spring Cloud Config Server (optional)

