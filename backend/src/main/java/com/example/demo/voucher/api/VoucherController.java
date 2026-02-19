package com.example.demo.voucher.api;

import com.example.demo.voucher.api.dto.ValidateVoucherRequest;
import com.example.demo.voucher.api.dto.ValidateVoucherResponse;
import com.example.demo.voucher.api.dto.VoucherPublicResponse;
import com.example.demo.voucher.service.VoucherService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping("/active")
    public List<VoucherPublicResponse> getActiveVouchers() {
        return voucherService.getActiveVouchers();
    }

    @PostMapping("/validate")
    public ValidateVoucherResponse validateVoucher(@Valid @RequestBody ValidateVoucherRequest request) {
        return voucherService.validateVoucher(request);
    }
}

