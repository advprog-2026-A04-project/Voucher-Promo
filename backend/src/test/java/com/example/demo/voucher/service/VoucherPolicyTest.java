package com.example.demo.voucher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.voucher.domain.DiscountType;
import com.example.demo.voucher.domain.Voucher;
import com.example.demo.voucher.domain.VoucherStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class VoucherPolicyTest {

    private final VoucherPolicy voucherPolicy = new VoucherPolicy();

    @Test
    void normalizeCodeShouldTrimAndUppercase() {
        assertThat(voucherPolicy.normalizeCode(" demo10 ")).isEqualTo("DEMO10");
    }

    @Test
    void validateVoucherDefinitionShouldRejectInvalidWindowsAndPercentages() {
        assertThatThrownBy(() -> voucherPolicy.validateVoucherDefinition(
                LocalDateTime.parse("2026-02-19T00:00:00"),
                LocalDateTime.parse("2026-02-19T00:00:00"),
                DiscountType.FIXED,
                new BigDecimal("10.00")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endAt must be after startAt");

        assertThatThrownBy(() -> voucherPolicy.validateVoucherDefinition(
                LocalDateTime.parse("2026-02-19T00:00:00"),
                LocalDateTime.parse("2026-02-20T00:00:00"),
                DiscountType.PERCENT,
                new BigDecimal("150.00")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percent discount must be <= 100");
    }

    @Test
    void validateVoucherUsabilityShouldRejectAllMilestoneFailureCases() {
        LocalDateTime now = LocalDateTime.parse("2026-02-19T00:00:00");

        Voucher expired = voucher(now.minusDays(1), now.plusDays(1), 5, null, VoucherStatus.EXPIRED);
        Voucher inactive = voucher(now.minusDays(1), now.plusDays(1), 5, null, VoucherStatus.INACTIVE);
        Voucher outsideWindow = voucher(now.plusDays(1), now.plusDays(2), 5, null, VoucherStatus.ACTIVE);
        Voucher quotaExhausted = voucher(now.minusDays(1), now.plusDays(1), 0, null, VoucherStatus.ACTIVE);
        Voucher minSpend = voucher(now.minusDays(1), now.plusDays(1), 5, new BigDecimal("200.00"), VoucherStatus.ACTIVE);

        assertThat(voucherPolicy.validateVoucherUsability(expired, new BigDecimal("100.00"), now)).isEqualTo("voucher expired");
        assertThat(voucherPolicy.validateVoucherUsability(inactive, new BigDecimal("100.00"), now)).isEqualTo("voucher inactive");
        assertThat(voucherPolicy.validateVoucherUsability(outsideWindow, new BigDecimal("100.00"), now)).isEqualTo("voucher not in active period");
        assertThat(voucherPolicy.validateVoucherUsability(quotaExhausted, new BigDecimal("100.00"), now)).isEqualTo("voucher quota exhausted");
        assertThat(voucherPolicy.validateVoucherUsability(minSpend, new BigDecimal("100.00"), now)).isEqualTo("minimum spend not met");
    }

    @Test
    void validateVoucherUsabilityShouldAllowEligibleVoucher() {
        LocalDateTime now = LocalDateTime.parse("2026-02-19T00:00:00");
        Voucher voucher = voucher(now.minusDays(1), now.plusDays(1), 5, new BigDecimal("50.00"), VoucherStatus.ACTIVE);

        assertThat(voucherPolicy.validateVoucherUsability(voucher, new BigDecimal("100.00"), now)).isNull();
    }

    @Test
    void ensureVoucherEditableShouldRejectExpiredOrOverclaimedUpdates() {
        LocalDateTime now = LocalDateTime.parse("2026-02-19T00:00:00");
        Voucher expired = voucher(now.minusDays(2), now.minusDays(1), 5, null, VoucherStatus.ACTIVE);
        expired.setQuotaTotal(10);
        expired.setQuotaRemaining(5);

        Voucher claimed = voucher(now.minusDays(1), now.plusDays(1), 5, null, VoucherStatus.ACTIVE);
        claimed.setQuotaTotal(10);
        claimed.setQuotaRemaining(4);

        assertThatThrownBy(() -> voucherPolicy.ensureVoucherEditable(expired, now, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("voucher expired");

        assertThatThrownBy(() -> voucherPolicy.ensureVoucherEditable(claimed, now, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("new quotaTotal cannot be less than already claimed quota");
    }

    @Test
    void calculateDiscountShouldHandlePercentAndClampFixedAmounts() {
        assertThat(voucherPolicy.calculateDiscount(new BigDecimal("100.00"), DiscountType.PERCENT, new BigDecimal("10.00")))
                .isEqualByComparingTo("10.00");
        assertThat(voucherPolicy.calculateDiscount(new BigDecimal("5.00"), DiscountType.FIXED, new BigDecimal("10.00")))
                .isEqualByComparingTo("5.00");
    }

    private static Voucher voucher(
            LocalDateTime startAt,
            LocalDateTime endAt,
            int quotaRemaining,
            BigDecimal minSpend,
            VoucherStatus status
    ) {
        return Voucher.builder()
                .code("DEMO10")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("10.00"))
                .startAt(startAt)
                .endAt(endAt)
                .minSpend(minSpend)
                .quotaTotal(10)
                .quotaRemaining(quotaRemaining)
                .status(status)
                .version(0L)
                .build();
    }
}
