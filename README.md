# Leave Management Service

A production-ready REST API for managing employee leave requests with role-based access control, automated balance tracking, and transactional integrity. Built with Spring Boot 3, PostgreSQL, and secured with JWT authentication.

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 17 | Programming language |
| **Spring Boot** | 3.2.5 | Application framework |
| **Spring Security** | 6.2.4 | Authentication & authorization |
| **PostgreSQL** | 15 | Production database |
| **H2** | 2.2.224 | In-memory test database |
| **Flyway** | 10.11.0 | Database migrations |
| **JWT (jjwt)** | 0.11.5 | Token-based auth |
| **JUnit 5** | 5.10.2 | Unit testing |
| **Mockito** | 5.11.0 | Mocking framework |
| **JaCoCo** | 0.8.11 | Code coverage (80% minimum) |
| **Docker** | Latest | Containerization |
| **Maven** | 3.9+ | Build tool |

## Quick Start with Docker

**Prerequisites**: Docker and Docker Compose installed

```bash
# 1. Create environment file
cat > .env << EOF
DB_NAME=leavedb
DB_USER=leaveuser
DB_PASSWORD=leavepass
JWT_SECRET=your-secret-key-min-256-bits-replace-with-strong-random-key
JWT_EXPIRATION=86400000
EOF

# 2. Build and start services
docker-compose up -d

# 3. Access the application
# API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

The application will automatically:
- Create PostgreSQL database with schema migrations
- Seed initial data (leave types)
- Start the REST API on port 8080

## API Endpoints

### Authentication Endpoints (Public)

#### Register New User
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "john.doe@company.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response**: 201 Created
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "email": "john.doe@company.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "EMPLOYEE",
    "enabled": true
  }
}
```

**Note**: Registration automatically initializes leave balances:
- Annual Leave: 20 days
- Sick Leave: 10 days
- Casual Leave: 5 days

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john.doe@company.com",
  "password": "SecurePass123!"
}
```

**Response**: 200 OK (same structure as register)

---

### Leave Request Endpoints (Authenticated)

**Authentication Required**: All endpoints below require JWT token in header:
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

#### Submit Leave Request (EMPLOYEE, MANAGER)
```http
POST /api/leave/submit
Content-Type: application/json
Authorization: Bearer {token}

{
  "leaveTypeId": 1,
  "startDate": "2026-06-01",
  "endDate": "2026-06-05",
  "reason": "Family vacation"
}
```

**Response**: 201 Created
```json
{
  "id": 1,
  "user": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@company.com"
  },
  "leaveType": {
    "id": 1,
    "name": "Annual Leave",
    "defaultDays": 20
  },
  "startDate": "2026-06-01",
  "endDate": "2026-06-05",
  "numberOfDays": 5,
  "status": "PENDING",
  "reason": "Family vacation",
  "submittedAt": "2026-05-05T10:30:00"
}
```

**Validations**:
- ❌ 409 Conflict: Insufficient balance
- ❌ 409 Conflict: Overlapping dates with existing leave
- ❌ 400 Bad Request: End date before start date

#### Get My Leave Requests (EMPLOYEE, MANAGER)
```http
GET /api/leave/my-requests
Authorization: Bearer {token}
```

**Response**: 200 OK (array of leave request objects)

#### Get Pending Requests (MANAGER only)
```http
GET /api/leave/pending
Authorization: Bearer {token}
```

**Response**: 200 OK (array of PENDING requests from all employees)

#### Approve Leave Request (MANAGER only)
```http
PUT /api/leave/{id}/approve
Authorization: Bearer {token}
```

**Response**: 200 OK

**What happens (@Transactional)**:
1. Request status → APPROVED
2. Leave balance updated: `usedDays += numberOfDays`

**Both operations succeed or both fail (atomicity).**

**Example Without @Transactional (BROKEN)**:
```java
// ❌ This would be dangerous!
public void approveLeave(Long id) {
    request.setStatus(APPROVED);
    requestRepository.save(request);  // ✅ Succeeds
    
    balance.setUsedDays(balance.getUsedDays() + 5);
    balanceRepository.save(balance);  // ❌ Fails (DB connection lost)
    
    // PROBLEM: Request approved but balance not deducted!
    // User can take leave without consuming days → DATA CORRUPTION
}
```

**With @Transactional (CORRECT)**:
```java
@Transactional
public void approveLeave(Long id) {
    // Both writes wrapped in transaction
    request.setStatus(APPROVED);
    requestRepository.save(request);  // Write 1
    
    balance.setUsedDays(balance.getUsedDays() + 5);
    balanceRepository.save(balance);  // Write 2
    
    // If Write 2 fails → Write 1 automatically rolled back
    // System stays consistent: PENDING status + unchanged balance
}
```

**Access Control**:
- ❌ 403 Forbidden: If user is not MANAGER
- ❌ 409 Conflict: Insufficient balance at approval time
- ❌ 409 Conflict: Manager trying to approve own leave

#### Reject Leave Request (MANAGER only)
```http
PUT /api/leave/{id}/reject
Content-Type: application/json
Authorization: Bearer {token}

