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

Admin endpoint used for demo setup:
- `POST /admin/vouchers`

`/vouchers/validate` and `/vouchers/claim` require `X-Internal-Token`. `/admin/vouchers` requires `X-Admin-Token`.

## Local Run

Prerequisites:
- Java `17+`

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

Cloud Run uses the `cloudrun` profile from `backend/src/main/resources/application-cloudrun.properties`, which swaps the service to H2 for demo deployment.

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

## Demo Voucher Seed

Example:

```bash
curl -X POST https://voucher-promo-api-383620816191.us-central1.run.app/admin/vouchers \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: <admin-token>" \
  -d '{
    "code": "MILESTONE10",
    "discountType": "PERCENT",
    "discountValue": 10,
    "startAt": "2026-04-14T12:11:53",
    "endAt": "2026-05-15T12:11:53",
    "minSpend": 100000,
    "quotaTotal": 20
  }'
```

## Notes

- Voucher quota is decremented only after successful Order checkout.
- Scope is intentionally limited to the milestone checkout path.
