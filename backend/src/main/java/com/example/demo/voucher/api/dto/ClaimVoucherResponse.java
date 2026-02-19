package com.example.demo.voucher.api.dto;

import java.math.BigDecimal;

public record ClaimVoucherResponse(
        boolean success,
        boolean idempotent,
        String code,
        String orderId,
        BigDecimal orderAmount,
        BigDecimal discountApplied,
        Integer quotaRemaining,
        String message
) {
}

