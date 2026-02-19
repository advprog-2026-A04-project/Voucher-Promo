package com.example.demo.voucher.repository;

import com.example.demo.voucher.domain.VoucherRedemption;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, Long> {

    Optional<VoucherRedemption> findByVoucherIdAndOrderId(Long voucherId, String orderId);
}

