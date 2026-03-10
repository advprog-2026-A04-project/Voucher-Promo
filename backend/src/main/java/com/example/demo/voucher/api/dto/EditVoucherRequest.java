package com.example.demo.voucher.api.dto;

import com.example.demo.voucher.domain.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EditVoucherRequest(
        @NotNull(message = "discountType cannot be null")
        DiscountType discountType,

        @NotNull(message = "discountValue cannot be null")
        @DecimalMin(value = "0.01", message = "discountValue must be > 0")
        BigDecimal discountValue,

        @NotNull(message = "startAt cannot be null")
        LocalDateTime startAt,

        @NotNull(message = "endAt cannot be null")
        LocalDateTime endAt,

        @DecimalMin(value = "0.0", message = "minSpend must be >= 0")
        BigDecimal minSpend,

        @NotNull(message = "quotaTotal cannot be null")
        @Min(value = 1, message = "quotaTotal must be >= 1")
        Integer quotaTotal
) {
}
