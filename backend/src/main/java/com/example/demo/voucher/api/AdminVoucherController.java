package com.example.demo.voucher.api;

import com.example.demo.voucher.api.dto.CreateVoucherRequest;
import com.example.demo.voucher.api.dto.CreateVoucherResponse;
import com.example.demo.voucher.api.dto.EditVoucherRequest;
import com.example.demo.voucher.domain.VoucherStatus;
import com.example.demo.voucher.service.VoucherService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping
    public List<CreateVoucherResponse> listVouchers(@RequestParam(value = "status", required = false) VoucherStatus status) {
        return voucherService.getAdminVouchers(status);
    }

    @PutMapping("/{id}")
    public CreateVoucherResponse editVoucher(@PathVariable("id") Long id, @Valid @RequestBody EditVoucherRequest request) {
        return voucherService.editVoucher(id, request);
    }

    @PostMapping("/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableVoucher(@PathVariable("id") Long id) {
        voucherService.disableVoucher(id);
    }
}

