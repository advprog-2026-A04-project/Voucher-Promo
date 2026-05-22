package com.example.demo.voucher.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record VoucherRedemptionAuditResponse(
        Long id,
        Long voucherId,
        String code,
        String orderId,
        Long buyerId,
        BigDecimal orderAmount,
        BigDecimal discountApplied,
        Instant claimedAt
) {
}
