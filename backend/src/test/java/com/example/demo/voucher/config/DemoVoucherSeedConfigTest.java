package com.example.demo.voucher.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.voucher.domain.DiscountType;
import com.example.demo.voucher.domain.VoucherStatus;
import com.example.demo.voucher.repository.VoucherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = "app.demo-seed.enabled=true")
class DemoVoucherSeedConfigTest {

    @Autowired
    private VoucherRepository voucherRepository;

    @Test
    void demoSeedEnabledShouldCreateActiveMilestoneVoucher() {
        var voucher = voucherRepository.findByCode("MILESTONE10").orElseThrow();

        assertThat(voucher.getDiscountType()).isEqualTo(DiscountType.PERCENT);
        assertThat(voucher.getDiscountValue()).isEqualByComparingTo("10.00");
        assertThat(voucher.getQuotaRemaining()).isEqualTo(100);
        assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.ACTIVE);
    }
}
