package com.example.demo.voucher.service;

import com.example.demo.voucher.api.dto.CreateVoucherRequest;
import com.example.demo.voucher.api.dto.CreateVoucherResponse;
import com.example.demo.voucher.api.dto.ClaimVoucherRequest;
import com.example.demo.voucher.api.dto.ClaimVoucherResponse;
import com.example.demo.voucher.api.dto.EditVoucherRequest;
import com.example.demo.voucher.api.dto.ValidateVoucherRequest;
import com.example.demo.voucher.api.dto.ValidateVoucherResponse;
import com.example.demo.voucher.api.dto.VoucherPublicResponse;
import com.example.demo.voucher.domain.DiscountType;
import com.example.demo.voucher.domain.Voucher;
import com.example.demo.voucher.domain.VoucherRedemption;
import com.example.demo.voucher.domain.VoucherStatus;
import com.example.demo.voucher.repository.VoucherRedemptionRepository;
import com.example.demo.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final Clock clock;

    public VoucherService(
            VoucherRepository voucherRepository,
            VoucherRedemptionRepository voucherRedemptionRepository,
            Clock clock
    ) {
        this.voucherRepository = voucherRepository;
        this.voucherRedemptionRepository = voucherRedemptionRepository;
        this.clock = clock;
    }

    public List<VoucherPublicResponse> getActiveVouchers() {
        LocalDateTime now = LocalDateTime.now(clock);
        expireVouchers(now);
        return voucherRepository
                .findByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualAndQuotaRemainingGreaterThan(
                        VoucherStatus.ACTIVE,
                        now,
                        now,
                        0
                )
                .stream()
                .map(v -> new VoucherPublicResponse(
                        v.getCode(),
                        v.getDiscountType(),
                        v.getDiscountValue(),
                        v.getMinSpend(),
                        v.getQuotaRemaining(),
                        v.getStartAt(),
                        v.getEndAt()
                ))
                .toList();
    }

    public List<CreateVoucherResponse> getAdminVouchers(VoucherStatus status) {
        LocalDateTime now = LocalDateTime.now(clock);
        expireVouchers(now);

        List<Voucher> vouchers = status == null
                ? voucherRepository.findAllByOrderByCreatedAtDesc()
                : voucherRepository.findByStatusOrderByCreatedAtDesc(status);

        return vouchers.stream()
                .map(VoucherService::toCreateVoucherResponse)
                .toList();
    }

    @Transactional
    public ClaimVoucherResponse claimVoucher(ClaimVoucherRequest request) {
        String code = normalizeCode(request.code());
        String orderId = request.orderId().trim();
        Long buyerId = request.buyerId();
        BigDecimal orderAmount = request.orderAmount();
        LocalDateTime now = LocalDateTime.now(clock);
        expireVouchers(now);

        Voucher voucher = voucherRepository.findByCodeForUpdate(code).orElse(null);
        if (voucher == null) {
            return new ClaimVoucherResponse(false, false, code, orderId, orderAmount, null, null, "voucher not found");
        }

        VoucherRedemption existing = voucherRedemptionRepository
                .findByVoucherIdAndOrderId(voucher.getId(), orderId)
                .orElse(null);
        if (existing != null) {
            return new ClaimVoucherResponse(
                    true,
                    true,
                    code,
                    orderId,
                    existing.getOrderAmount(),
                    existing.getDiscountApplied(),
                    voucher.getQuotaRemaining(),
                    "already claimed for this orderId"
            );
        }

        String error = validateVoucherUsability(voucher, orderAmount, now);
        if (error != null) {
            return new ClaimVoucherResponse(false, false, code, orderId, orderAmount, null, voucher.getQuotaRemaining(), error);
        }

        BigDecimal discount = calculateDiscount(orderAmount, voucher.getDiscountType(), voucher.getDiscountValue());

        try {
            voucherRedemptionRepository.save(VoucherRedemption.builder()
                    .voucher(voucher)
                    .orderId(orderId)
                    .buyerId(buyerId)
                    .orderAmount(orderAmount)
                    .discountApplied(discount)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            VoucherRedemption retryExisting = voucherRedemptionRepository
                    .findByVoucherIdAndOrderId(voucher.getId(), orderId)
                    .orElseThrow(() -> ex);
            return new ClaimVoucherResponse(
                    true,
                    true,
                    code,
                    orderId,
                    retryExisting.getOrderAmount(),
                    retryExisting.getDiscountApplied(),
                    voucher.getQuotaRemaining(),
                    "already claimed for this orderId"
            );
        }

        int remainingAfter = voucher.getQuotaRemaining() - 1;
        voucher.setQuotaRemaining(remainingAfter);

        return new ClaimVoucherResponse(true, false, code, orderId, orderAmount, discount, remainingAfter, "ok");
    }

    public ValidateVoucherResponse validateVoucher(ValidateVoucherRequest request) {
        String code = normalizeCode(request.code());
        BigDecimal orderAmount = request.orderAmount();
        LocalDateTime now = LocalDateTime.now(clock);
        expireVouchers(now);

        Voucher voucher = voucherRepository.findByCode(code).orElse(null);
        if (voucher == null) {
            return new ValidateVoucherResponse(false, code, orderAmount, null, "voucher not found");
        }

        String error = validateVoucherUsability(voucher, orderAmount, now);
        if (error != null) {
            return new ValidateVoucherResponse(false, code, orderAmount, null, error);
        }

        BigDecimal discount = calculateDiscount(orderAmount, voucher.getDiscountType(), voucher.getDiscountValue());
        return new ValidateVoucherResponse(true, code, orderAmount, discount, "ok");
    }

    @Transactional
    public CreateVoucherResponse createVoucher(CreateVoucherRequest request) {
        String code = normalizeCode(request.code());
        if (!request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }

        if (request.discountType() == DiscountType.PERCENT
                && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("percent discount must be <= 100");
        }

        Voucher voucher = Voucher.builder()
                .code(code)
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .minSpend(request.minSpend())
                .quotaTotal(request.quotaTotal())
                .quotaRemaining(request.quotaTotal())
                .status(VoucherStatus.ACTIVE)
                .build();

        Voucher saved;
        try {
            saved = voucherRepository.save(voucher);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("voucher code already exists");
        }

        return toCreateVoucherResponse(saved);
    }

    @Transactional
    public CreateVoucherResponse editVoucher(Long id, EditVoucherRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
        if (request.discountType() == DiscountType.PERCENT
                && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("percent discount must be <= 100");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        expireVouchers(now);

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("voucher not found"));

        if (voucher.getStatus() == VoucherStatus.EXPIRED || now.isAfter(voucher.getEndAt())) {
            throw new IllegalArgumentException("voucher expired");
        }

        int claimed = voucher.getQuotaTotal() - voucher.getQuotaRemaining();
        if (request.quotaTotal() < claimed) {
            throw new IllegalArgumentException("new quotaTotal cannot be less than already claimed quota (" + claimed + ")");
        }

        voucher.setDiscountType(request.discountType());
        voucher.setDiscountValue(request.discountValue());
        voucher.setStartAt(request.startAt());
        voucher.setEndAt(request.endAt());
        voucher.setMinSpend(request.minSpend());
        voucher.setQuotaTotal(request.quotaTotal());
        voucher.setQuotaRemaining(request.quotaTotal() - claimed);

        return toCreateVoucherResponse(voucher);
    }

    @Transactional
    public void disableVoucher(Long id) {
        LocalDateTime now = LocalDateTime.now(clock);
        expireVouchers(now);

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("voucher not found"));

        if (voucher.getStatus() == VoucherStatus.EXPIRED || now.isAfter(voucher.getEndAt())) {
            voucher.setStatus(VoucherStatus.EXPIRED);
            return;
        }

        voucher.setStatus(VoucherStatus.INACTIVE);
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void expireVouchers(LocalDateTime now) {
        voucherRepository.markExpiredVouchers(VoucherStatus.EXPIRED, now);
    }

    private static String validateVoucherUsability(Voucher voucher, BigDecimal orderAmount, LocalDateTime now) {
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

    private static BigDecimal calculateDiscount(BigDecimal orderAmount, DiscountType discountType, BigDecimal discountValue) {
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

    private static CreateVoucherResponse toCreateVoucherResponse(Voucher voucher) {
        return new CreateVoucherResponse(
                voucher.getId(),
                voucher.getCode(),
                voucher.getDiscountType(),
                voucher.getDiscountValue(),
                voucher.getStartAt(),
                voucher.getEndAt(),
                voucher.getMinSpend(),
                voucher.getQuotaTotal(),
                voucher.getQuotaRemaining(),
                voucher.getStatus()
        );
    }
}

