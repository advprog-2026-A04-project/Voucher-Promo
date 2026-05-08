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

The backend now defaults to the verifier-compatible demo mode:

- port `8085` unless `PORT` is overridden
- `cloudrun` profile unless `SPRING_PROFILES_ACTIVE` is set explicitly
- in-memory H2 with seeded `MILESTONE10`
- default admin token `local-demo-admin-token` unless `ADMIN_TOKEN` is overridden

That means a plain local boot is enough for the live Selenium suite:

```powershell
.\gradlew.bat :backend:bootRun
```

If you want to start it explicitly in the same demo mode, use:

```powershell
$env:PORT=8085
$env:SPRING_PROFILES_ACTIVE='cloudrun'
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

If you intentionally want the old stateful MySQL-backed mode for manual debugging, set a different explicit profile and datasource configuration instead of relying on the default local boot path.

## Test

```bash
./gradlew :backend:test
```

Includes:
- web-layer coverage for public, admin, and internal voucher endpoints
- service tests covering voucher claim logic

Local verifier expectation after a plain boot:

- `GET /vouchers/active` returns `200`
- `POST /admin/vouchers` returns `201` with `X-Admin-Token: local-demo-admin-token`
- the seeded `MILESTONE10` voucher is available for checkout flows

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
