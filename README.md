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
- Git
- Java 17+ (backend targets Java 17 bytecode; local dev can use newer JDKs)
- Node.js 20+ (frontend)
- MySQL 8 (pick one):
  - Docker Desktop + Docker Compose (recommended; easiest)
  - Install MySQL locally (works, but you must create the DB/user yourself)

Optional:
- Docker Desktop (required only if you want `docker compose` and Testcontainers-backed tests)

### Install on a New Computer (Quickstart)
1. Clone this repo.
2. Start MySQL (Docker Compose recommended).
3. Run backend (Spring Boot).
4. Run frontend (Vite dev server).
5. Open `http://localhost:5173`.

### Start MySQL
Option A: Docker Compose (recommended)
```bash
docker compose up -d
```

Option B: Local MySQL (no Docker)
- Create DB + user (example):
```sql
CREATE DATABASE voucherpromo;
CREATE USER 'app'@'%' IDENTIFIED BY 'app';
GRANT ALL PRIVILEGES ON voucherpromo.* TO 'app'@'%';
FLUSH PRIVILEGES;
```
- Or, if you want to use your existing MySQL `root` user, set `DB_USER=root` and `DB_PASSWORD=...` when running the backend.

### Run Backend
```bash
./gradlew :backend:bootRun
```

Windows PowerShell:
```powershell
.\gradlew.bat :backend:bootRun
```

Backend defaults to:
- `DB_HOST=localhost`
- `DB_PORT=3306`
- `DB_NAME=voucherpromo`
- `DB_USER=app`
- `DB_PASSWORD=app`
- `PORT=8080`
- `APP_TIME_ZONE=` (optional; e.g., `Asia/Jakarta` to make voucher start/end windows consistent across environments)

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

### Verify It Works
- Backend health: `http://localhost:8080/health`
- Active vouchers (DB-backed): `http://localhost:8080/vouchers/active`
- Frontend UI: `http://localhost:5173`

### Troubleshooting
- `Port 8080 was already in use`: stop the other process or run with `PORT=8081` and update `frontend/vite.config.ts` proxy targets.
- `401 missing or invalid admin token` on `POST /admin/vouchers`: set `ADMIN_TOKEN` and send the same value in `X-Admin-Token`.
- `403 Forbidden` on POST requests with curl: you must fetch `/csrf` first and send `X-XSRF-TOKEN` (see CSRF section below).
- `voucher not in active period`: your start/end timestamps must include "now"; if your local/staging time zone differs, set `APP_TIME_ZONE=Asia/Jakarta` (or your preferred zone).

## Environment Variables

Backend reads DB configuration from environment variables:
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `DB_SSL_MODE` (optional; MySQL `sslMode`, default: `PREFERRED`)

Other:
- `ADMIN_TOKEN` (demo guard for `POST /admin/vouchers`, default: `dev-admin-token`)
- `PORT` (server port, default: `8080`)
- `APP_TIME_ZONE` (optional; time zone for voucher start/end window checks, e.g., `Asia/Jakarta`)

## API (MVP)

Full documentation: `docs/api.md`

### Base URL
- Local: `http://localhost:8080`
- Deployed: `http://<host>` (ask for the current staging/prod URL)

### Health
```bash
BASE_URL="http://localhost:8080"
curl "$BASE_URL/health"
curl "$BASE_URL/actuator/health"
```

### CSRF (Required for POST)
This backend enables cookie-based CSRF protection. The React frontend handles it automatically.

For non-browser clients, every `POST` must include:
- Cookie: `XSRF-TOKEN=<token>`
- Header: `X-XSRF-TOKEN: <token>`

Fetch the CSRF cookie + token once and reuse it:
```bash
BASE_URL="http://localhost:8080"

# Option A (recommended): parse the token from /csrf JSON with Python
CSRF=$(
  curl -s -c cookies.txt "$BASE_URL/csrf" \
  | python -c "import sys,json; print(json.load(sys.stdin)['token'])"
)

# Option B: parse the token from cookies.txt (bash + awk)
# curl -s -c cookies.txt "$BASE_URL/csrf" > /dev/null
# CSRF=$(awk '$6==\"XSRF-TOKEN\"{print $7}' cookies.txt)
```

Windows PowerShell example:
```powershell
$BaseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf = (Invoke-RestMethod "$BaseUrl/csrf" -WebSession $session).token
Invoke-RestMethod "$BaseUrl/vouchers/validate" -Method Post -WebSession $session `
  -Headers @{ "X-XSRF-TOKEN" = $csrf } -ContentType "application/json" `
  -Body '{ "code": "DEMO10", "orderAmount": 100.00 }'
```

### List Active Vouchers
```bash
curl "$BASE_URL/vouchers/active"
```

### Admin: Create Voucher (Demo)
Guarded by `X-Admin-Token` header (value comes from `ADMIN_TOKEN` env var).

```bash
curl -X POST "$BASE_URL/admin/vouchers" \
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
curl -X POST "$BASE_URL/vouchers/validate" \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -H "X-XSRF-TOKEN: $CSRF" \
  -d '{ "code": "DEMO10", "orderAmount": 100.00 }'
```

### Claim Voucher (Idempotent + Concurrency-Safe)
Idempotency key: `orderId`.

```bash
curl -X POST "$BASE_URL/vouchers/claim" \
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
- Triggers: `pull_request` and `push` to all branches
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

## Branching Model (Staging -> Production)

- `staging`: staging environment (default PR target for feature branches)
- `main`: production environment (only `staging` may be merged into `main`)

Branching policy check:
- Workflow: `.github/workflows/pr-policy.yml`
- Enforces: PRs into `main` must come from `staging`

Docs:
- `docs/repo-setup.md`

## CD: Deploy to AWS (ECR + App Runner)

Workflow: `.github/workflows/deploy-aws.yml`
- Triggers: `push` to `staging` (staging) and `main` (production), after PR merge
- Pushes image tags (staging): `:staging` and `:staging-${GITHUB_SHA}`
- Pushes image tags (prod): `:prod` and `:prod-${GITHUB_SHA}`
- GitHub Environments used: `aws-staging` and `aws-prod`

Docs:
- `docs/aws-apprunner.md`

## Module 02 Reflection (CI/CD)

This repository implements Continuous Integration by automatically running repeatable build steps (backend tests + coverage, frontend lint/build, and static analysis) on every pull request and on pushes to branches. These checks prevent broken code from being merged by catching compilation issues, failing tests, and code-quality regressions early. The deployment workflow implements Continuous Delivery by deploying automatically on merges to `staging` (staging environment) and `main` (production environment) using a Dockerfile-based build and AWS.

Quality issue fixed:
- Strategy: enable a static analysis tool (PMD), observe failures, then fix the reported violation and keep the workflow as a guardrail to prevent regressions.

## Commit Log

See `commits.md`.

## Screenshot
<img width="2464" height="1369" alt="Screenshot 2026-02-20 170805" src="https://github.com/user-attachments/assets/b284ca22-c00a-4452-b193-cfe795fe6ae9" />

##Link
http://18.232.174.224/

