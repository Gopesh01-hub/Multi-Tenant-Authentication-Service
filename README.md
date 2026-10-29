# Identity Platform OSS

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DD0031?logo=redis)](https://redis.io/)

A production-grade, **multi-tenant identity platform** built with Spring Boot 3.3 and Java 21. Features JWT-based authentication with refresh token rotation, configuration-driven tenant onboarding, two-tier caching (Redis + Caffeine), account lockout protection, and a partitioned audit trail for compliance.

---

## Key Features

| Feature | Description |
|---|---|
| **Multi-Tenancy** | Thread-local tenant isolation via `X-Tenant-ID` header. Per-tenant password policies, rate limits, session caps, and allowed origins — all config-driven. |
| **JWT Authentication** | Access tokens (15 min) + refresh tokens (7 days) with automatic rotation. Tenant claim cross-validation prevents cross-tenant token replay. |
| **Account Security** | BCrypt password hashing (cost 12), account lockout after 5 failed attempts (30 min), automatic unlock. |
| **Two-Tier Caching** | Caffeine L1 (in-process, sub-ms) for hot tenant configs + Redis L2 (shared) for sessions and revoked tokens. |
| **Audit Trail** | Async audit logging to partitioned PostgreSQL tables (yearly). Captures IP, user-agent, action, resource, and status for compliance. |
| **Zero-Deploy Onboarding** | New tenants onboarded via YAML config changes — no code changes or redeployment needed. |
| **RBAC** | Role-based access control (USER/ADMIN) enforced via Spring Security method-level annotations. |
| **API Documentation** | Auto-generated OpenAPI 3.0 / Swagger UI at `/api/swagger-ui.html`. |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client                                   │
│                    (X-Tenant-ID: sky-uk)                         │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│  Tenant Interceptor → Resolve Config → Set ThreadLocal + MDC    │
├─────────────────────────────────────────────────────────────────┤
│  JWT Auth Filter → Validate Token → Cross-check Tenant Claim    │
├─────────────┬───────────────┬───────────────┬───────────────────┤
│ Auth        │ User          │ Tenant        │ Audit             │
│ Controller  │ Controller    │ Controller    │ Controller        │
├─────────────┼───────────────┼───────────────┼───────────────────┤
│ Auth        │ User          │ Tenant Config │ Audit             │
│ Service     │ Service       │ Registry      │ Service (async)   │
├─────────────┴───────────────┴───────────────┴───────────────────┤
│                   JPA Repositories                               │
├──────────────┬──────────────────────────────┬───────────────────┤
│  PostgreSQL  │   Caffeine (L1 Cache)        │  Redis (L2 Cache) │
│  (Partitioned│   - Tenant configs (5min)    │  - Sessions (1hr) │
│   Audit Logs)│   - Token blacklist (5min)   │  - Revoked (24hr) │
└──────────────┴──────────────────────────────┴───────────────────┘
```

For detailed sequence diagrams, ER diagrams, and caching strategy, see [docs/architecture.md](docs/architecture.md).

---

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 21 (for local development)

### Run with Docker (3 commands)

```bash
git clone https://github.com/Gopesh01-hub/Multi-Tenant-Authentication-Service
cd identity-platform-oss
docker compose up -d
```

The API is available at `http://localhost:8080/api`.  
Swagger UI: `http://localhost:8080/api/swagger-ui.html`.

### Run Locally

```bash
# Start dependencies
docker compose up -d postgres redis

# Run the application
./mvnw spring-boot:run
```

---

## API Reference

All endpoints require the `X-Tenant-ID` header. Protected endpoints require `Authorization: Bearer <token>`.

### Authentication

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/v1/auth/login` | Public | Login and receive JWT tokens |
| POST | `/v1/auth/refresh` | Public | Refresh expired access token |
| POST | `/v1/auth/logout` | Public | Revoke refresh token |

### Users

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/v1/users` | Public | Register a new user |
| GET | `/v1/users/{id}` | Bearer | Get user by ID |
| GET | `/v1/users` | Admin | List all users (paginated) |

### Tenants

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/v1/tenants` | Public | List all registered tenant IDs |
| GET | `/v1/tenants/{tenantId}` | Public | Get tenant public config |

### Admin

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/v1/admin/audit-logs` | Admin | Paginated audit trail |

### Example: Full Auth Flow

```bash
# 1. Register
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: sky-uk" \
  -d '{"email":"user@sky.com","password":"SecurePass123!","firstName":"John","lastName":"Doe"}'

# 2. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: sky-uk" \
  -d '{"email":"user@sky.com","password":"SecurePass123!"}'

# 3. Access protected resource (use accessToken from login response)
curl http://localhost:8080/api/v1/users/1 \
  -H "X-Tenant-ID: sky-uk" \
  -H "Authorization: Bearer <accessToken>"

# 4. Refresh token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: sky-uk" \
  -d '{"refreshToken":"<refreshToken>"}'
```

A [Postman collection](postman/identity-platform.postman_collection.json) is included for interactive testing.

---

## Tenant Configuration

New tenants are onboarded through `application.yml` — zero code changes:

```yaml
identity:
  tenant:
    configs:
      - tenant-id: sky-uk
        display-name: Sky UK
        enabled: true
        max-sessions: 5
        password-policy:
          min-length: 10
          require-uppercase: true
          require-special-char: true
          max-age-days: 90
        rate-limit:
          requests-per-minute: 1000
          burst-size: 100
        allowed-origins:
          - https://sky.com
```

Each tenant gets independent:
- Password complexity rules
- Maximum concurrent sessions
- Rate limiting thresholds
- CORS allowed origins

---

## Security Design

| Mechanism | Implementation |
|---|---|
| Password hashing | BCrypt with cost factor 12 |
| Token signing | HMAC-SHA256 via JJWT |
| Refresh rotation | Old token revoked on each refresh |
| Cross-tenant guard | JWT tenant claim validated against request header |
| Account lockout | 5 failed attempts → 30-minute lock |
| Audit trail | Async writes to partitioned tables (yearly) |
| IP tracking | `X-Forwarded-For` aware client IP extraction |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3.3 |
| Security | Spring Security 6, JJWT 0.12 |
| Database | PostgreSQL 16 (partitioned tables) |
| Cache | Redis 7 (L2) + Caffeine (L1) |
| ORM | Spring Data JPA / Hibernate 6 |
| Migration | Flyway |
| API Docs | SpringDoc OpenAPI 2.6 |
| Containerization | Docker multi-stage build |
| CI/CD | GitHub Actions |
| Testing | JUnit 5, Mockito, MockMvc, Testcontainers |

---

## Project Structure

```
src/main/java/com/gopesh/identity/
├── config/           # Security, cache, web MVC, OpenAPI, UserDetailsService
├── tenant/           # Multi-tenancy: context, interceptor, config registry, controller
├── security/         # JWT provider, authentication filter, UserPrincipal
├── auth/             # Login/refresh/logout: controller, service, DTOs
├── user/             # User CRUD: entity, repository, service, controller, DTOs
├── token/            # Refresh token: entity, repository, rotation service
├── audit/            # Audit logging: entity, repository, async service, controller
└── exception/        # Global exception handler, custom exceptions
```

---

## Testing

```bash
# Unit tests
./mvnw test

# Integration tests (requires Docker for Testcontainers)
./mvnw verify

# Test with coverage report
./mvnw test jacoco:report
```

---

## Roadmap

- [ ] OAuth 2.0 Authorization Server (PKCE flow)
- [ ] SAML 2.0 SP integration
- [ ] Rate limiting middleware (per-tenant, using Redis sliding window)
- [ ] Kafka event streaming for cross-service audit propagation
- [ ] Kubernetes Helm chart with HPA
- [ ] Admin dashboard (React + TypeScript)
- [ ] Multi-factor authentication (TOTP)

---

## Contributing

Contributions are welcome! Please open an issue first to discuss proposed changes.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## Author

**Gopesh Gupta** — Backend Engineering  
[GitHub](https://github.com/Gopesh01-hub) · [LinkedIn](https://www.linkedin.com/in/gupta-gopesh/)