{
  "reason": "Overlaps with team sprint deadline"
}
```

**Response**: 200 OK

**What happens**:
- Request status → REJECTED
- Balance unchanged (never deducted)

#### Cancel Leave Request (Owner only)
```http
PUT /api/leave/{id}/cancel
Authorization: Bearer {token}
```

**Response**: 200 OK

**What happens (@Transactional)**:
- **If request was PENDING**: Status → CANCELLED (no balance change)
- **If request was APPROVED**: Status → CANCELLED + `usedDays -= numberOfDays` (balance restored)

**Transaction Protection**: Status update and balance restoration happen atomically.

---

### Leave Balance Endpoints (Authenticated)

#### Get My Leave Balances (EMPLOYEE, MANAGER)
```http
GET /api/leave/balance
Authorization: Bearer {token}
```

**Response**: 200 OK
```json
[
  {
    "id": 1,
    "leaveType": {
      "id": 1,
      "name": "Annual Leave",
      "defaultDays": 20
    },
    "availableDays": 20,
    "usedDays": 5,
    "remainingDays": 15,
    "year": 2026
  },
  {
    "id": 2,
    "leaveType": {
      "id": 2,
      "name": "Sick Leave",
      "defaultDays": 10
    },
    "availableDays": 10,
    "usedDays": 0,
    "remainingDays": 10,
    "year": 2026
  }
]
```

#### Get User Balance (MANAGER only)
```http
GET /api/leave/balance/{userId}
Authorization: Bearer {token}
```

**Response**: 200 OK (same structure as above)

**Access Control**: ❌ 403 Forbidden if user is not MANAGER

---

### Leave Type Endpoints (Authenticated)

#### Get All Leave Types (EMPLOYEE, MANAGER)
```http
GET /api/leave/types
Authorization: Bearer {token}
```

**Response**: 200 OK
```json
[
  {
    "id": 1,
    "name": "Annual Leave",
    "defaultDays": 20
  },
  {
    "id": 2,
    "name": "Sick Leave",
    "defaultDays": 10
  },
  {
    "id": 3,
    "name": "Casual Leave",
    "defaultDays": 5
  }
]
```

**Usage**: Populate dropdown when submitting leave requests.

---

## Swagger UI

Interactive API documentation available at:
```
http://localhost:8080/swagger-ui.html
```

**How to use:**
1. Click **Authorize** button (top right)
2. Enter: `Bearer {your-jwt-token}`
3. Click **Authorize** → **Close**
4. All endpoints now authenticated

**Tip**: Register or login first to get your JWT token.

---

## Balance Protection Mechanisms

### 1. Initialization at Registration
Every new user automatically gets leave balance records:
```sql
INSERT INTO leave_balances (user_id, leave_type_id, available_days, used_days, year)
VALUES 
  (1, 1, 20, 0, 2026),  -- Annual
  (1, 2, 10, 0, 2026),  -- Sick
  (1, 3, 5, 0, 2026);   -- Casual
