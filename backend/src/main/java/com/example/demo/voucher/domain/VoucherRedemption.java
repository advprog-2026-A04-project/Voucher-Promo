package com.example.demo.voucher.domain;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "voucher_redemptions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_redemptions_voucher_order",
                        columnNames = {"voucher_id", "order_id"}
                )
        },
        indexes = {
                @Index(name = "idx_redemptions_order_id", columnList = "order_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal orderAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountApplied;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant claimedAt;
}

