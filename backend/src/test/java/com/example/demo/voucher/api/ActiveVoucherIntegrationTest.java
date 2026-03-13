package com.example.demo.voucher.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.voucher.domain.DiscountType;
import com.example.demo.voucher.domain.Voucher;
import com.example.demo.voucher.domain.VoucherStatus;
import com.example.demo.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("cloudrun")
class ActiveVoucherIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VoucherRepository voucherRepository;

    @BeforeEach
    void setUp() {
        voucherRepository.deleteAll();
    }

    @Test
    void getActiveVouchers_marksExpiredVouchersAndReturnsOnlyActiveOnes() throws Exception {
        voucherRepository.save(Voucher.builder()
                .code("OLD5")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("5.00"))
                .startAt(LocalDateTime.parse("1999-01-01T00:00:00"))
                .endAt(LocalDateTime.parse("2000-01-01T00:00:00"))
                .minSpend(null)
                .quotaTotal(1)
                .quotaRemaining(1)
                .status(VoucherStatus.ACTIVE)
                .build());

        voucherRepository.save(Voucher.builder()
                .code("ACTIVE10")
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("10.00"))
                .startAt(LocalDateTime.parse("2000-01-01T00:00:00"))
                .endAt(LocalDateTime.parse("2999-01-01T00:00:00"))
                .minSpend(new BigDecimal("100.00"))
                .quotaTotal(2)
                .quotaRemaining(2)
                .status(VoucherStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/vouchers/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("ACTIVE10"));

        Voucher expiredVoucher = voucherRepository.findByCode("OLD5").orElseThrow();
        assertThat(expiredVoucher.getStatus()).isEqualTo(VoucherStatus.EXPIRED);
    }
}
