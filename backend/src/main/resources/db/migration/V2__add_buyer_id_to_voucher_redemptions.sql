ALTER TABLE voucher_redemptions
    ADD COLUMN buyer_id BIGINT NULL;

CREATE INDEX idx_redemptions_buyer_id ON voucher_redemptions (buyer_id);

