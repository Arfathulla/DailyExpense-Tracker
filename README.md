# SpendLedger: Expense Tracker

A full-stack personal finance app. **Spring Boot 3 (Java 17) + Spring Security (JWT) + JPA/SQL** on the back end, and a **vanilla HTML/CSS/JS** front end served by the same app.

Track income and expenses, set monthly budgets with 80% / 100% warnings, see charts, automate recurring items, and export to CSV.

## Features
- Register / login with JWT (BCrypt-hashed passwords, stateless sessions)
- Income and expense transactions with categories; filter by date, type, category and note search; sorting and pagination
- Monthly budgets per category, with `OK` / `WARNING` (80%) / `EXCEEDED` (100%) status
- Dashboard: monthly summary, category doughnut, 6-month income vs expense chart, budget alerts
- Recurring transactions created by a `@Scheduled` job (idempotent, catches up after downtime)
- CSV export (respects filters, protects against spreadsheet formula injection)
- Monthly summary email (HTML + plain text over SMTP on the 1st at 08:00; logs only until you enable it)
- Swagger UI at `/swagger-ui.html`, light and dark themes, responsive layout

## Quick start (no database needed)
Requires Java 17+ and Maven.

```bash
mvn spring-boot:run
```
Open http://localhost:8080 and click **Use the demo account** (`demo@example.com` / `Demo@123`). Demo data is seeded automatically, in an in-memory H2 database (reset on restart).

## Run with MySQL
```bash
# 1. create a database
mysql -u root -p -e "CREATE DATABASE expensetracker"

# 2. run with environment variables
DB_URL=jdbc:mysql://localhost:3306/expensetracker DB_USER=root DB_PASSWORD=yourpw \
JWT_SECRET=$(openssl rand -hex 32) SEED_DEMO=false mvn spring-boot:run
```
The schema is created by Flyway on startup (see [Database migrations](#database-migrations)).

## Run with Docker
```bash
docker compose up --build
```
App on http://localhost:8080, MySQL on port 3307. Emails are caught by Mailpit at http://localhost:8025.

## Configuration
| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | in-memory H2 | Database connection |
| `JWT_SECRET` | dev placeholder | **Set a random 32+ character value in production** |
| `SEED_DEMO` | `true` | Create the demo user and sample data |
| `PORT` | `8080` | Server port |
| `MAIL_ENABLED` | `false` | `true` sends real monthly summary emails; `false` only logs them |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` | SMTP server |
| `MAIL_USER` / `MAIL_PASSWORD` | empty | SMTP credentials |
| `MAIL_SMTP_AUTH` / `MAIL_STARTTLS` | `false` / `false` | Set both to `true` for most hosted providers (port 587) |
| `MAIL_FROM` | `SpendLedger <no-reply@spendledger.local>` | Sender address (use one your provider allows) |
| `MAIL_CURRENCY` | `₹` | Currency symbol in emails |

## Database migrations
Schema changes are Flyway migrations in `src/main/resources/db/migration` (`V1__init_schema.sql` is the baseline). Hibernate runs with `ddl-auto=validate`, so the app refuses to start if entities and schema disagree. To change the schema, add `V2__describe_change.sql`; never edit a migration that has been applied.

The SQL is written to run on both MySQL and the default in-memory H2 (MySQL mode). A MySQL database that was created earlier by `ddl-auto=update` is adopted as V1 automatically (`baseline-on-migrate`); check that it matches `V1` first.

## Monthly summary email
On the 1st at 08:00 each user with activity last month gets an email with income, expense, balance and their top five spending categories. Try it locally with `docker compose up` and open Mailpit, or point `MAIL_HOST` at a real SMTP server. A failure for one user is logged and does not stop the others.

## Architecture
```
src/main/java/com/expensetracker
├── config       SecurityConfig, DataSeeder
├── controller   REST endpoints (thin)
├── service      Business rules, @Transactional
├── repository   Spring Data JPA, custom JPQL, Specifications
├── entity       User, Category, Transaction, Budget, RecurringTransaction
├── dto          Request/response records with Bean Validation
├── security     JwtService, JwtFilter
├── exception    GlobalExceptionHandler (consistent JSON errors)
└── scheduler    RecurringTransactionJob, MonthlySummaryJob
src/main/resources/static   HTML pages, css/style.css, js/ (ES modules)
```

## API
All endpoints except `/api/auth/*` need `Authorization: Bearer <token>`.

| Area | Endpoint |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login` |
| Categories | `GET/POST /api/categories`, `PUT/DELETE /api/categories/{id}` |
| Transactions | `GET /api/transactions?from&to&categoryId&type&q&page&size&sortBy&dir`, `POST`, `PUT/DELETE /{id}`, `GET /export` |
| Budgets | `GET /api/budgets?month=2026-10`, `POST` (create or update), `PUT/DELETE /{id}` |
| Analytics | `GET /api/analytics/summary`, `/by-category`, `/monthly-trend?months=6` |
| Recurring | `GET/POST /api/recurring`, `DELETE /api/recurring/{id}` |

## Design decisions worth discussing
- **Data isolation:** every query is scoped by the authenticated user's id (taken from the JWT, never from the request body), so users can't read or change each other's data. Foreign resources return 404.
- **Money is `DECIMAL(12,2)` / `BigDecimal`**, never floating point.
- **Indexing:** composite index on `transactions(user_id, txn_date)` supports the date-range filters and aggregations.
- **Aggregation in SQL:** totals, category breakdowns and monthly trends use `GROUP BY` queries, not in-memory loops. See `docs/queries.sql` for plain-SQL versions including window functions.
- **Idempotent scheduling:** `lastRunMonth` on each recurring rule prevents duplicate transactions if the job runs twice.
- **Token storage tradeoff:** the JWT is kept in `localStorage` for simplicity, which is exposed to XSS. The front end escapes all user content before rendering. A production hardening step would be an httpOnly cookie plus CSRF protection.

## Tests
```bash
mvn test
```
- Unit tests: JWT (tampered and expired tokens), budget thresholds, email composition (including HTML escaping), SMTP sender, and the summary job.
- Integration tests (`src/test/java/.../integration`) run the whole app against a real **MySQL 8.4 in Docker** using Testcontainers: Flyway migrations plus Hibernate schema validation, the REST flow, per-user data isolation, budget statuses, aggregate queries, and the scheduled jobs. They need Docker running and are skipped automatically when it is not.

