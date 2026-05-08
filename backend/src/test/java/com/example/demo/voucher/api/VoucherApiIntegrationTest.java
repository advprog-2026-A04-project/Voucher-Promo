package com.example.demo.voucher.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.voucher.domain.DiscountType;
import com.example.demo.voucher.domain.Voucher;
import com.example.demo.voucher.domain.VoucherStatus;
import com.example.demo.voucher.repository.VoucherRedemptionRepository;
import com.example.demo.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "app.admin-token=test-admin-token",
        "app.cors.allowed-origins=http://localhost:5173"
})
class VoucherApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherRedemptionRepository voucherRedemptionRepository;

    @BeforeEach
    void setUp() {
        voucherRedemptionRepository.deleteAll();
        voucherRepository.deleteAll();
    }

    @Test
    void getActiveVouchers_whenNoneExist_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/vouchers/active"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getActiveVouchers_whenActiveVoucherExists_returnsVoucherData() throws Exception {
        voucherRepository.save(activeVoucher("MILESTONE10"));

        mockMvc.perform(get("/vouchers/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("MILESTONE10"))
                .andExpect(jsonPath("$[0].discountType").value("PERCENT"))
                .andExpect(jsonPath("$[0].quotaRemaining").value(5));
    }

    @Test
    void postAdminVoucher_whenPayloadValid_createsVoucher() throws Exception {
        mockMvc.perform(post("/admin/vouchers")
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "TEST10",
                                  "discountType": "PERCENT",
                                  "discountValue": 10.00,
                                  "startAt": "2026-05-08T00:00:00",
                                  "endAt": "2026-06-08T00:00:00",
                                  "minSpend": 0.00,
                                  "quotaTotal": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TEST10"))
                .andExpect(jsonPath("$.quotaRemaining").value(5));
    }

    @Test
    void postAdminVoucher_whenPayloadInvalid_returns400Not500() throws Exception {
        mockMvc.perform(post("/admin/vouchers")
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "",
                                  "discountType": "PERCENT",
                                  "discountValue": 10.00,
                                  "startAt": "2026-06-08T00:00:00",
                                  "endAt": "2026-05-08T00:00:00",
                                  "minSpend": 0.00,
                                  "quotaTotal": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("validation failed"))
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.quotaTotal").exists());
    }

    private static Voucher activeVoucher(String code) {
        return Voucher.builder()
                .code(code)
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("10.00"))
                .startAt(LocalDateTime.parse("2026-05-01T00:00:00"))
                .endAt(LocalDateTime.parse("2026-06-01T00:00:00"))
                .minSpend(new BigDecimal("100000.00"))
                .quotaTotal(5)
                .quotaRemaining(5)
                .status(VoucherStatus.ACTIVE)
                .version(0L)
                .build();
    }
}
