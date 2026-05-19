# User Profile Service

REST microservice for managing user profiles and payment cards.  
Authenticates requests via a gRPC call to a separate **auth-service** that validates JWT tokens.

## Tech stack

| Layer | Choice |
|---|---|
| Runtime | Java 17, Spring Boot 3.4.5 |
| Persistence | PostgreSQL 15, Spring Data JPA, Flyway |
| Caching | Redis 7, Spring Cache (`@Cacheable` / programmatic eviction) |
| Auth | Spring Security + JWT validated via gRPC (auth-service) |
| Mapping | MapStruct 1.6.3 |
| Containerisation | Docker, Docker Compose |
| CI | GitHub Actions |

## API

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/users` | Any authenticated | Create user |
| GET | `/api/v1/users` | ADMIN | List users (paginated, filter by name/surname) |
| GET | `/api/v1/users/{id}` | ADMIN or own user | Get user by ID |
| GET | `/api/v1/users/search?email=` | ADMIN or own user | Get user by email |
| PUT | `/api/v1/users/{id}` | ADMIN or own user | Update user |
| PATCH | `/api/v1/users/{id}/deactivate` | ADMIN or own user | Deactivate user |
| PATCH | `/api/v1/users/{id}/activate` | ADMIN | Activate user |
| POST | `/api/v1/users/{id}/cards` | ADMIN or own user | Add payment card (max 5 active) |
| GET | `/api/v1/users/{id}/cards` | ADMIN or own user | List user's cards |
| PATCH | `/api/v1/users/cards/{cardId}/deactivate` | ADMIN or card owner | Deactivate card |
| PATCH | `/api/v1/users/cards/{cardId}/activate` | ADMIN or card owner | Activate card |

## Running locally

### Prerequisites
- Docker & Docker Compose

### 1. Copy env template
```bash
cp .env.example .env
# edit .env if needed — defaults work out of the box
```

### 2. Start everything
```bash
docker compose up --build
```

Service starts on **http://localhost:8082**.  
Every request needs an `Authorization: Bearer <token>` header issued by the auth-service.

### 3. Local development (no Docker for the app)
```bash
# Start only infrastructure
docker compose up postgres redis -d

# Run the app with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Running tests

```bash
# Unit tests only (no Docker needed)
./mvnw test -pl . -Dtest="UserServiceImplTest"

# All tests including integration (requires Docker for Testcontainers)
./mvnw verify
```

## Database migrations

Flyway runs automatically on startup.

| Version | Description |
|---|---|
| V1 | `users` table |
| V2 | `payment_cards` table |

## Project structure

```
src/main/java/.../
├── config/          # JPA auditing, Redis cache, Security
├── controller/      # UserController — all REST endpoints
├── dto/             # Request / response DTOs
├── entity/          # User, PaymentCard, Role
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── filter/          # JwtAuthFilter (gRPC token validation)
├── mapper/          # MapStruct interfaces (UserMapper, PaymentCardMapper)
├── repository/      # Spring Data repositories + UserSpecifications
└── service/         # UserService interface + UserServiceImpl
```