```

**Why**: Users can immediately submit leave requests after registration (no manual setup).

### 2. Balance Validation on Submission
```java
int remaining = balance.getAvailableDays() - balance.getUsedDays();
if (remaining < request.getNumberOfDays()) {
    throw new InsufficientLeaveBalanceException(
        "Available: " + remaining + " days, Requested: " + request.getNumberOfDays()
    );
}
```

**Result**: ❌ 409 Conflict if insufficient days.

### 3. Atomic Balance Updates (@Transactional)

**Approve Leave**:
```java
@Transactional
public void approveLeave(Long id) {
    request.setStatus(APPROVED);           // Operation 1
    balance.setUsedDays(used + days);      // Operation 2
    
    // Both succeed or both fail together
}
```

**Cancel Approved Leave**:
```java
@Transactional
public void cancelLeave(Long id) {
    if (request.getStatus() == APPROVED) {
        balance.setUsedDays(used - days);  // Restore balance
    }
    request.setStatus(CANCELLED);          // Update status
    
    // Both succeed or both fail together
}
```

### 4. Final Balance Check at Approval Time
```java
@Transactional
public void approveLeave(Long id) {
    // Re-check balance (might have changed since submission)
    int remaining = balance.getAvailableDays() - balance.getUsedDays();
    if (remaining < request.getNumberOfDays()) {
        throw new InsufficientLeaveBalanceException(...);
    }
    // Proceed with approval...
}
```

**Why**: Balance might have decreased between submission and approval (other leaves approved).

### 5. Overlapping Date Detection
```java
List<LeaveRequest> overlapping = repository.findOverlappingRequests(
    userId, startDate, endDate, List.of(PENDING, APPROVED)
);
if (!overlapping.isEmpty()) {
    throw new OverlappingLeaveRequestException(...);
}
```

**Result**: ❌ 409 Conflict if dates overlap with existing leave.

---

## Role-Based Access Control

| Endpoint | EMPLOYEE | MANAGER |
|----------|----------|---------|
| `POST /api/leave/submit` | ✅ | ✅ |
| `GET /api/leave/my-requests` | ✅ | ✅ |
| `GET /api/leave/pending` | ❌ | ✅ |
| `PUT /api/leave/{id}/approve` | ❌ | ✅ |
| `PUT /api/leave/{id}/reject` | ❌ | ✅ |
| `PUT /api/leave/{id}/cancel` | ✅ (own) | ✅ (own) |
| `GET /api/leave/balance` | ✅ | ✅ |
| `GET /api/leave/balance/{userId}` | ❌ | ✅ |
| `GET /api/leave/types` | ✅ | ✅ |

**Implementation**:
- URL-based: `SecurityConfig` restricts endpoints by role
- Method-based: `@PreAuthorize("hasRole('MANAGER')")` on controller methods
- Business logic: Service layer validates ownership (e.g., cannot cancel others' requests)

---

## Testing

### Run All Tests
```bash
mvn clean test
```

**Test Coverage**:
- ✅ 42 unit tests (service layer)
- ✅ 16 integration tests (controller layer)
- ✅ **80% minimum code coverage** (enforced by JaCoCo in CI)

### Test Breakdown

**Unit Tests** (isolated, mocked dependencies):
- `AuthServiceTest`: 9 tests (registration, login, balance initialization)
- `LeaveRequestServiceTest`: 20 tests (submit, approve, reject, cancel, overlaps, balance)
- `LeaveBalanceServiceTest`: 8 tests (get balances, calculate remaining)
- `LeaveTypeServiceTest`: 5 tests (get all types)

**Integration Tests** (full Spring context, H2 database):
- `LeaveRequestControllerIntegrationTest`: 16 tests (HTTP requests, JWT auth, role-based access)

### View Coverage Report
```bash
mvn clean verify
open target/site/jacoco/index.html
```

**Coverage Breakdown** (from JaCoCo report):

| Package | Coverage | Status |
|---------|----------|--------|
| **service** | 95% ✅ | Business logic fully tested |
| **controller** | 100% ✅ | All endpoints tested |
| **security** | 99% ✅ | JWT & auth tested |
| **exception** | 35% ⚠️ | Exception handlers (invoked via controllers) |
| dto.response | 25% | Data classes (excluded from gate) |
| dto.request | 32% | Data classes (excluded from gate) |
| entity | 26% | JPA entities (excluded from gate) |
| enums | 100% | Simple enums |
| config | 100% | Spring config |

**Overall Coverage**: 41% (includes all classes)

**Business Logic Coverage** (enforced by gate): **≥ 80%** ✅

**Coverage Gate**: Build fails if:
- Instruction coverage < 80% (for services, controllers, security, exceptions, repositories)
- Branch coverage < 80% (for services, controllers, security, exceptions, repositories)
- **Excludes**: DTOs, entities, enums, config classes (data/configuration classes don't need unit tests)

**Why exclude DTOs/Entities?**
- They're pure data classes with no logic (Lombok-generated getters/setters/builders)
- Testing them would just test Lombok, not our code
- Focus testing effort on business logic where bugs occur

This ensures code quality standards before merge.

---

## Local Development (Without Docker)

### Prerequisites
- Java 17
- PostgreSQL 15
- Maven 3.9+

### Setup

1. **Create Database**
```sql
CREATE DATABASE leavedb;
CREATE USER leaveuser WITH PASSWORD 'leavepass';
GRANT ALL PRIVILEGES ON DATABASE leavedb TO leaveuser;
```

2. **Configure Environment**
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=leavedb
export DB_USER=leaveuser
export DB_PASSWORD=leavepass
export JWT_SECRET=your-secret-key-min-256-bits-replace-with-strong-random-key
export JWT_EXPIRATION=86400000
```

