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
