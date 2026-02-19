package com.example.demo.voucher.api.dto;

import com.example.demo.voucher.domain.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherPublicResponse(
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minSpend,
        Integer quotaRemaining,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}

