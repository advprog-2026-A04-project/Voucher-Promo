package com.example.demo.voucher.config;

import com.example.demo.voucher.domain.DiscountType;
import com.example.demo.voucher.domain.Voucher;
import com.example.demo.voucher.domain.VoucherStatus;
import com.example.demo.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoVoucherSeedConfig {

    private static final Logger log = LoggerFactory.getLogger(DemoVoucherSeedConfig.class);
    private static final String DEMO_CODE = "MILESTONE10";

    @Bean
    public CommandLineRunner seedDemoVoucher(
            VoucherRepository voucherRepository,
            Clock clock,
            @Value("${app.demo-seed.enabled:false}") boolean enabled
    ) {
        return args -> {
            if (!enabled) {
                return;
            }
            if (voucherRepository.findByCode(DEMO_CODE).isPresent()) {
                return;
            }

            LocalDateTime now = LocalDateTime.now(clock);
            voucherRepository.save(Voucher.builder()
                    .code(DEMO_CODE)
                    .discountType(DiscountType.PERCENT)
                    .discountValue(new BigDecimal("10.00"))
                    .startAt(now.minusDays(1))
                    .endAt(now.plusDays(30))
                    .minSpend(BigDecimal.ZERO)
                    .quotaTotal(100)
                    .quotaRemaining(100)
                    .status(VoucherStatus.ACTIVE)
                    .build());
            log.warn("Demo voucher seeding is enabled for this environment.");
        };
    }
}
