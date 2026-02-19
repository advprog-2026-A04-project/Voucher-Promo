package com.example.demo.voucher.api.dto;

import java.math.BigDecimal;

public record ValidateVoucherResponse(
        boolean valid,
        String code,
        BigDecimal orderAmount,
        BigDecimal discountAmount,
        String message
) {
}

