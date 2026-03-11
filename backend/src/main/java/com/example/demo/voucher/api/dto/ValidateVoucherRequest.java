package com.example.demo.voucher.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ValidateVoucherRequest(
        @NotBlank
        @Size(max = 64)
        String code,

        @NotNull
        @DecimalMin("0.00")
        @JsonAlias("subtotal")
        BigDecimal orderAmount,

        Long buyerId
) {
}

