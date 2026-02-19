CREATE TABLE vouchers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    discount_type VARCHAR(16) NOT NULL,
    discount_value DECIMAL(19, 2) NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    min_spend DECIMAL(19, 2) NULL,
    quota_total INT NOT NULL,
    quota_remaining INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_vouchers PRIMARY KEY (id),
    CONSTRAINT uk_vouchers_code UNIQUE (code),
    CONSTRAINT chk_vouchers_quota_remaining CHECK (quota_remaining >= 0)
) ENGINE = InnoDB;

CREATE TABLE voucher_redemptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    voucher_id BIGINT NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_amount DECIMAL(19, 2) NOT NULL,
    discount_applied DECIMAL(19, 2) NOT NULL,
    claimed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_voucher_redemptions PRIMARY KEY (id),
    CONSTRAINT uk_redemptions_voucher_order UNIQUE (voucher_id, order_id),
    CONSTRAINT fk_redemptions_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB;

CREATE INDEX idx_redemptions_order_id ON voucher_redemptions (order_id);

