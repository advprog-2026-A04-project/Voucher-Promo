package com.example.demo.voucher.repository;

import com.example.demo.voucher.domain.Voucher;
import com.example.demo.voucher.domain.VoucherStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCode(String code);

    List<Voucher> findByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualAndQuotaRemainingGreaterThan(
            VoucherStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Integer quotaRemaining
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Voucher v
            SET v.quotaRemaining = v.quotaRemaining - 1
            WHERE v.id = :voucherId
              AND v.status = :status
              AND v.startAt <= :now
              AND v.endAt >= :now
              AND v.quotaRemaining > 0
            """)
    int decrementQuotaIfClaimable(
            @Param("voucherId") Long voucherId,
            @Param("status") VoucherStatus status,
            @Param("now") LocalDateTime now
    );
}

