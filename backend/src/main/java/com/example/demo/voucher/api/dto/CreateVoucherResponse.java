package com.example.demo.voucher.api.dto;

import com.example.demo.voucher.domain.DiscountType;
import com.example.demo.voucher.domain.VoucherStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateVoucherResponse(
        Long id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        LocalDateTime startAt,
        LocalDateTime endAt,
        BigDecimal minSpend,
        Integer quotaTotal,
        Integer quotaRemaining,
        VoucherStatus status
) {
}

