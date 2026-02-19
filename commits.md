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
