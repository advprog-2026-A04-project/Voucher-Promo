# Voucher-Promo Service

Voucher service for Milestone `25%` and `50%`.

## Deployed URL

- `https://voucher-promo-api-383620816191.us-central1.run.app`

## Implemented Scope

Public endpoint:
- `GET /vouchers/active`

Internal checkout endpoints:
- `POST /vouchers/validate`
- `POST /vouchers/claim`

Admin endpoint available for manual voucher setup:
- `POST /admin/vouchers`

`/vouchers/validate` and `/vouchers/claim` require `X-Internal-Token`. `/admin/vouchers` requires `X-Admin-Token`.

## Local Run

Prerequisites:
- Java `17+`

The backend runtime now expects a MySQL-compatible database. For local work,
start MySQL with the schema migrations available under `backend/src/main/resources/db/migration`,
then provide database credentials:

```powershell
$env:PORT=8085
$env:DB_URL='jdbc:mysql://localhost:3306/voucherpromo?allowPublicKeyRetrieval=true&sslMode=PREFERRED&serverTimezone=UTC'
$env:DB_USERNAME='app'
$env:DB_PASSWORD='app'
$env:ADMIN_TOKEN='local-demo-admin-token'
.\gradlew.bat :backend:bootRun
```

Run backend:

```bash
./gradlew :backend:bootRun
```

PowerShell:

```powershell
.\gradlew.bat :backend:bootRun
```

Default local URL:
- `http://localhost:8085`

## Environment Variables

Backend:
- `PORT`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `APP_CORS_ALLOWED_ORIGINS`
- `INTERNAL_API_TOKEN`
- `ADMIN_TOKEN`
- `APP_TIME_ZONE`

Cloud Run uses the `cloudsql` profile and must receive Cloud SQL/MySQL
credentials through environment variables or Secret Manager. H2 is not used for
runtime deployments.

## Test

```bash
./gradlew :backend:test
```

Includes:
- web-layer coverage for public, admin, and internal voucher endpoints
- service tests covering voucher claim logic

Local verifier expectation after booting with a valid MySQL database:

- `GET /vouchers/active` returns `200`
- `POST /admin/vouchers` returns `201` with `X-Admin-Token: local-demo-admin-token`
- seed or create the required checkout voucher through `POST /admin/vouchers`

## Cloud Run Deploy

```bash
gcloud run deploy voucher-promo-api --source . --region us-central1 --allow-unauthenticated --max-instances=1 \
  --set-env-vars APP_CORS_ALLOWED_ORIGINS=https://advprog-frontend-m25-m50-383620816191.us-central1.run.app \
  --set-env-vars SPRING_PROFILES_ACTIVE=cloudsql \
  --set-env-vars DB_URL=<mysql-or-cloud-sql-jdbc-url> \
  --set-env-vars DB_USERNAME=<db-user> \
  --set-env-vars DB_PASSWORD=<db-password> \
  --set-env-vars INTERNAL_API_TOKEN=<shared-internal-token> \
  --set-env-vars ADMIN_TOKEN=<admin-token>
```

## Demo Voucher

Create `MILESTONE10` through the admin API for demo flows with:
- `10%` discount
- `minSpend=100000`
- active start/end window relative to startup
- initial quota `50`

Do not rely on H2 startup seed data in staging or production.

## Notes

- Voucher quota is decremented only after successful Order checkout.
- Scope is intentionally limited to the milestone checkout path.
