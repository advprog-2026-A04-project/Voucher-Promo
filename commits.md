# Commit History

This file records meaningful project commits (code/config changes). Commits that only update `commits.md` are not self-recorded to avoid infinite recursion.

- a035031 chore: bootstrap Spring Boot project
  - Initial Spring Boot + Gradle wrapper skeleton.

- 31d5ac3 chore: move Spring Boot app into backend subproject
  - Restructured repo to multi-project Gradle with a `backend/` subproject.

- dd57f71 feat: add voucher entities and Flyway migrations
  - Added `vouchers` and `voucher_redemptions` tables + JPA entities, and local MySQL `docker-compose.yml`.

- 56ab51c feat: add voucher validate, list, and admin create APIs
  - Implemented `GET /vouchers/active`, `POST /vouchers/validate`, and demo `POST /admin/vouchers` (guarded by `X-Admin-Token`).

- 7dc16bb feat: add idempotent voucher claim by orderId
  - Implemented `POST /vouchers/claim` with idempotency via `voucher_redemptions` unique constraint; added idempotency test.

- 34a24b3 feat: make voucher claim quota decrement atomic
  - Prevented quota leak under race conditions using an atomic conditional update (`quota_remaining > 0`).

- 13ac2c6 test: add DB connectivity and concurrency tests
  - Added DB connectivity smoke test and a parallel-claims test to ensure quota never goes below 0.

- c699e2c fix: prevent deadlocks in voucher claim
  - Uses a pessimistic row lock during claim to avoid MySQL deadlocks under concurrent requests.

- e5a9933 feat: add React + Tailwind connectivity test app
  - Added minimal frontend dashboard and forms for voucher create/validate/claim using a Vite dev proxy to the backend.

- ef22139 chore: enable JaCoCo report for backend tests
  - Added JaCoCo plugin + XML/HTML reports and ensured `jacocoTestReport` runs after tests.

- eaebb7e ci: add backend+frontend GitHub Actions workflow
  - Added `ci.yml` to run backend tests + frontend lint/build on PRs and feature-branch pushes.

- 582362c ci: add Scorecard and security workflows
  - Added OSSF Scorecard SARIF upload, Dependency Review, and CodeQL analysis workflows.

- 3aa5b5e ci: skip dependency review when graph disabled
  - Prevents `dependency-review-action` from failing when GitHub Dependency graph is disabled (auto-skips instead).

- 62fd19f ci: fix Gradle wrapper permissions
  - Marks `gradlew` executable and adds `chmod +x gradlew` steps so CI can run Gradle on Linux runners.

- e48ec15 ci: upload backend test reports on failure
  - Uploads JUnit XML and HTML test reports as artifacts even when backend CI fails, to make debugging faster.

- b5b2058 test: prevent stale Testcontainers DB across classes
  - Prevents Spring test context reuse across classes so the MySQL container port does not go stale between tests.

- 1d1e601 ci: add PMD analysis workflow
  - Added Gradle PMD config + `pmd.yml` (runs on every push; fails on priority 1-3 violations).

- a7a22a6 fix: resolve PMD generic exception catch
  - Fixed `AvoidCatchingGenericException` violation in `VoucherClaimConcurrencyTest`.

- c04cf76 security: enable CSRF protection
  - Enables cookie-based CSRF tokens (SPA-friendly) and adds a `/csrf` endpoint; frontend now sends CSRF header on POST requests.

- f024ea6 chore: add Dockerfile to bundle frontend into backend
  - Added multi-stage Docker build that compiles the Vite frontend and serves it from Spring Boot static resources.

- 092602a cd: deploy staging to Koyeb on main
  - Added GitHub Actions workflow to deploy staging on merges to `main` using Dockerfile builds on Koyeb.

- acbff25 docs: add project documentation
  - Added end-to-end setup docs, API examples, CI/CD explanation, and Module 02 reflection.

- 6613ec3 docs: update README for CSRF and claim concurrency
  - Updates curl examples to include CSRF tokens and documents the row-lock based claim strategy.
