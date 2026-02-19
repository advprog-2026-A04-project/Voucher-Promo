package com.example.demo.voucher.api.dto;

import com.example.demo.voucher.domain.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVoucherRequest(
        @NotBlank
        @Size(max = 64)
        String code,

        @NotNull
        DiscountType discountType,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal discountValue,

        @NotNull
        LocalDateTime startAt,

        @NotNull
        LocalDateTime endAt,

        @DecimalMin("0.00")
        BigDecimal minSpend,

        @NotNull
        @Min(1)
        Integer quotaTotal
) {
}

