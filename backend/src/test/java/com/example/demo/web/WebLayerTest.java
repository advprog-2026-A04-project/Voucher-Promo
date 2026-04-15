package com.example.demo.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.config.SecurityConfig;
import com.example.demo.security.CsrfController;
import com.example.demo.security.InternalTokenFilter;
import com.example.demo.voucher.api.AdminVoucherController;
import com.example.demo.voucher.api.VoucherController;
import com.example.demo.voucher.api.dto.ClaimVoucherResponse;
import com.example.demo.voucher.api.dto.CreateVoucherResponse;
import com.example.demo.voucher.api.dto.ValidateVoucherResponse;
import com.example.demo.voucher.api.dto.VoucherPublicResponse;
import com.example.demo.voucher.domain.DiscountType;
import com.example.demo.voucher.domain.VoucherStatus;
import com.example.demo.voucher.service.VoucherService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
        VoucherController.class,
        AdminVoucherController.class,
        CsrfController.class
})
@Import({SecurityConfig.class, InternalTokenFilter.class, ApiExceptionHandler.class})
@TestPropertySource(properties = {
        "app.admin-token=test-admin-token",
        "app.internal-token=test-internal-token",
        "app.cors.allowed-origins=http://localhost:5173"
})
class WebLayerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VoucherService voucherService;

    @Test
    void postAdminVoucher_withoutAdminToken_returns401() throws Exception {
        mockMvc.perform(post("/admin/vouchers")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("missing or invalid admin token"));
    }

    @Test
    void postAdminVoucher_withAdminToken_validationError_returns400() throws Exception {
        mockMvc.perform(post("/admin/vouchers")
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("validation failed"))
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.discountType").exists());
    }

    @Test
    void postAdminVoucher_withAdminToken_illegalArgument_returns400() throws Exception {
        when(voucherService.createVoucher(any())).thenThrow(new IllegalArgumentException("boom"));
        mockMvc.perform(post("/admin/vouchers")
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "DEMO10",
                                  "discountType": "FIXED",
                                  "discountValue": 10.00,
                                  "startAt": "2026-02-19T00:00:00",
                                  "endAt": "2026-03-01T00:00:00",
                                  "minSpend": null,
                                  "quotaTotal": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("boom"));
    }

    @Test
    void postAdminVoucher_success_returns201() throws Exception {
        when(voucherService.createVoucher(any())).thenReturn(new CreateVoucherResponse(
                1L,
                "DEMO10",
                DiscountType.FIXED,
                new BigDecimal("10.00"),
                LocalDateTime.parse("2026-02-19T00:00:00"),
                LocalDateTime.parse("2026-03-01T00:00:00"),
                null,
                5,
                5,
                VoucherStatus.ACTIVE
        ));
        mockMvc.perform(post("/admin/vouchers")
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "DEMO10",
                                  "discountType": "FIXED",
                                  "discountValue": 10.00,
                                  "startAt": "2026-02-19T00:00:00",
                                  "endAt": "2026-03-01T00:00:00",
                                  "minSpend": null,
                                  "quotaTotal": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DEMO10"))
                .andExpect(jsonPath("$.quotaRemaining").value(5));
    }

    @Test
    void getAdminVouchers_withoutAdminToken_returns401() throws Exception {
        mockMvc.perform(get("/admin/vouchers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("missing or invalid admin token"));
    }

    @Test
    void getAdminVouchers_withAdminToken_returns200() throws Exception {
        when(voucherService.getAdminVouchers(any())).thenReturn(List.of(
                new CreateVoucherResponse(
                        1L,
                        "DEMO10",
                        DiscountType.FIXED,
                        new BigDecimal("10.00"),
                        LocalDateTime.parse("2026-02-19T00:00:00"),
                        LocalDateTime.parse("2026-03-01T00:00:00"),
                        null,
                        5,
                        5,
                        VoucherStatus.ACTIVE
                )
        ));

        mockMvc.perform(get("/admin/vouchers")
                        .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("DEMO10"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void putAdminVoucher_success_returns200() throws Exception {
        when(voucherService.editVoucher(any(), any())).thenReturn(new CreateVoucherResponse(
                1L,
                "DEMO10",
                DiscountType.FIXED,
                new BigDecimal("20.00"),
                LocalDateTime.parse("2026-02-19T00:00:00"),
                LocalDateTime.parse("2026-03-01T00:00:00"),
                null,
                10,
                10,
                VoucherStatus.ACTIVE
        ));
        mockMvc.perform(put("/admin/vouchers/1")
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "discountType": "FIXED",
                                  "discountValue": 20.00,
                                  "startAt": "2026-02-19T00:00:00",
                                  "endAt": "2026-03-01T00:00:00",
                                  "minSpend": null,
                                  "quotaTotal": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountValue").value(20.00))
                .andExpect(jsonPath("$.quotaTotal").value(10));
    }

    @Test
    void putAdminVoucher_withoutAdminToken_returns401() throws Exception {
        mockMvc.perform(put("/admin/vouchers/1")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disableAdminVoucher_success_returns204() throws Exception {
        mockMvc.perform(post("/admin/vouchers/1/disable")
                        .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getActiveVouchers_delegatesToService() throws Exception {
        when(voucherService.getActiveVouchers()).thenReturn(List.of(
                new VoucherPublicResponse(
                        "DEMO10",
                        DiscountType.FIXED,
                        new BigDecimal("10.00"),
                        null,
                        5,
                        LocalDateTime.parse("2026-02-19T00:00:00"),
                        LocalDateTime.parse("2026-03-01T00:00:00")
                )
        ));

        mockMvc.perform(get("/vouchers/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("DEMO10"));
    }

    @Test
    void postValidate_delegatesToService() throws Exception {
        when(voucherService.validateVoucher(any())).thenReturn(new ValidateVoucherResponse(
                true,
                "DEMO10",
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                "ok"
        ));
        mockMvc.perform(post("/vouchers/validate")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"DEMO10\",\"orderAmount\":100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.discountAmount").value(10.00));
    }

    @Test
    void postValidate_acceptsSubtotalAlias() throws Exception {
        when(voucherService.validateVoucher(any())).thenReturn(new ValidateVoucherResponse(
                true,
                "DEMO10",
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                "ok"
        ));
        mockMvc.perform(post("/vouchers/validate")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"DEMO10\",\"subtotal\":100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.discountAmount").value(10.00));
    }

    @Test
    void postClaim_whenServiceThrowsIllegalArgument_returnsApiResponse() throws Exception {
        when(voucherService.claimVoucher(any())).thenThrow(new IllegalArgumentException("bad request"));
        mockMvc.perform(post("/vouchers/claim")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\" demo10 \",\"orderId\":\" ORDER-1 \",\"orderAmount\":100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DEMO10"))
                .andExpect(jsonPath("$.orderId").value("ORDER-1"))
                .andExpect(jsonPath("$.message").value("bad request"));
    }

    @Test
    void postClaim_success_delegatesToService() throws Exception {
        when(voucherService.claimVoucher(any())).thenReturn(new ClaimVoucherResponse(
                true,
                false,
                "DEMO10",
                "ORDER-1",
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                4,
                "ok"
        ));
        mockMvc.perform(post("/vouchers/claim")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"DEMO10\",\"orderId\":\"ORDER-1\",\"orderAmount\":100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.quotaRemaining").value(4));
    }

    @Test
    void postClaim_acceptsSubtotalAlias() throws Exception {
        when(voucherService.claimVoucher(any())).thenReturn(new ClaimVoucherResponse(
                true,
                false,
                "DEMO10",
                "ORDER-1",
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                4,
                "ok"
        ));
        mockMvc.perform(post("/vouchers/claim")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"DEMO10\",\"orderId\":\"ORDER-1\",\"subtotal\":100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.quotaRemaining").value(4));
    }
}
