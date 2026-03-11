# Voucher-Promo API (MVP)

## Base URL

- Local: `http://localhost:8080`
- Deployed: `http://<host>` (ask for the current staging/prod URL)

All endpoints use JSON.

## CSRF (Required For `POST`/`PUT`)

This backend uses cookie-based CSRF protection.

For every `POST`/`PUT` request:

1. Call `GET /csrf` once to receive:
   - Cookie: `XSRF-TOKEN=<token>`
   - JSON body: `{ "token": "<token>", "headerName": "X-XSRF-TOKEN", ... }`
2. Send both on every `POST`:
   - Cookie: `XSRF-TOKEN=<token>`
   - Header: `X-XSRF-TOKEN: <token>`

### CSRF Example (curl)

```bash
BASE_URL="http://localhost:8080"

# Fetch CSRF cookie + token
CSRF=$(
  curl -s -c cookies.txt "$BASE_URL/csrf" \
  | python -c "import sys,json; print(json.load(sys.stdin)['token'])"
)

# Use it on POST
curl -i -b cookies.txt -c cookies.txt \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF" \
  -X POST "$BASE_URL/vouchers/validate" \
  -d '{\"code\":\"DEMO10\",\"orderAmount\":100.00}'
```

## Admin Auth (Only For `/admin/*`)

Admin endpoints require:

- Header: `X-Admin-Token: <ADMIN_TOKEN>`

`ADMIN_TOKEN` is configured via backend env var `ADMIN_TOKEN` (default: `dev-admin-token`).

## Endpoints

### Health

- `GET /health` -> `{ "status": "UP", "db": "UP" }` (checks app + DB connectivity)
- `GET /actuator/health` (Spring Boot actuator health)

### List Active Vouchers

`GET /vouchers/active`

Returns only vouchers that:
- `status == ACTIVE`
- `startAt <= now <= endAt`
- `quotaRemaining > 0`

Response (array):
```json
[
  {
    "code": "SPRING10",
    "discountType": "PERCENT",
    "discountValue": 10.00,
    "minSpend": 100.00,
    "quotaRemaining": 2,
    "startAt": "2026-03-06T00:00:00",
    "endAt": "2026-04-06T00:00:00"
  }
]
```

### Validate Voucher

`POST /vouchers/validate`

Request:
```json
{ "code": "SPRING10", "orderAmount": 150.00, "buyerId": 123 }
```

Notes:
- `buyerId` is optional (used for cross-module integration/audit).
- `subtotal` is accepted as an alias for `orderAmount` (useful when integrating with an Order module).

Response (valid):
```json
{ "valid": true, "code": "SPRING10", "orderAmount": 150.00, "discountAmount": 15.00, "message": "ok" }
```

Response (invalid):
```json
{ "valid": false, "code": "SPRING10", "orderAmount": 50.00, "discountAmount": null, "message": "minimum spend not met" }
```

### Claim Voucher (Idempotent)

`POST /vouchers/claim`

Request:
```json
{ "code": "SPRING10", "orderId": "ORDER-123", "orderAmount": 150.00, "buyerId": 123 }
```

Notes:
- Idempotency key: `orderId`.
- `buyerId` is optional (used for cross-module integration/audit).
- `subtotal` is accepted as an alias for `orderAmount`.

Response (success):
```json
{ "success": true, "idempotent": false, "code": "SPRING10", "orderId": "ORDER-123", "orderAmount": 150.00, "discountApplied": 15.00, "quotaRemaining": 1, "message": "ok" }
```

Response (idempotent retry: same `orderId`):
```json
{ "success": true, "idempotent": true, "code": "SPRING10", "orderId": "ORDER-123", "orderAmount": 150.00, "discountApplied": 15.00, "quotaRemaining": 1, "message": "already claimed for this orderId" }
```

### Admin: Create Voucher

`POST /admin/vouchers` (requires `X-Admin-Token` + CSRF)

Request:
```json
{
  "code": "SPRING10",
  "discountType": "PERCENT",
  "discountValue": 10.00,
  "startAt": "2026-03-06T00:00:00",
  "endAt": "2026-04-06T00:00:00",
  "minSpend": 100.00,
  "quotaTotal": 100
}
```

Notes:
- `discountType`: `PERCENT` or `FIXED`
- If `PERCENT`, `discountValue` must be `<= 100`
- `endAt` must be after `startAt`
- `minSpend` may be `null`

### Admin: List Vouchers

`GET /admin/vouchers` (requires `X-Admin-Token`)

Optional query params:
- `status`: `ACTIVE` | `INACTIVE` | `EXPIRED`

Response (array):
```json
[
  {
    "id": 1,
    "code": "SPRING10",
    "discountType": "PERCENT",
    "discountValue": 10.00,
    "startAt": "2026-03-06T00:00:00",
    "endAt": "2026-04-06T00:00:00",
    "minSpend": 100.00,
    "quotaTotal": 100,
    "quotaRemaining": 42,
    "status": "ACTIVE"
  }
]
```

### Admin: Edit Voucher

`PUT /admin/vouchers/{id}` (requires `X-Admin-Token` + CSRF)

Request:
```json
{
  "discountType": "PERCENT",
  "discountValue": 10.00,
  "startAt": "2026-03-06T00:00:00",
  "endAt": "2026-04-06T00:00:00",
  "minSpend": 100.00,
  "quotaTotal": 100
}
```

Notes:
- Editing an expired voucher returns `400` (`voucher expired`).
- `quotaTotal` cannot be set below the already-claimed quota.
- `quotaRemaining` is recomputed as `quotaTotal - claimed`.

### Admin: Disable Voucher

`POST /admin/vouchers/{id}/disable` (requires `X-Admin-Token` + CSRF)

Response: `204 No Content`

## Behavior Notes

- Voucher codes are normalized: `trim + uppercase` (case-insensitive input).
- Voucher statuses: `ACTIVE`, `INACTIVE`, `EXPIRED`.
- Expired vouchers are automatically marked as `EXPIRED` (and won't show up in `/vouchers/active`).
- Discounts are rounded to 2 decimals (HALF_UP).
- `FIXED` discount is capped at `orderAmount` (cannot exceed order total).
- Claim is concurrency-safe and idempotent by `(voucher, orderId)`.

## Error Responses

- `400 Bad Request` (validation):
```json
{ "timestamp": "...", "message": "validation failed", "errors": { "code": "must not be blank" } }
```

- `400 Bad Request` (illegal argument):
```json
{ "timestamp": "...", "message": "voucher code already exists" }
```

- `401 Unauthorized` (admin token missing/invalid):
```json
{ "message": "missing or invalid admin token" }
```

- `403 Forbidden` typically means CSRF missing/invalid for `POST`.

