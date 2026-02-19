package com.example.demo.voucher.api;

import com.example.demo.voucher.api.dto.CreateVoucherRequest;
import com.example.demo.voucher.api.dto.CreateVoucherResponse;
import com.example.demo.voucher.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/vouchers")
public class AdminVoucherController {

    private final VoucherService voucherService;

    public AdminVoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateVoucherResponse createVoucher(@Valid @RequestBody CreateVoucherRequest request) {
        return voucherService.createVoucher(request);
    }
}

