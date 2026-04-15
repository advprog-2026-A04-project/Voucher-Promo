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

For a demo-ready local run without MySQL, use the same `cloudrun` profile as deployment:

```powershell
$env:PORT=8085
$env:SPRING_PROFILES_ACTIVE='cloudrun'
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
- `http://localhost:8080`

## Environment Variables

Backend:
- `PORT`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `DB_SSL_MODE`
- `APP_CORS_ALLOWED_ORIGINS`
- `INTERNAL_API_TOKEN`
- `ADMIN_TOKEN`
- `APP_TIME_ZONE`

Cloud Run uses the `cloudrun` profile from `backend/src/main/resources/application-cloudrun.properties`, which swaps the service to H2 for demo deployment and seeds `MILESTONE10` automatically if it does not exist.

## Test

```bash
./gradlew :backend:test
```

Includes:
- web-layer coverage for public, admin, and internal voucher endpoints
- service tests covering voucher claim logic

## Cloud Run Deploy

```bash
gcloud run deploy voucher-promo-api --source . --region us-central1 --allow-unauthenticated --max-instances=1 \
  --set-env-vars APP_CORS_ALLOWED_ORIGINS=https://advprog-frontend-m25-m50-383620816191.us-central1.run.app \
  --set-env-vars INTERNAL_API_TOKEN=<shared-internal-token> \
  --set-env-vars ADMIN_TOKEN=<admin-token>
```

## Demo Voucher

`MILESTONE10` is seeded automatically in the `cloudrun` profile with:
- `10%` discount
- `minSpend=100000`
- active start/end window relative to startup
- initial quota `50`

Use the admin API only when you want additional vouchers beyond the default demo code.

## Notes

- Voucher quota is decremented only after successful Order checkout.
- Scope is intentionally limited to the milestone checkout path.
