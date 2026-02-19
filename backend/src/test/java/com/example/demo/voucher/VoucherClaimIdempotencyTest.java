package com.example.demo.voucher;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.voucher.api.dto.ClaimVoucherRequest;
import com.example.demo.voucher.api.dto.ClaimVoucherResponse;
import com.example.demo.voucher.domain.DiscountType;
import com.example.demo.voucher.domain.Voucher;
import com.example.demo.voucher.domain.VoucherStatus;
import com.example.demo.voucher.repository.VoucherRepository;
import com.example.demo.voucher.service.VoucherService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VoucherClaimIdempotencyTest {

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private Clock clock;

    @Test
    void claimSameOrderIdTwice_decrementsQuotaOnce() {
        LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));

        Voucher voucher = Voucher.builder()
                .code("IDEMPOTENT10")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("10.00"))
                .startAt(now.minusDays(1))
                .endAt(now.plusDays(1))
                .minSpend(null)
                .quotaTotal(2)
                .quotaRemaining(2)
                .status(VoucherStatus.ACTIVE)
                .build();
        voucherRepository.save(voucher);

        ClaimVoucherRequest req = new ClaimVoucherRequest("IDEMPOTENT10", "ORDER-123", new BigDecimal("100.00"));

        ClaimVoucherResponse first = voucherService.claimVoucher(req);
        ClaimVoucherResponse second = voucherService.claimVoucher(req);

        assertThat(first.success()).isTrue();
        assertThat(first.idempotent()).isFalse();

        assertThat(second.success()).isTrue();
        assertThat(second.idempotent()).isTrue();

        Voucher reloaded = voucherRepository.findByCode("IDEMPOTENT10").orElseThrow();
        assertThat(reloaded.getQuotaRemaining()).isEqualTo(1);
    }
}

