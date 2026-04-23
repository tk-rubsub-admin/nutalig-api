CREATE TABLE request_price_detail (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_price_header_id VARCHAR(255) NOT NULL,
    option_name VARCHAR(255) NULL,
    spec TEXT NOT NULL,
    sort_order INT NULL,
    remark TEXT NULL,
    created_date DATETIME(6) NULL,
    updated_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_request_price_detail_header
        FOREIGN KEY (request_price_header_id) REFERENCES request_price_header (id)
);

CREATE TABLE request_price_tier (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_price_detail_id BIGINT NOT NULL,
    quantity DECIMAL(18,0) NOT NULL,
    product_price DECIMAL(18,4) NOT NULL,
    land_freight_cost DECIMAL(18,4) NULL,
    sea_freight_cost DECIMAL(18,4) NULL,
    land_total_price DECIMAL(18,4) NULL,
    sea_total_price DECIMAL(18,4) NULL,
    sort_order INT NULL,
    created_date DATETIME(6) NULL,
    updated_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_request_price_tier_detail
        FOREIGN KEY (request_price_detail_id) REFERENCES request_price_detail (id),
    CONSTRAINT uk_request_price_tier_detail_qty
        UNIQUE (request_price_detail_id, quantity)
);

CREATE TABLE request_price_additional_cost (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_price_header_id VARCHAR(255) NOT NULL,
    cost_type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    unit VARCHAR(50) NULL,
    amount DECIMAL(18,4) NULL,
    sort_order INT NULL,
    created_date DATETIME(6) NULL,
    updated_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_request_price_additional_cost_header
        FOREIGN KEY (request_price_header_id) REFERENCES request_price_header (id)
);