3. **Run Application**
```bash
mvn clean install
mvn spring-boot:run
```

4. **Access**
- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- H2 Console (test profile): http://localhost:8080/h2-console

---

## Database Migrations

**Flyway** manages schema versions automatically on application startup.

**Migration Files**: `src/main/resources/db/migration/`

| File | Description |
|------|-------------|
| `V1__create_users_table.sql` | Users and roles |
| `V2__create_leave_types_table.sql` | Leave types (Annual, Sick, Casual) |
| `V3__create_leave_balances_table.sql` | User leave balances |
| `V4__create_leave_requests_table.sql` | Leave requests and status |
| `V5__seed_initial_data.sql` | Default leave types |

**Flyway ensures**:
- Migrations run in order (V1 → V2 → V3...)
- Each migration runs exactly once
- Schema changes tracked in `flyway_schema_history` table

**Add New Migration**:
```bash
# Create file: src/main/resources/db/migration/V6__add_column.sql
ALTER TABLE leave_requests ADD COLUMN reviewer_id BIGINT;
```

On next startup, Flyway detects and applies V6 automatically.

---

## CI/CD with GitHub Actions

**Workflow**: `.github/workflows/ci.yml`

**Triggered on**:
- Push to `main` branch
- Pull requests to `main`

**Pipeline Steps**:
1. Checkout code
2. Setup JDK 17 (Temurin)
3. Cache Maven dependencies
4. Run `mvn clean verify`
   - Compile code
   - Run 58 tests (42 unit + 16 integration)
   - Generate JaCoCo coverage report
   - **Fail build if coverage < 80%**
5. Upload coverage report as artifact

**View Results**:
- GitHub Actions tab → Select workflow run
- Download `jacoco-report` artifact
- Open `index.html` in browser

**Coverage Gate**: Build fails if:
- Instruction coverage < 80%
- Branch coverage < 80%

This ensures code quality standards before merge.

---

## Project Structure

