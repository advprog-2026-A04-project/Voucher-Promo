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

## Design Patterns

### Repository Pattern

Persistence access is isolated behind Spring Data repository interfaces. The
service layer depends on these abstractions instead of issuing database queries
directly.

Source: [`VoucherRepository.java`](backend/src/main/java/com/example/demo/voucher/repository/VoucherRepository.java)

```java
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.code = :code")
    Optional<Voucher> findByCodeForUpdate(@Param("code") String code);
}
```

This pattern keeps persistence concerns separate from business rules and makes
the service testable with mocked repositories.

### Domain Policy Pattern

Voucher eligibility and discount rules are centralized in a dedicated policy
component instead of being duplicated across API handlers.

Source: [`VoucherPolicy.java`](backend/src/main/java/com/example/demo/voucher/service/VoucherPolicy.java)

```java
@Component
public class VoucherPolicy {
    public String validateVoucherUsability(
            Voucher voucher,
            BigDecimal orderAmount,
            LocalDateTime now
    ) {
        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            return "voucher inactive";
        }
        if (voucher.getQuotaRemaining() <= 0) {
            return "voucher quota exhausted";
        }
        return null;
    }
}
```

The complete policy also handles active windows, minimum spend, definition
validation, editable state, code normalization, and discount calculation.

### Service Layer Pattern

REST controllers remain thin. Transaction boundaries and checkout-oriented
voucher workflows live in the service layer.

Sources:
[`VoucherController.java`](backend/src/main/java/com/example/demo/voucher/api/VoucherController.java),
[`VoucherService.java`](backend/src/main/java/com/example/demo/voucher/service/VoucherService.java)

```java
@PostMapping("/claim")
public ClaimVoucherResponse claimVoucher(
        @Valid @RequestBody ClaimVoucherRequest request
) {
    return voucherService.claimVoucher(request);
}

@Transactional
public ClaimVoucherResponse claimVoucher(ClaimVoucherRequest request) {
    Voucher voucher = voucherRepository.findByCodeForUpdate(code).orElse(null);
    // Apply idempotency, policy checks, and quota mutation.
}
```

### Builder Pattern

Voucher domain objects use Lombok's Builder Pattern to keep object construction
readable in service code and tests.

Source: [`Voucher.java`](backend/src/main/java/com/example/demo/voucher/domain/Voucher.java)

```java
@Builder
public class Voucher {
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer quotaRemaining;
}
```

## TDD-Oriented Development

The module is developed with a TDD-oriented workflow: behavior is protected by
unit, integration, concurrency, and regression tests. The repository history
contains tests added alongside features and tests added after defects were
found. This is intentionally described as **TDD-oriented**, rather than
claiming that every historical commit followed a strict red-green-refactor
cycle.

Example idempotency regression test:
[`VoucherClaimIdempotencyTest.java`](backend/src/test/java/com/example/demo/voucher/VoucherClaimIdempotencyTest.java)

```java
ClaimVoucherResponse first = voucherService.claimVoucher(req);
ClaimVoucherResponse second = voucherService.claimVoucher(req);

assertThat(first.idempotent()).isFalse();
assertThat(second.idempotent()).isTrue();
assertThat(reloaded.getQuotaRemaining()).isEqualTo(1);
```

Additional evidence:
- [`VoucherPolicyTest.java`](backend/src/test/java/com/example/demo/voucher/service/VoucherPolicyTest.java)
  covers domain policy behavior.
- [`VoucherClaimConcurrencyTest.java`](backend/src/test/java/com/example/demo/voucher/VoucherClaimConcurrencyTest.java)
  submits parallel claims and verifies that quota cannot become negative.
- [`VoucherApiIntegrationTest.java`](backend/src/test/java/com/example/demo/voucher/api/VoucherApiIntegrationTest.java)
  verifies public and admin HTTP contracts.
- [`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs tests and JaCoCo
  verification on pushes and pull requests.

The current JaCoCo gate requires `100%` line coverage and at least `90%` branch
coverage. Fresh local verification on May 31, 2026 passed `83` tests with
`100%` line coverage and `94.87%` branch coverage.

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
