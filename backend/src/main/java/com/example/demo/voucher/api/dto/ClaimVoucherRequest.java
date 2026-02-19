package com.example.demo.voucher.api.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClaimVoucherRequest(
        @NotBlank
        @Size(max = 64)
        String code,

        @NotBlank
        @Size(max = 64)
        String orderId,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal orderAmount
) {
}

