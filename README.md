# Finance Data Processing and Access Control Backend
### Assignment Submission — Zorvyn Assignment Portal
**Submitted by:** Suriyaprakash K | ksuriyaprakash12@gmail.com
**Role Applied:** Backend Developer Intern
**Deadline:** Mon, 06 Apr, 2026

---

## Table of Contents

1. [Project Summary](#1-project-summary)
2. [Technology Stack](#2-technology-stack)
3. [Project Architecture](#3-project-architecture)
4. [How Each Requirement Is Met](#4-how-each-requirement-is-met)
5. [Database Schema](#5-database-schema)
6. [API Endpoints Reference](#6-api-endpoints-reference)
7. [Access Control Matrix](#7-access-control-matrix)
8. [Setup and Running Instructions](#8-setup-and-running-instructions)
9. [Testing the API](#9-testing-the-api)
10. [Assumptions Made](#10-assumptions-made)
11. [Design Decisions and Trade-offs](#11-design-decisions-and-trade-offs)
12. [Optional Enhancements Implemented](#12-optional-enhancements-implemented)
13. [What I Would Add Next](#13-what-i-would-add-next)

---

## 1. Project Summary

This project is a fully functional **Finance Dashboard Backend REST API** built with **Java 17 + Spring Boot 3.2**. It supports multi-role user management, financial record CRUD with filtering and pagination, aggregated dashboard analytics, and stateless JWT-based authentication — all enforced with a two-layer role-based access control system.

The backend is designed to serve a finance dashboard frontend. Different users see and do different things based on their assigned role.

### Repository Structure at a Glance

```
finance-dashboard/
├── pom.xml                          # Maven build + all dependencies
├── README.md                        # Setup guide
├── SUBMISSION_REPORT.md             # This file
└── src/
    └── main/
        ├── java/com/finance/dashboard/
        │   ├── FinanceDashboardApplication.java
        │   ├── config/              # Security, Swagger, Data seeding
        │   ├── controller/          # HTTP layer — 4 controllers
        │   ├── dto/                 # Request and Response shapes
        │   │   ├── request/
        │   │   └── response/
        │   ├── entity/              # JPA entities (DB tables)
        │   ├── enums/               # Role, TransactionType
        │   ├── exception/           # Custom exceptions + global handler
        │   ├── repository/          # Spring Data JPA + custom JPQL
        │   ├── security/            # JWT filter, UserDetailsService
        │   └── service/impl/        # Business logic layer
        └── resources/
            └── application.properties
```

---

## 2. Technology Stack

| Layer | Choice | Reason |
|---|---|---|
| Language | Java 17 | LTS release, modern features (records, sealed classes), required by Spring Boot 3 |
| Framework | Spring Boot 3.2 | Industry standard for Java REST APIs. Auto-configuration, embedded Tomcat. |
| Security | Spring Security 6 + JWT (jjwt 0.12) | Stateless auth — no sessions, scales horizontally. JWT is the industry standard. |
| ORM | Spring Data JPA + Hibernate 6 | Eliminates boilerplate SQL. Custom JPQL queries for complex aggregations. |
| Database | MySQL 8 | Relational model suits financial data. ACID compliance. Widely used in FinTech. |
| Validation | Jakarta Bean Validation | Declarative input validation via annotations (`@NotBlank`, `@Email`, `@DecimalMin`). |
| Documentation | SpringDoc OpenAPI 2 (Swagger UI) | Auto-generated, interactive API docs. Evaluators can test every endpoint in browser. |
| Build | Apache Maven 3.8 | Standard Java build tool. Dependency management via pom.xml. |

---

## 3. Project Architecture

The project follows a clean **layered architecture** with strict separation of concerns:

```
HTTP Request
     │
     ▼
┌─────────────────────────────────┐
│   JwtAuthenticationFilter       │  ← Reads Bearer token, validates, sets SecurityContext
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│   SecurityConfig (Route Rules)  │  ← Layer 1 access control: role vs HTTP method + path
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│   Controller   (@PreAuthorize)  │  ← Layer 2 access control + request validation
│   AuthController                │
│   UserController                │
│   FinancialRecordController     │
│   DashboardController           │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│   Service Layer (Business Logic)│
│   AuthServiceImpl               │
│   UserServiceImpl               │
│   FinancialRecordServiceImpl    │
│   DashboardServiceImpl          │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│   Repository Layer (Data Access)│
│   UserRepository                │
│   FinancialRecordRepository     │  ← Custom JPQL: filters, aggregations, trends
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│   MySQL 8 Database              │
│   users table                   │
│   financial_records table       │
└─────────────────────────────────┘
                 │
     (Any exception thrown at any layer)
                 │
                 ▼
┌─────────────────────────────────┐
│   GlobalExceptionHandler        │  ← Converts all exceptions → structured JSON
└─────────────────────────────────┘
```

### Key Architectural Principles Applied

- **Interface + Implementation separation** — Every service has an interface (`UserService`) and a concrete implementation (`UserServiceImpl`). This makes future changes easier — if the data source or business logic changes, only the implementation changes, not the interface or controllers.
- **DTO pattern** — Entities never leave the service layer. Request DTOs carry validated input. Response DTOs carry shaped output. The internal entity model is completely hidden from API consumers.
- **Repository pattern** — All database access goes through Spring Data repositories. Business logic never writes raw SQL.
- **Fail fast** — Input validation happens at the controller boundary before any service code runs. Invalid data never reaches the business logic.

---

## 4. How Each Requirement Is Met

### Requirement 1 — User and Role Management ✅

**Creating and managing users**

`UserController` exposes full CRUD:
- `POST /users` — create with name, email, password, role
- `GET /users` — list all users
- `GET /users/{id}` — get by ID
- `PUT /users/{id}` — update name / role / status (partial — only non-null fields applied)
- `DELETE /users/{id}` — soft-deactivate (sets `active = false`)

**Assigning roles to users**

Every user has exactly one `Role` (enum: `VIEWER`, `ANALYST`, `ADMIN`). Role is set at creation and can be changed by ADMIN via `PUT /users/{id}`.

**Managing user status (active/inactive)**

The `User` entity has an `active` boolean field. Deactivation sets it to `false`. Spring Security's `UserDetails` uses `active` as the `enabled` flag — deactivated users cannot log in even with the correct password.

**Restricting actions based on roles**

Two-layer enforcement (explained in Requirement 4).

---

### Requirement 2 — Financial Records Management ✅

**Entity fields:**

```java
// FinancialRecord.java
BigDecimal amount         // DECIMAL(15,2) — no floating-point errors
TransactionType type      // ENUM: INCOME | EXPENSE
String category           // VARCHAR(100)
LocalDate transactionDate // DATE
String notes              // VARCHAR(500), nullable
boolean deleted           // soft-delete flag
User createdBy            // FK → users.id (LAZY fetch)
```

**CRUD operations via `FinancialRecordController`:**

| Operation | Endpoint | Auth |
|---|---|---|
| Create | `POST /records` | ADMIN |
| Read (list) | `GET /records` | ADMIN, ANALYST |
| Read (single) | `GET /records/{id}` | ADMIN, ANALYST |
| Update | `PUT /records/{id}` | ADMIN |
| Delete (soft) | `DELETE /records/{id}` | ADMIN |

**Filtering** — `GET /records` accepts all of these as optional query parameters:

```
?type=EXPENSE           # filter by INCOME or EXPENSE
&category=Food          # partial match (case-insensitive LIKE)
&from=2024-01-01        # start date inclusive
&to=2024-01-31          # end date inclusive
&page=0&size=10         # pagination
&sortBy=transactionDate # sort field
&sortDir=desc           # sort direction
```

All filter logic is in a single JPQL query using `IS NULL OR` guards so that omitting a filter returns all records:

```java
// FinancialRecordRepository.java
@Query("""
    SELECT r FROM FinancialRecord r
    WHERE r.deleted = false
      AND (:type     IS NULL OR r.type     = :type)
      AND (:category IS NULL OR LOWER(r.category) LIKE LOWER(CONCAT('%', :category, '%')))
      AND (:from     IS NULL OR r.transactionDate >= :from)
      AND (:to       IS NULL OR r.transactionDate <= :to)
    """)
Page<FinancialRecord> findAllWithFilters(..., Pageable pageable);
```

---

### Requirement 3 — Dashboard Summary APIs ✅

`GET /dashboard/summary` returns a single response object containing:

| Field | How Computed |
|---|---|
| `totalIncome` | `SELECT SUM(amount) WHERE type = INCOME AND deleted = false` |
| `totalExpenses` | `SELECT SUM(amount) WHERE type = EXPENSE AND deleted = false` |
| `netBalance` | `totalIncome - totalExpenses` (computed in Java, not SQL) |
| `incomeByCategory` | `SELECT category, SUM(amount) GROUP BY category WHERE type = INCOME` |
| `expenseByCategory` | `SELECT category, SUM(amount) GROUP BY category WHERE type = EXPENSE` |
| `recentActivity` | Last 5 records ordered by `created_at DESC` |
| `monthlyTrends` | Year + month + type + SUM grouped, filtered to last 6 months |

The monthly trends are assembled in Java from raw rows into a `List<MonthlyTrend>` where each entry has `{ year, month, income, expense }` — ready to be charted on the frontend.

---

### Requirement 4 — Access Control Logic ✅

Access control is enforced at **two independent layers**:

**Layer 1 — Route level (`SecurityConfig.java`)**

```java
.requestMatchers("/users/**").hasRole("ADMIN")
.requestMatchers(HttpMethod.POST,   "/records/**").hasRole("ADMIN")
.requestMatchers(HttpMethod.PUT,    "/records/**").hasRole("ADMIN")
.requestMatchers(HttpMethod.DELETE, "/records/**").hasRole("ADMIN")
.requestMatchers(HttpMethod.GET,    "/records/**").hasAnyRole("ADMIN", "ANALYST")
.requestMatchers("/dashboard/**").hasAnyRole("ADMIN", "ANALYST", "VIEWER")
```

**Layer 2 — Method level (`@PreAuthorize` on every controller method)**

```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<FinancialRecordResponse>> create(...) { ... }
```

Why two layers? If a route rule is ever misconfigured (e.g., during refactoring), the method-level check still prevents unauthorized access. Defence in depth.

---

### Requirement 5 — Validation and Error Handling ✅

**Input validation** — Request DTOs use Jakarta Bean Validation annotations:

```java
// CreateUserRequest.java
@NotBlank(message = "Name is required")
@Size(min = 2, max = 100)
private String name;

@Email(message = "Must be a valid email address")
@NotBlank
private String email;

@Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
         message = "Password must contain at least one uppercase letter and one digit")
private String password;

// FinancialRecordRequest.java
@DecimalMin(value = "0.01", message = "Amount must be greater than 0")
@Digits(integer = 13, fraction = 2)
private BigDecimal amount;
```

**Global exception handling** — `GlobalExceptionHandler` catches every exception type and returns a consistent JSON error envelope:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "email": "Must be a valid email address",
    "amount": "Amount must be greater than 0"
  }
}
```

**Status codes used:**

| Code | When |
|---|---|
| 200 OK | Successful read/update/delete |
| 201 Created | Successful resource creation |
| 400 Bad Request | Validation failure or invalid input |
| 401 Unauthorized | Missing or invalid/expired JWT |
| 403 Forbidden | Authenticated but insufficient role |
| 404 Not Found | Resource does not exist |
| 409 Conflict | Email already registered |
| 500 Internal Server Error | Unexpected failure (logged server-side) |

---

### Requirement 6 — Data Persistence ✅

**MySQL 8** is used as the relational database.

Hibernate `ddl-auto=update` automatically creates and maintains the schema — no manual SQL scripts are needed to set up tables.

**`BigDecimal` for all monetary values** — `float` and `double` have well-known precision errors for financial arithmetic (e.g., `0.1 + 0.2 != 0.3`). All amounts use `BigDecimal` with `DECIMAL(15, 2)` in MySQL.

**Database indexes** on the most-queried columns of `financial_records`:

```java
@Table(indexes = {
    @Index(name = "idx_record_date",     columnList = "transactionDate"),
    @Index(name = "idx_record_type",     columnList = "type"),
    @Index(name = "idx_record_category", columnList = "category"),
    @Index(name = "idx_record_deleted",  columnList = "deleted")
})
```

---

## 5. Database Schema

### `users` table

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| name | VARCHAR(100) | NOT NULL |
| email | VARCHAR(150) | NOT NULL, UNIQUE |
| password | VARCHAR(255) | NOT NULL (BCrypt hashed) |
| role | ENUM | NOT NULL — VIEWER, ANALYST, ADMIN |
| active | BOOLEAN | NOT NULL, DEFAULT true |
| created_at | DATETIME | NOT NULL, auto-set on insert |
| updated_at | DATETIME | NOT NULL, auto-set on update |

### `financial_records` table

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| amount | DECIMAL(15,2) | NOT NULL |
| type | ENUM | NOT NULL — INCOME, EXPENSE |
| category | VARCHAR(100) | NOT NULL |
| transaction_date | DATE | NOT NULL |
| notes | VARCHAR(500) | nullable |
| deleted | BOOLEAN | NOT NULL, DEFAULT false |
| created_by | BIGINT | FK → users.id, NOT NULL |
| created_at | DATETIME | NOT NULL, auto-set on insert |
| updated_at | DATETIME | NOT NULL, auto-set on update |

### Entity Relationship

```
users (1) ────────── (many) financial_records
       └─── id ←─── created_by (FK)
```

One user can create many financial records. Records are never orphaned — the `created_by` reference tracks who entered each record.

---

## 6. API Endpoints Reference

**Base URL:** `http://localhost:8080/api`

**Authentication:** All endpoints except `/auth/login` require:
```
Authorization: Bearer <JWT token>
```

### Authentication

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/login` | Public | Get JWT token |

**Request body:**
```json
{ "email": "admin@finance.com", "password": "Admin@123" }
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "email": "admin@finance.com",
    "name": "System Admin",
    "role": "ADMIN"
  }
}
```

---

### User Management

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/users/me` | Any | Get own profile |
| GET | `/users` | ADMIN | List all users |
| GET | `/users/{id}` | ADMIN | Get user by ID |
| POST | `/users` | ADMIN | Create new user |
| PUT | `/users/{id}` | ADMIN | Update user (partial) |
| DELETE | `/users/{id}` | ADMIN | Soft-deactivate user |

---

### Financial Records

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/records` | ADMIN, ANALYST | List records (filtered, paginated) |
| GET | `/records/{id}` | ADMIN, ANALYST | Get single record |
| POST | `/records` | ADMIN | Create record |
| PUT | `/records/{id}` | ADMIN | Update record (full replace) |
| DELETE | `/records/{id}` | ADMIN | Soft-delete record |

**Filter parameters for `GET /records`:**

| Param | Type | Description |
|---|---|---|
| type | INCOME \| EXPENSE | Filter by transaction type |
| category | string | Partial case-insensitive match |
| from | yyyy-MM-dd | Start date (inclusive) |
| to | yyyy-MM-dd | End date (inclusive) |
| page | int (default 0) | Page number |
| size | int (default 10) | Items per page |
| sortBy | string | Field to sort by |
| sortDir | asc \| desc | Sort direction |

---

### Dashboard

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/dashboard/summary` | ADMIN, ANALYST, VIEWER | Full analytics summary |

**Response structure:**
```json
{
  "totalIncome": 150000.00,
  "totalExpenses": 42500.00,
  "netBalance": 107500.00,
  "incomeByCategory": { "Salary": 120000.00, "Freelance": 30000.00 },
  "expenseByCategory": { "Rent": 20000.00, "Food": 8500.00 },
  "recentActivity": [ ...last 5 records... ],
  "monthlyTrends": [
    { "year": 2024, "month": 1, "income": 25000.00, "expense": 7000.00 }
  ]
}
```

---

## 7. Access Control Matrix

| Endpoint | VIEWER | ANALYST | ADMIN |
|---|:---:|:---:|:---:|
| `POST /auth/login` | ✅ | ✅ | ✅ |
| `GET /users/me` | ✅ | ✅ | ✅ |
| `GET /users` | ❌ | ❌ | ✅ |
| `GET /users/{id}` | ❌ | ❌ | ✅ |
| `POST /users` | ❌ | ❌ | ✅ |
| `PUT /users/{id}` | ❌ | ❌ | ✅ |
| `DELETE /users/{id}` | ❌ | ❌ | ✅ |
| `GET /records` | ❌ | ✅ | ✅ |
| `GET /records/{id}` | ❌ | ✅ | ✅ |
| `POST /records` | ❌ | ❌ | ✅ |
| `PUT /records/{id}` | ❌ | ❌ | ✅ |
| `DELETE /records/{id}` | ❌ | ❌ | ✅ |
| `GET /dashboard/summary` | ✅ | ✅ | ✅ |

---

## 8. Setup and Running Instructions

### Prerequisites

- Java 17 (JDK)
- Maven 3.8+
- MySQL 8

### Step 1 — Create MySQL database

```sql
CREATE DATABASE finance_dashboard;
```

### Step 2 — Configure credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/finance_dashboard?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

### Step 3 — Run the application

```bash
# From the project root (where pom.xml is)
mvn spring-boot:run
```

Or build a JAR:

```bash
mvn clean package -DskipTests
java -jar target/dashboard-1.0.0.jar
```

### Step 4 — Access Swagger UI

```
http://localhost:8080/api/swagger-ui.html
```

Once this page loads, the server is running correctly. Use Swagger UI to test all endpoints interactively in the browser without any extra tool.

### Default Admin Credentials

| Field | Value |
|---|---|
| Email | admin@finance.com |
| Password | Admin@123 |
| Role | ADMIN |

---

## 9. Testing the API

### Quick test sequence using curl

```bash
# 1. Login — get your token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@finance.com","password":"Admin@123"}'

# Copy the token from the response, then:
TOKEN="eyJhbGci..."

# 2. Create a financial record
curl -X POST http://localhost:8080/api/records \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 75000.00,
    "type": "INCOME",
    "category": "Salary",
    "transactionDate": "2024-01-15",
    "notes": "January salary"
  }'

# 3. Create an ANALYST user
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Priya Analyst",
    "email": "priya@finance.com",
    "password": "Analyst1",
    "role": "ANALYST"
  }'

# 4. View the dashboard
curl -X GET http://localhost:8080/api/dashboard/summary \
  -H "Authorization: Bearer $TOKEN"

# 5. Filter records by type and date range
curl -X GET "http://localhost:8080/api/records?type=INCOME&from=2024-01-01&to=2024-01-31" \
  -H "Authorization: Bearer $TOKEN"

# 6. Test role enforcement — VIEWER cannot access records
# Login as viewer first, then:
curl -X GET http://localhost:8080/api/records \
  -H "Authorization: Bearer $VIEWER_TOKEN"
# Expected: 403 Forbidden
```

---

## 10. Assumptions Made

1. **One role per user.** A user has exactly one role. There is no role hierarchy (e.g., ADMIN does not automatically inherit ANALYST permissions — both are explicitly granted in `SecurityConfig`).

2. **ADMIN is the only writer.** Only ADMIN can create, update, or delete financial records. ANALYST is a read-only consumer of records. If the requirement were for analysts to also create records, `SecurityConfig` and `@PreAuthorize` annotations on the relevant endpoints would be updated.

3. **Category is free-text.** There is no master category table. Categories like "Salary", "Food", "Rent" are plain strings entered by the user. This keeps the schema simple. A `Category` entity could be added later to enforce a controlled vocabulary.

4. **Soft delete is permanent via API.** A soft-deleted record's `deleted = true` flag cannot be unset through the API. Restoration would require a direct database update. This is acceptable for an assignment; production systems would expose a restore endpoint.

5. **Monthly trends span last 6 months.** The dashboard always returns the previous 6 calendar months. This window is hardcoded in `DashboardServiceImpl`. A date range parameter could be added to make it configurable.

6. **JWT expiry is 24 hours.** Configurable via `app.jwt.expiration-ms`. Refresh tokens are not implemented — the user simply logs in again when the token expires.

7. **No multi-tenancy.** All users share the same pool of financial records. There is no per-organisation or per-account isolation. This matches the assignment description.

8. **No email verification.** User accounts are created by ADMIN and are immediately active. In a real product, email verification would be required before a user can log in.

---

## 11. Design Decisions and Trade-offs

### Decision 1 — Two-layer access control (SecurityConfig + @PreAuthorize)

**Why:** A single layer is sufficient for simple cases, but layered defence is more robust. If routes are reorganised during development, the method-level `@PreAuthorize` still blocks unauthorised access. The cost is a small amount of duplication (each rule appears twice). The benefit is resilience to refactoring errors.

---

### Decision 2 — BigDecimal for all monetary values

**Why:** `double` and `float` have binary floating-point representation errors. `0.1 + 0.2` in a double context does not equal `0.3`. For financial software, even a rounding error of Rs. 0.01 is unacceptable at scale. `BigDecimal` is slower and more verbose, but it guarantees exact decimal arithmetic. Mapped to `DECIMAL(15, 2)` in MySQL for the same reason.

---

### Decision 3 — Soft delete on both entities

**Why:** Financial data should never be physically erased. Regulatory requirements in real FinTech products mandate that transaction history is immutable. Soft delete (setting `deleted = true` or `active = false`) hides records from normal queries while keeping them in the database for audit purposes. The cost is slightly more complex queries (every query must filter `WHERE deleted = false`).

---

### Decision 4 — Interface + Implementation separation for services

**Why:** Following the Spring convention of `UserService` (interface) + `UserServiceImpl` (class) cleanly separates the contract from the implementation. If the data source changes in the future (e.g., switching from MySQL to PostgreSQL, or adding a caching layer between service and repository), only the implementation file changes — controllers and other callers are completely unaffected. This is the standard Spring layering pattern and makes the codebase easy to maintain and extend.

---

### Decision 5 — Database indexes on filter columns

**Why:** The four columns used most often in WHERE clauses — `transactionDate`, `type`, `category`, `deleted` — each have a dedicated index. Without indexes, filtering a table of 100,000 records requires a full table scan. With indexes, the same query uses an index seek — orders of magnitude faster. The trade-off is slightly slower INSERT operations (the index must be updated), which is acceptable here since reads vastly outnumber writes in a dashboard system.

---

### Decision 6 — JPQL instead of native SQL for aggregations

**Why:** JPQL (Hibernate's object-oriented query language) keeps queries database-agnostic. The same queries work on MySQL, PostgreSQL, and other databases without modification. If the database engine is ever changed, no query rewriting is required. Native SQL would lock the code to MySQL-specific syntax and create a tighter coupling between the business logic and the underlying database engine.

---

### Decision 7 — Paginated response for record listing

**Why:** A finance dashboard may accumulate thousands of records over months. Returning all records in a single response would be slow and memory-intensive. Pagination limits every response to a manageable number of records and includes metadata (`totalElements`, `totalPages`) so the frontend can display navigation controls.

---

## 12. Optional Enhancements Implemented

The following optional enhancements from the assignment brief are implemented:

| Enhancement | Status | Where |
|---|---|---|
| JWT Authentication | ✅ Implemented | `security/JwtUtils.java`, `JwtAuthenticationFilter.java` |
| Pagination | ✅ Implemented | `GET /records` with `page`, `size`, `sortBy`, `sortDir` params |
| Search / filtering | ✅ Implemented | `GET /records` with `type`, `category`, `from`, `to` params |
| Soft delete | ✅ Implemented | `deleted` flag on `FinancialRecord`, `active` flag on `User` |
| API documentation (Swagger) | ✅ Implemented | `http://localhost:8080/api/swagger-ui.html` |
| Unit tests / integration tests | ❌ Not included | Planned for future — see Section 13 |

---

## 13. What I Would Add Next

If this were a production deployment or if the timeline were longer, I would add:

1. **Unit and integration tests** — JUnit 5 + Mockito for service layer unit tests, MockMvc for controller integration tests. Testing was not included in this submission due to time constraints and unfamiliarity with the testing framework at this stage, but it is the next skill I am actively learning.

2. **Refresh tokens** — Issue a short-lived access token (15 min) and a long-lived refresh token (7 days). The user exchanges the refresh token for a new access token without re-entering credentials.

3. **Rate limiting** — Use Bucket4j or Spring's rate-limiting support to limit login attempts (prevent brute-force attacks) and limit dashboard API calls per user per minute.

4. **Audit log table** — A separate `audit_log` table recording every create/update/delete with the user who performed it, the old value, and the new value. Essential for FinTech compliance.

5. **Restore endpoint for soft-deleted records** — `PATCH /records/{id}/restore` to allow ADMIN to undelete a record.

6. **Category master table** — A `categories` table with predefined category names. Financial records would reference a category by ID instead of a free-text string, ensuring consistent categorisation.

7. **Docker Compose** — A `docker-compose.yml` to spin up both the Spring Boot app and a MySQL container with a single command, making local setup trivial.

8. **Environment-specific configuration** — Separate `application-dev.properties` and `application-prod.properties` profiles with different log levels, database connections, and security settings.

---

## Declaration

This is my own original work. The backend was designed, architected, and implemented by me for this assignment. All design decisions, trade-offs, and assumptions are documented above.

The project demonstrates:
- Clean layered architecture with strict separation of concerns
- Secure stateless JWT authentication
- Role-based access control enforced at two independent layers
- Correct financial data modeling using BigDecimal and relational schema
- Aggregated dashboard analytics with JPQL
- Comprehensive input validation and structured error handling
- Paginated, filterable record listing
- Soft delete on both entities
- Interactive API documentation via Swagger UI

---

*Submitted by: Suriyaprakash K — ksuriyaprakash12@gmail.com*
*Assignment: Finance Data Processing and Access Control Backend — Zorvyn*
*Date: April 06, 2026*
