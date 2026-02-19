# Voucher-Promo (Voucher & Promo MVP)

Monorepo that implements a minimal Voucher & Promo module plus a small "connectivity test app" proving **Frontend <-> Backend <-> Database** integration end-to-end.

## Architecture

- `frontend/`: React + Vite + Tailwind (connectivity dashboard + forms).
- `backend/`: Spring Boot (Gradle), REST APIs.
- `docker-compose.yml`: local MySQL.

Data flow:
1. React UI calls Spring Boot endpoints (`/actuator/health`, `/vouchers/*`, `/admin/vouchers`).
2. Spring Boot persists vouchers/redemptions in MySQL via JPA + Flyway migrations.

## Local Setup

### Prereqs
- Java 17+ (backend targets Java 17 bytecode; local dev can use newer JDKs).
- Node.js 20+ (frontend).
- Docker (for local MySQL and for Testcontainers-backed integration tests).

### Start MySQL
```bash
docker compose up -d
```

### Run Backend
```bash
./gradlew :backend:bootRun
```

Backend defaults to:
- `DB_HOST=localhost`
- `DB_PORT=3306`
- `DB_NAME=voucherpromo`
- `DB_USER=app`
- `DB_PASSWORD=app`
- `PORT=8080`

### Run Frontend (Dev)
```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`.

Dev proxy is configured in `frontend/vite.config.ts` so the frontend can call the backend without CORS:
- `/actuator` -> `http://localhost:8080`
- `/vouchers` -> `http://localhost:8080`
- `/admin` -> `http://localhost:8080`

## Environment Variables

Backend reads DB configuration from environment variables:
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

Other:
- `ADMIN_TOKEN` (demo guard for `POST /admin/vouchers`, default: `dev-admin-token`)
- `PORT` (server port, default: `8080`)

## API (MVP)

### Health
```bash
curl http://localhost:8080/actuator/health
```

### CSRF (Required for POST)
This backend enables cookie-based CSRF protection. The React frontend handles it automatically.

For `curl`, fetch the CSRF cookie once and then send it as a header on every POST:
```bash
curl -s -c cookies.txt http://localhost:8080/csrf > /dev/null
CSRF=$(awk '$6=="XSRF-TOKEN"{print $7}' cookies.txt)
```

### List Active Vouchers
```bash
curl http://localhost:8080/vouchers/active
```

### Admin: Create Voucher (Demo)
Guarded by `X-Admin-Token` header (value comes from `ADMIN_TOKEN` env var).

```bash
curl -X POST http://localhost:8080/admin/vouchers \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: dev-admin-token" \
  -b cookies.txt \
  -H "X-XSRF-TOKEN: $CSRF" \
  -d '{
    "code": "DEMO10",
    "discountType": "FIXED",
    "discountValue": 10.00,
    "startAt": "2026-02-19T00:00:00",
    "endAt": "2026-03-01T00:00:00",
    "minSpend": null,
    "quotaTotal": 5
  }'
```

### Validate Voucher
```bash
curl -X POST http://localhost:8080/vouchers/validate \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -H "X-XSRF-TOKEN: $CSRF" \
  -d '{ "code": "DEMO10", "orderAmount": 100.00 }'
```

### Claim Voucher (Idempotent + Concurrency-Safe)
Idempotency key: `orderId`.

```bash
curl -X POST http://localhost:8080/vouchers/claim \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -H "X-XSRF-TOKEN: $CSRF" \
  -d '{ "code": "DEMO10", "orderId": "ORDER-001", "orderAmount": 100.00 }'
```

## Voucher Claim Guarantees

- **Idempotency by `orderId`**: redemption records are uniquely constrained by `(voucher_id, order_id)`. Retrying the same `orderId` returns the original discount and does not decrement quota twice.
- **Concurrency-safe quota**: the claim transaction takes a row lock (`SELECT ... FOR UPDATE`) on the voucher to serialize concurrent claims and prevent quota leaks/negatives under race conditions.

## Tests & Coverage

### Run Tests
```bash
./gradlew test
```

Notes:
- DB-backed tests use Testcontainers MySQL when Docker is available.

### JaCoCo Coverage
```bash
./gradlew :backend:jacocoTestReport
```

Report location:
- `backend/build/reports/jacoco/test/html/index.html`

## CI / Code Scanning (Module 02)

### CI
Workflow: `.github/workflows/ci.yml`
- Triggers: `pull_request` and `push` to non-`main` branches
- Backend: Gradle tests + JaCoCo report
- Frontend: `npm ci`, `npm run lint`, `npm run build`

### OSSF Scorecard
Workflow: `.github/workflows/scorecard.yml`
- Runs on `main` and on a weekly schedule
- Uploads SARIF results to GitHub Code Scanning

### PMD (Required)
Workflow: `.github/workflows/pmd.yml`
- Trigger: every `push` to every branch
- Fail policy: **fail on any Priority 1-3 violations** (configured in `backend/build.gradle.kts`)

PMD issue fixed (separate commit):
- Rule: `AvoidCatchingGenericException`
- Fix: narrowed `catch (Exception)` to `catch (IllegalArgumentException)` in `backend/src/test/java/com/example/demo/voucher/VoucherClaimConcurrencyTest.java`

### Extra Workflows
- Dependency Review: `.github/workflows/dependency-review.yml`
- CodeQL: `.github/workflows/codeql.yml`

## CD: Deploy Staging (Koyeb)

Workflow: `.github/workflows/deploy-staging.yml`
- Deploys **only on push to `main`** (after PR merge).
- Uses Dockerfile-based deployment (`Dockerfile`).

### Required GitHub Secrets
- `KOYEB_API_TOKEN`
- `KOYEB_APP_NAME`
- `KOYEB_SERVICE_NAME`
- `STAGING_DB_HOST`
- `STAGING_DB_PORT`
- `STAGING_DB_NAME`
- `STAGING_DB_USER`
- `STAGING_DB_PASSWORD`
- `STAGING_ADMIN_TOKEN`

### Staging URL
TBD (set after the first successful deployment).

## Module 02 Reflection (CI/CD)

This repository implements Continuous Integration by automatically running repeatable build steps (backend tests + coverage, frontend lint/build, and static analysis) on every pull request and on pushes to feature branches. These checks prevent broken code from being merged by catching compilation issues, failing tests, and code-quality regressions early. The deployment workflow implements Continuous Delivery by providing an automated, production-like deployment to a staging environment that is triggered only after changes are merged into `main`, using secrets for configuration and a Dockerfile for reproducible builds.

Quality issue fixed:
- Strategy: enable a static analysis tool (PMD), observe failures, then fix the reported violation and keep the workflow as a guardrail to prevent regressions.

## Commit Log

See `commits.md`.