```
src/
├── main/
│   ├── java/com/precious/leavemanagement/
│   │   ├── LeaveManagementApplication.java    # Main entry point
│   │   ├── config/
│   │   │   └── OpenApiConfig.java             # Swagger configuration
│   │   ├── controller/                        # REST endpoints
│   │   │   ├── AuthController.java
│   │   │   ├── LeaveRequestController.java
│   │   │   ├── LeaveBalanceController.java
│   │   │   └── LeaveTypeController.java
│   │   ├── dto/                               # Data Transfer Objects
│   │   │   ├── request/                       # Request DTOs
│   │   │   └── response/                      # Response DTOs
│   │   ├── entity/                            # JPA entities
│   │   │   ├── User.java
│   │   │   ├── LeaveRequest.java
│   │   │   ├── LeaveBalance.java
│   │   │   └── LeaveType.java
│   │   ├── enums/                             # Enumerations
│   │   │   ├── Role.java                      # EMPLOYEE, MANAGER
│   │   │   └── LeaveStatus.java              # PENDING, APPROVED, REJECTED, CANCELLED
│   │   ├── exception/                         # Custom exceptions
│   │   │   ├── GlobalExceptionHandler.java    # @ControllerAdvice
│   │   │   ├── DuplicateResourceException.java
│   │   │   ├── InsufficientLeaveBalanceException.java
│   │   │   ├── InvalidCredentialsException.java
│   │   │   ├── LeaveRequestNotFoundException.java
│   │   │   ├── OverlappingLeaveRequestException.java
│   │   │   └── UserNotFoundException.java
│   │   ├── repository/                        # JPA repositories
│   │   │   ├── UserRepository.java
│   │   │   ├── LeaveRequestRepository.java
│   │   │   ├── LeaveBalanceRepository.java
│   │   │   └── LeaveTypeRepository.java
│   │   ├── security/                          # Security configuration
│   │   │   ├── JwtAuthFilter.java             # JWT validation filter
│   │   │   ├── JwtUtil.java                   # Token generation/parsing
│   │   │   ├── SecurityConfig.java            # Security rules
│   │   │   └── CustomUserDetailsService.java  # Load user for auth
│   │   └── service/                           # Business logic
│   │       ├── AuthService.java
│   │       ├── LeaveRequestService.java
│   │       ├── LeaveBalanceService.java
│   │       └── LeaveTypeService.java
│   └── resources/
│       ├── application.yml                    # Main configuration
│       ├── application-test.properties        # Test configuration (H2)
│       └── db/migration/                      # Flyway migrations
│           ├── V1__create_users_table.sql
│           ├── V2__create_leave_types_table.sql
│           ├── V3__create_leave_balances_table.sql
│           ├── V4__create_leave_requests_table.sql
│           └── V5__seed_initial_data.sql
└── test/
    └── java/com/precious/leavemanagement/
        ├── controller/
        │   └── LeaveRequestControllerIntegrationTest.java  # Integration tests
        └── service/                                        # Unit tests
            ├── AuthServiceTest.java
            ├── LeaveRequestServiceTest.java
            ├── LeaveBalanceServiceTest.java
            └── LeaveTypeServiceTest.java
```

---

## Documentation

- **[CONCEPTS.md](CONCEPTS.md)**: Technical concepts explained (Spring Boot, REST, JPA, JWT, @Transactional, DTOs, Docker, CI/CD)
- **[CLASS_GUIDE.md](CLASS_GUIDE.md)**: Detailed documentation of every class and method with test coverage

---

## Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DB_HOST` | PostgreSQL host | localhost | Yes |
| `DB_PORT` | PostgreSQL port | 5432 | Yes |
| `DB_NAME` | Database name | leavedb | Yes |
| `DB_USER` | Database user | leaveuser | Yes |
| `DB_PASSWORD` | Database password | - | Yes |
| `JWT_SECRET` | JWT signing key (256+ bits) | - | Yes |
| `JWT_EXPIRATION` | Token validity (milliseconds) | 86400000 (24h) | No |
| `SPRING_PROFILES_ACTIVE` | Active profile (prod/test) | prod | No |

**Security Note**: Never commit `.env` file or hardcode secrets. Use environment variables or secret management tools.

---

## Troubleshooting

### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>
```

### Database Connection Failed
```bash
# Check PostgreSQL is running
docker-compose ps

# View logs
docker-compose logs postgres

# Restart database
docker-compose restart postgres
```

### Coverage Below 80%
```bash
# Generate coverage report
mvn clean verify

# View report
open target/site/jacoco/index.html

# Add tests for uncovered code
# Focus on service layer (business logic)
```

### JWT Token Expired
- Default expiration: 24 hours
- Login again to get new token
- Adjust `JWT_EXPIRATION` environment variable if needed

---

## License

This project is for educational purposes. Modify and use as needed.

---

## Contact

For questions or issues, please create an issue in the repository.
