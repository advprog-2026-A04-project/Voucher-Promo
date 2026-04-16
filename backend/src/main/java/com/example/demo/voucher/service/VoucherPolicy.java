package com.example.demo.voucher.service;

import com.example.demo.voucher.domain.DiscountType;
import com.example.demo.voucher.domain.Voucher;
import com.example.demo.voucher.domain.VoucherStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class VoucherPolicy {

    public String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    public void validateVoucherDefinition(LocalDateTime startAt, LocalDateTime endAt, DiscountType discountType, BigDecimal discountValue) {
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }

        if (discountType == DiscountType.PERCENT && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("percent discount must be <= 100");
        }
    }

    public void ensureVoucherEditable(Voucher voucher, LocalDateTime now, int requestedQuotaTotal) {
        if (voucher.getStatus() == VoucherStatus.EXPIRED || now.isAfter(voucher.getEndAt())) {
            throw new IllegalArgumentException("voucher expired");
        }

        int claimed = voucher.getQuotaTotal() - voucher.getQuotaRemaining();
        if (requestedQuotaTotal < claimed) {
            throw new IllegalArgumentException("new quotaTotal cannot be less than already claimed quota (" + claimed + ")");
        }
    }

    public String validateVoucherUsability(Voucher voucher, BigDecimal orderAmount, LocalDateTime now) {
        if (voucher.getStatus() == VoucherStatus.EXPIRED) {
            return "voucher expired";
        }
        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            return "voucher inactive";
        }
        if (now.isBefore(voucher.getStartAt()) || now.isAfter(voucher.getEndAt())) {
            return "voucher not in active period";
        }
        if (voucher.getQuotaRemaining() <= 0) {
            return "voucher quota exhausted";
        }
        if (voucher.getMinSpend() != null && orderAmount.compareTo(voucher.getMinSpend()) < 0) {
            return "minimum spend not met";
        }
        return null;
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount, DiscountType discountType, BigDecimal discountValue) {
        BigDecimal discount;
        if (discountType == DiscountType.PERCENT) {
            discount = orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = discountValue;
        }
        if (discount.compareTo(orderAmount) > 0) {
            return orderAmount.setScale(2, RoundingMode.HALF_UP);
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }
}
