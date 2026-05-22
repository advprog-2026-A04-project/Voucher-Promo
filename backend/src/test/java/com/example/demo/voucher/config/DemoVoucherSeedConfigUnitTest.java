package com.example.demo.voucher.config;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.voucher.domain.Voucher;
import com.example.demo.voucher.repository.VoucherRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DemoVoucherSeedConfigUnitTest {

    private final DemoVoucherSeedConfig config = new DemoVoucherSeedConfig();
    private final VoucherRepository voucherRepository = Mockito.mock(VoucherRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-22T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void seedDemoVoucherShouldSkipWhenDisabled() throws Exception {
        config.seedDemoVoucher(voucherRepository, clock, false).run();

        verify(voucherRepository, never()).findByCode("MILESTONE10");
        verify(voucherRepository, never()).save(Mockito.any(Voucher.class));
    }

    @Test
    void seedDemoVoucherShouldSkipWhenMilestoneVoucherExists() throws Exception {
        when(voucherRepository.findByCode("MILESTONE10")).thenReturn(Optional.of(new Voucher()));

        config.seedDemoVoucher(voucherRepository, clock, true).run();

        verify(voucherRepository, never()).save(Mockito.any(Voucher.class));
    }
}
