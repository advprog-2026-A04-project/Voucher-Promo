package com.example.demo.voucher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.voucher.api.dto.ClaimVoucherRequest;
import com.example.demo.voucher.api.dto.ClaimVoucherResponse;
import com.example.demo.voucher.api.dto.CreateVoucherRequest;
import com.example.demo.voucher.api.dto.CreateVoucherResponse;
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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class VoucherServiceUnitTest {

    private final VoucherRepository voucherRepository = mock(VoucherRepository.class);
    private final VoucherRedemptionRepository voucherRedemptionRepository = mock(VoucherRedemptionRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-02-19T00:00:00Z"), ZoneId.of("UTC"));

    private VoucherService voucherService;

    @BeforeEach
    void setUp() {
        voucherService = new VoucherService(voucherRepository, voucherRedemptionRepository, clock);
    }

    @Test
    void getActiveVouchers_mapsEntitiesToPublicResponse() {
        Voucher voucher = Voucher.builder()
                .id(1L)
                .code("DEMO10")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("10.00"))
                .startAt(LocalDateTime.parse("2026-02-18T00:00:00"))
                .endAt(LocalDateTime.parse("2026-02-20T00:00:00"))
                .minSpend(null)
                .quotaTotal(5)
                .quotaRemaining(5)
                .status(VoucherStatus.ACTIVE)
                .version(0L)
                .build();

        when(voucherRepository.findByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualAndQuotaRemainingGreaterThan(
                any(), any(), any(), any()
        )).thenReturn(List.of(voucher));

        List<VoucherPublicResponse> resp = voucherService.getActiveVouchers();
        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).code()).isEqualTo("DEMO10");
        assertThat(resp.get(0).quotaRemaining()).isEqualTo(5);
    }

    @Test
    void validateVoucher_whenNotFound_returnsInvalid() {
        when(voucherRepository.findByCode("MISSING")).thenReturn(Optional.empty());

        ValidateVoucherResponse resp = voucherService.validateVoucher(new ValidateVoucherRequest("missing", new BigDecimal("100.00")));

        assertThat(resp.valid()).isFalse();
        assertThat(resp.code()).isEqualTo("MISSING");
        assertThat(resp.message()).isEqualTo("voucher not found");
    }

    @Test
    void validateVoucher_whenUsable_percentDiscount_isComputed() {
        Voucher voucher = Voucher.builder()
                .id(1L)
                .code("PERC10")
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("10.00"))
                .startAt(LocalDateTime.parse("2026-02-18T00:00:00"))
                .endAt(LocalDateTime.parse("2026-02-20T00:00:00"))
                .minSpend(null)
                .quotaTotal(5)
                .quotaRemaining(5)
                .status(VoucherStatus.ACTIVE)
                .version(0L)
                .build();
        when(voucherRepository.findByCode("PERC10")).thenReturn(Optional.of(voucher));

        ValidateVoucherResponse resp = voucherService.validateVoucher(new ValidateVoucherRequest("perc10", new BigDecimal("100.00")));

        assertThat(resp.valid()).isTrue();
        assertThat(resp.discountAmount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(resp.message()).isEqualTo("ok");
    }

    @Test
    void validateVoucher_whenFixedDiscountExceedsOrderAmount_isClamped() {
        Voucher voucher = Voucher.builder()
                .id(1L)
                .code("BIG")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("999.00"))
                .startAt(LocalDateTime.parse("2026-02-18T00:00:00"))
                .endAt(LocalDateTime.parse("2026-02-20T00:00:00"))
                .minSpend(null)
                .quotaTotal(5)
                .quotaRemaining(5)
                .status(VoucherStatus.ACTIVE)
                .version(0L)
                .build();
        when(voucherRepository.findByCode("BIG")).thenReturn(Optional.of(voucher));

        ValidateVoucherResponse resp = voucherService.validateVoucher(new ValidateVoucherRequest("big", new BigDecimal("5.00")));

        assertThat(resp.valid()).isTrue();
        assertThat(resp.discountAmount()).isEqualTo(new BigDecimal("5.00"));
    }

    @Test
    void validateVoucher_whenInactive_returnsInvalid() {
        Voucher voucher = Voucher.builder()
                .id(1L)
                .code("INACT")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("1.00"))
                .startAt(LocalDateTime.parse("2026-02-18T00:00:00"))
                .endAt(LocalDateTime.parse("2026-02-20T00:00:00"))
                .minSpend(null)
                .quotaTotal(5)
                .quotaRemaining(5)
                .status(VoucherStatus.INACTIVE)
                .version(0L)
                .build();
        when(voucherRepository.findByCode("INACT")).thenReturn(Optional.of(voucher));

        ValidateVoucherResponse resp = voucherService.validateVoucher(new ValidateVoucherRequest("inact", new BigDecimal("100.00")));

        assertThat(resp.valid()).isFalse();
        assertThat(resp.message()).isEqualTo("voucher inactive");
    }

    @Test
    void validateVoucher_whenOutsideWindow_returnsInvalid() {
        Voucher voucher = Voucher.builder()
                .id(1L)
                .code("WIN")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("1.00"))
                .startAt(LocalDateTime.parse("2026-02-20T00:00:00"))
                .endAt(LocalDateTime.parse("2026-02-21T00:00:00"))
                .minSpend(null)
                .quotaTotal(5)
                .quotaRemaining(5)
                .status(VoucherStatus.ACTIVE)
                .version(0L)
                .build();
        when(voucherRepository.findByCode("WIN")).thenReturn(Optional.of(voucher));

        ValidateVoucherResponse resp = voucherService.validateVoucher(new ValidateVoucherRequest("win", new BigDecimal("100.00")));

        assertThat(resp.valid()).isFalse();
        assertThat(resp.message()).isEqualTo("voucher not in active period");
    }

    @Test
    void validateVoucher_whenQuotaExhausted_returnsInvalid() {
        Voucher voucher = Voucher.builder()
                .id(1L)
                .code("Q0")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("1.00"))
                .startAt(LocalDateTime.parse("2026-02-18T00:00:00"))
                .endAt(LocalDateTime.parse("2026-02-20T00:00:00"))
                .minSpend(null)
                .quotaTotal(5)
                .quotaRemaining(0)
                .status(VoucherStatus.ACTIVE)
                .version(0L)
                .build();
        when(voucherRepository.findByCode("Q0")).thenReturn(Optional.of(voucher));

        ValidateVoucherResponse resp = voucherService.validateVoucher(new ValidateVoucherRequest("q0", new BigDecimal("100.00")));

        assertThat(resp.valid()).isFalse();
        assertThat(resp.message()).isEqualTo("voucher quota exhausted");
    }

    @Test
    void validateVoucher_whenMinSpendNotMet_returnsInvalid() {
        Voucher voucher = Voucher.builder()
                .id(1L)
                .code("MIN")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("1.00"))
                .startAt(LocalDateTime.parse("2026-02-18T00:00:00"))
                .endAt(LocalDateTime.parse("2026-02-20T00:00:00"))
                .minSpend(new BigDecimal("200.00"))
                .quotaTotal(5)
                .quotaRemaining(5)
                .status(VoucherStatus.ACTIVE)
                .version(0L)
                .build();
        when(voucherRepository.findByCode("MIN")).thenReturn(Optional.of(voucher));

        ValidateVoucherResponse resp = voucherService.validateVoucher(new ValidateVoucherRequest("min", new BigDecimal("100.00")));

        assertThat(resp.valid()).isFalse();
        assertThat(resp.message()).isEqualTo("minimum spend not met");
    }

    @Test
    void claimVoucher_whenVoucherNotFound_returnsFailure() {
        when(voucherRepository.findByCodeForUpdate("MISSING")).thenReturn(Optional.empty());

        ClaimVoucherResponse resp = voucherService.claimVoucher(
                new ClaimVoucherRequest("missing", "ORDER-1", new BigDecimal("100.00"))
        );

        assertThat(resp.success()).isFalse();
        assertThat(resp.message()).isEqualTo("voucher not found");
    }

    @Test
    void claimVoucher_whenAlreadyRedeemed_returnsIdempotentSuccess() {
        Voucher voucher = Voucher.builder()
                .id(10L)
                .code("IDEMP")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("10.00"))
                .startAt(LocalDateTime.parse("2026-02-18T00:00:00"))
                .endAt(LocalDateTime.parse("2026-02-20T00:00:00"))
                .minSpend(null)
                .quotaTotal(5)
                .quotaRemaining(4)
                .status(VoucherStatus.ACTIVE)
                .version(0L)
                .build();

        VoucherRedemption redemption = VoucherRedemption.builder()
                .id(1L)
                .voucher(voucher)
                .orderId("ORDER-1")
                .orderAmount(new BigDecimal("100.00"))
                .discountApplied(new BigDecimal("10.00"))
                .build();

        when(voucherRepository.findByCodeForUpdate("IDEMP")).thenReturn(Optional.of(voucher));
        when(voucherRedemptionRepository.findByVoucherIdAndOrderId(10L, "ORDER-1")).thenReturn(Optional.of(redemption));

        ClaimVoucherResponse resp = voucherService.claimVoucher(
                new ClaimVoucherRequest("idemp", "ORDER-1", new BigDecimal("100.00"))
        );

        assertThat(resp.success()).isTrue();
        assertThat(resp.idempotent()).isTrue();
        assertThat(resp.message()).isEqualTo("already claimed for this orderId");
        assertThat(resp.quotaRemaining()).isEqualTo(4);
        assertThat(resp.discountApplied()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void claimVoucher_whenUsable_decrementsQuotaAndReturnsOk() {
        Voucher voucher = Voucher.builder()
                .id(20L)
                .code("OK")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("10.00"))
                .startAt(LocalDateTime.parse("2026-02-18T00:00:00"))
                .endAt(LocalDateTime.parse("2026-02-20T00:00:00"))
                .minSpend(null)
                .quotaTotal(5)
                .quotaRemaining(2)
                .status(VoucherStatus.ACTIVE)
                .version(0L)
                .build();

        when(voucherRepository.findByCodeForUpdate("OK")).thenReturn(Optional.of(voucher));
        when(voucherRedemptionRepository.findByVoucherIdAndOrderId(20L, "ORDER-1")).thenReturn(Optional.empty());
        when(voucherRedemptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClaimVoucherResponse resp = voucherService.claimVoucher(
                new ClaimVoucherRequest("ok", "ORDER-1", new BigDecimal("100.00"))
        );

        assertThat(resp.success()).isTrue();
        assertThat(resp.idempotent()).isFalse();
        assertThat(resp.message()).isEqualTo("ok");
        assertThat(resp.quotaRemaining()).isEqualTo(1);
        assertThat(voucher.getQuotaRemaining()).isEqualTo(1);
    }

    @Test
    void claimVoucher_whenRedemptionInsertRaces_returnsIdempotentSuccess() {
        Voucher voucher = Voucher.builder()
                .id(30L)
                .code("RACE")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("10.00"))
                .startAt(LocalDateTime.parse("2026-02-18T00:00:00"))
                .endAt(LocalDateTime.parse("2026-02-20T00:00:00"))
                .minSpend(null)
                .quotaTotal(5)
                .quotaRemaining(2)
                .status(VoucherStatus.ACTIVE)
                .version(0L)
                .build();

        VoucherRedemption redemption = VoucherRedemption.builder()
                .id(1L)
                .voucher(voucher)
                .orderId("ORDER-1")
                .orderAmount(new BigDecimal("100.00"))
                .discountApplied(new BigDecimal("10.00"))
                .build();

        when(voucherRepository.findByCodeForUpdate("RACE")).thenReturn(Optional.of(voucher));
        when(voucherRedemptionRepository.findByVoucherIdAndOrderId(30L, "ORDER-1")).thenReturn(Optional.empty(), Optional.of(redemption));
        when(voucherRedemptionRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        ClaimVoucherResponse resp = voucherService.claimVoucher(
                new ClaimVoucherRequest("race", "ORDER-1", new BigDecimal("100.00"))
        );

        assertThat(resp.success()).isTrue();
        assertThat(resp.idempotent()).isTrue();
        assertThat(resp.message()).isEqualTo("already claimed for this orderId");
    }

    @Test
    void claimVoucher_whenNotUsable_returnsFailureWithReason() {
        Voucher voucher = Voucher.builder()
                .id(40L)
                .code("NOQUOTA")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("10.00"))
                .startAt(LocalDateTime.parse("2026-02-18T00:00:00"))
                .endAt(LocalDateTime.parse("2026-02-20T00:00:00"))
                .minSpend(null)
                .quotaTotal(1)
                .quotaRemaining(0)
                .status(VoucherStatus.ACTIVE)
                .version(0L)
                .build();

        when(voucherRepository.findByCodeForUpdate("NOQUOTA")).thenReturn(Optional.of(voucher));
        when(voucherRedemptionRepository.findByVoucherIdAndOrderId(40L, "ORDER-1")).thenReturn(Optional.empty());

        ClaimVoucherResponse resp = voucherService.claimVoucher(
                new ClaimVoucherRequest("noquota", "ORDER-1", new BigDecimal("100.00"))
        );

        assertThat(resp.success()).isFalse();
        assertThat(resp.message()).isEqualTo("voucher quota exhausted");
        assertThat(resp.quotaRemaining()).isEqualTo(0);
    }

    @Test
    void createVoucher_whenEndAtNotAfterStartAt_throws() {
        CreateVoucherRequest req = new CreateVoucherRequest(
                "demo10",
                DiscountType.FIXED,
                new BigDecimal("10.00"),
                LocalDateTime.parse("2026-02-19T00:00:00"),
                LocalDateTime.parse("2026-02-19T00:00:00"),
                null,
                5
        );

        assertThatThrownBy(() -> voucherService.createVoucher(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endAt must be after startAt");
    }

    @Test
    void createVoucher_whenPercentAbove100_throws() {
        CreateVoucherRequest req = new CreateVoucherRequest(
                "demo10",
                DiscountType.PERCENT,
                new BigDecimal("101"),
                LocalDateTime.parse("2026-02-19T00:00:00"),
                LocalDateTime.parse("2026-02-20T00:00:00"),
                null,
                5
        );

        assertThatThrownBy(() -> voucherService.createVoucher(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("percent discount must be <= 100");
    }

    @Test
    void createVoucher_whenDuplicateCode_throwsFriendlyMessage() {
        when(voucherRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        CreateVoucherRequest req = new CreateVoucherRequest(
                "demo10",
                DiscountType.FIXED,
                new BigDecimal("10.00"),
                LocalDateTime.parse("2026-02-19T00:00:00"),
                LocalDateTime.parse("2026-02-20T00:00:00"),
                null,
                5
        );

        assertThatThrownBy(() -> voucherService.createVoucher(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voucher code already exists");
    }

    @Test
    void createVoucher_success_normalizesCode() {
        Voucher saved = Voucher.builder()
                .id(99L)
                .code("DEMO10")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("10.00"))
                .startAt(LocalDateTime.parse("2026-02-19T00:00:00"))
                .endAt(LocalDateTime.parse("2026-02-20T00:00:00"))
                .minSpend(null)
                .quotaTotal(5)
                .quotaRemaining(5)
                .status(VoucherStatus.ACTIVE)
                .version(0L)
                .build();
        when(voucherRepository.save(any())).thenReturn(saved);

        CreateVoucherRequest req = new CreateVoucherRequest(
                " demo10 ",
                DiscountType.FIXED,
                new BigDecimal("10.00"),
                LocalDateTime.parse("2026-02-19T00:00:00"),
                LocalDateTime.parse("2026-02-20T00:00:00"),
                null,
                5
        );

        CreateVoucherResponse resp = voucherService.createVoucher(req);
        assertThat(resp.code()).isEqualTo("DEMO10");
        assertThat(resp.id()).isEqualTo(99L);
    }
}
