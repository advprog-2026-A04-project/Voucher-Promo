package com.example.demo.voucher;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.it.MySqlTestcontainersBase;
import com.example.demo.voucher.api.dto.ClaimVoucherRequest;
import com.example.demo.voucher.api.dto.ClaimVoucherResponse;
import com.example.demo.voucher.domain.DiscountType;
import com.example.demo.voucher.domain.Voucher;
import com.example.demo.voucher.domain.VoucherStatus;
import com.example.demo.voucher.repository.VoucherRedemptionRepository;
import com.example.demo.voucher.repository.VoucherRepository;
import com.example.demo.voucher.service.VoucherService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VoucherClaimConcurrencyTest extends MySqlTestcontainersBase {

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherRedemptionRepository voucherRedemptionRepository;

    @Autowired
    private Clock clock;

    @BeforeEach
    void cleanup() {
        voucherRedemptionRepository.deleteAll();
        voucherRepository.deleteAll();
    }

    @Test
    void quotaOne_parallelClaims_onlyOneSucceeds() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));

        Voucher voucher = Voucher.builder()
                .code("CONC1")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("5.00"))
                .startAt(now.minusDays(1))
                .endAt(now.plusDays(1))
                .minSpend(null)
                .quotaTotal(1)
                .quotaRemaining(1)
                .status(VoucherStatus.ACTIVE)
                .build();
        voucherRepository.save(voucher);

        int workers = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < workers; i++) {
            int idx = i;
            futures.add(executor.submit(() -> {
                start.await();
                try {
                    ClaimVoucherResponse resp = voucherService.claimVoucher(
                            new ClaimVoucherRequest("CONC1", "ORDER-" + idx, new BigDecimal("100.00"))
                    );
                    return resp.success() && !resp.idempotent();
                } catch (Exception ex) {
                    return false;
                }
            }));
        }

        start.countDown();

        int successCount = 0;
        for (Future<Boolean> f : futures) {
            if (Boolean.TRUE.equals(f.get())) {
                successCount++;
            }
        }

        executor.shutdownNow();

        assertThat(successCount).isEqualTo(1);

        Voucher reloaded = voucherRepository.findByCode("CONC1").orElseThrow();
        assertThat(reloaded.getQuotaRemaining()).isEqualTo(0);
        assertThat(voucherRedemptionRepository.count()).isEqualTo(1);
    }
}

