package com.example.demo.voucher.repository;

import com.example.demo.voucher.domain.Voucher;
import com.example.demo.voucher.domain.VoucherStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCode(String code);

    List<Voucher> findByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualAndQuotaRemainingGreaterThan(
            VoucherStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Integer quotaRemaining
    );
}

