CREATE TABLE rfq_detail (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rfq_header_id VARCHAR(255) NOT NULL,
    option_name VARCHAR(255) NULL,
    plan VARCHAR(255) NULL,
    spec TEXT NOT NULL,
    sort_order INT NULL,
    remark TEXT NULL,
    internal_remark TEXT NULL,
    commission DECIMAL(18,4) NULL,
    created_date DATETIME(6) NULL,
    updated_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rfq_detail_header
        FOREIGN KEY (rfq_header_id) REFERENCES rfq_header (id)
);

CREATE TABLE rfq_tier (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rfq_detail_id BIGINT NOT NULL,
    quantity DECIMAL(18,0) NOT NULL,
    product_price DECIMAL(18,4) NOT NULL,
    target_price DECIMAL(18,4) NULL,
    commission DECIMAL(18,4) NULL,
    land_freight_cost DECIMAL(18,4) NULL,
    sea_freight_cost DECIMAL(18,4) NULL,
    land_total_price DECIMAL(18,4) NULL,
    sea_total_price DECIMAL(18,4) NULL,
    sort_order INT NULL,
    created_date DATETIME(6) NULL,
    updated_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rfq_tier_detail
        FOREIGN KEY (rfq_detail_id) REFERENCES rfq_detail (id),
    CONSTRAINT uk_rfq_tier_detail_qty
        UNIQUE (rfq_detail_id, quantity)
);

CREATE TABLE rfq_tier_split (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rfq_detail_id BIGINT NOT NULL,
    supplier_id VARCHAR(255) NULL,
    quantity DECIMAL(18,0) NOT NULL,
    sell_price DECIMAL(18,4) NOT NULL,
    commission DECIMAL(18,4) NULL,
    currency VARCHAR(10) NULL,
    land_freight_cost DECIMAL(18,4) NULL,
    land_freight_qty DECIMAL(18,4) NULL,
    sea_freight_qty DECIMAL(18,4) NULL,
    sea_freight_cost DECIMAL(18,4) NULL,
    is_fcl BIT(1) NULL,
    is_share_fcl BIT(1) NULL,
    created_date DATETIME(6) NULL,
    updated_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rfq_tier_split_detail
        FOREIGN KEY (rfq_detail_id) REFERENCES rfq_detail (id),
    CONSTRAINT fk_rfq_tier_split_supplier
        FOREIGN KEY (supplier_id) REFERENCES supplier (id),
    CONSTRAINT uk_rfq_tier_split_detail_qty
        UNIQUE (rfq_detail_id, quantity)
);

CREATE TABLE rfq_additional_cost (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rfq_header_id VARCHAR(255) NOT NULL,
    cost_type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    unit VARCHAR(50) NULL,
    amount DECIMAL(18,4) NULL,
    sort_order INT NULL,
    created_date DATETIME(6) NULL,
    updated_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rfq_additional_cost_header
        FOREIGN KEY (rfq_header_id) REFERENCES rfq_header (id)
);
