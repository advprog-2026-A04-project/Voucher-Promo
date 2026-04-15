INSERT INTO vouchers (
    code,
    discount_type,
    discount_value,
    start_at,
    end_at,
    min_spend,
    quota_total,
    quota_remaining,
    status,
    version,
    created_at,
    updated_at
)
SELECT
    'MILESTONE10',
    'PERCENT',
    10.00,
    DATEADD('DAY', -1, CURRENT_TIMESTAMP()),
    DATEADD('DAY', 30, CURRENT_TIMESTAMP()),
    100000.00,
    50,
    50,
    'ACTIVE',
    0,
    CURRENT_TIMESTAMP(),
    CURRENT_TIMESTAMP()
WHERE NOT EXISTS (
    SELECT 1
    FROM vouchers
    WHERE code = 'MILESTONE10'
);
