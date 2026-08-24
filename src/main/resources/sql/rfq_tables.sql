CREATE TABLE IF NOT EXISTS rfq_header (
    id VARCHAR(255) NOT NULL,
    requested_date DATETIME(6) DEFAULT NULL,
    status VARCHAR(50) DEFAULT NULL,
    contact_name VARCHAR(255) DEFAULT NULL,
    contact_phone VARCHAR(255) DEFAULT NULL,
    sales_id VARCHAR(255) DEFAULT NULL,
    customer_id VARCHAR(255) DEFAULT NULL,
    rfq_type VARCHAR(255) DEFAULT NULL,
    order_type VARCHAR(255) DEFAULT NULL,
    product_family VARCHAR(50) DEFAULT NULL,
    product_usage VARCHAR(50) DEFAULT NULL,
    system_mechanic VARCHAR(50) DEFAULT NULL,
    material VARCHAR(50) DEFAULT NULL,
    capacity VARCHAR(255) DEFAULT NULL,
    target_price DECIMAL(18,4) DEFAULT NULL,
    requested_moq TEXT DEFAULT NULL,
    is_request_sample BIT(1) DEFAULT b'0',
    is_urgent_request BIT(1) DEFAULT b'0',
    urgent_request_reason TEXT DEFAULT NULL,
    urgent_request_status VARCHAR(30) DEFAULT NULL,
    urgent_requested_by VARCHAR(255) DEFAULT NULL,
    urgent_requested_date DATETIME(6) DEFAULT NULL,
    urgent_approved_by VARCHAR(255) DEFAULT NULL,
    urgent_approved_date DATETIME(6) DEFAULT NULL,
    urgent_rejected_by VARCHAR(255) DEFAULT NULL,
    urgent_rejected_date DATETIME(6) DEFAULT NULL,
    urgent_reject_reason TEXT DEFAULT NULL,
    description TEXT DEFAULT NULL,
    created_by VARCHAR(255) DEFAULT NULL,
    updated_by VARCHAR(255) DEFAULT NULL,
    procurement_id VARCHAR(255) DEFAULT NULL,
    sla_date DATETIME(6) DEFAULT NULL,
    quoted_date DATETIME(6) DEFAULT NULL,
    quotation_no VARCHAR(50) DEFAULT NULL,
    shipping_method VARCHAR(20) DEFAULT 'ALL',
    request_information TEXT DEFAULT NULL,
    created_date DATETIME(6) DEFAULT NULL,
    updated_date DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_rfq_header_sales_id (sales_id),
    KEY idx_rfq_header_customer_id (customer_id),
    KEY idx_rfq_header_procurement_id (procurement_id),
    KEY idx_rfq_header_product_family (product_family),
    KEY idx_rfq_header_product_usage (product_usage),
    KEY idx_rfq_header_system_mechanic (system_mechanic),
    KEY idx_rfq_header_material_family (material, product_family),
    CONSTRAINT fk_rfq_header_sales
        FOREIGN KEY (sales_id) REFERENCES employee (employee_id),
    CONSTRAINT fk_rfq_header_customer
        FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT fk_rfq_header_procurement
        FOREIGN KEY (procurement_id) REFERENCES employee (employee_id),
    CONSTRAINT fk_rfq_header_product_family
        FOREIGN KEY (product_family) REFERENCES product_family (code),
    CONSTRAINT fk_rfq_header_product_subtype1
        FOREIGN KEY (product_usage) REFERENCES product_subtype1 (code),
    CONSTRAINT fk_rfq_header_product_subtype2
        FOREIGN KEY (system_mechanic) REFERENCES product_subtype2 (code),
    CONSTRAINT fk_rfq_header_material
        FOREIGN KEY (material, product_family) REFERENCES product_material (code, product_family_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rfq_pictures (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rfq_header_id VARCHAR(255) NOT NULL,
    pic_url VARCHAR(1000) DEFAULT NULL,
    sort INT DEFAULT NULL,
    updated_date DATETIME(6) DEFAULT NULL,
    updated_by VARCHAR(255) DEFAULT NULL,
    file_name VARCHAR(255) DEFAULT NULL,
    file_type VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_rfq_pictures_header_id (rfq_header_id),
    CONSTRAINT fk_rfq_pictures_header
        FOREIGN KEY (rfq_header_id) REFERENCES rfq_header (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rfq_detail (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rfq_header_id VARCHAR(255) NOT NULL,
    option_name VARCHAR(255) DEFAULT NULL,
    spec TEXT NOT NULL,
    sort_order INT DEFAULT NULL,
    remark TEXT DEFAULT NULL,
    recommend TEXT DEFAULT NULL,
    commission DECIMAL(18,4) DEFAULT NULL,
    package_dimension TEXT DEFAULT NULL,
    package_weight TEXT DEFAULT NULL,
    package_capacity TEXT DEFAULT NULL,
    supplier_id VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_vi_0900_ai_ci DEFAULT NULL,
    created_by VARCHAR(255) DEFAULT NULL,
    updated_by VARCHAR(255) DEFAULT NULL,
    created_date DATETIME(6) DEFAULT NULL,
    updated_date DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_rfq_detail_header_id (rfq_header_id),
    KEY idx_rfq_detail_supplier_id (supplier_id),
    CONSTRAINT fk_rfq_detail_header
        FOREIGN KEY (rfq_header_id) REFERENCES rfq_header (id),
    CONSTRAINT fk_rfq_detail_supplier
        FOREIGN KEY (supplier_id) REFERENCES supplier (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rfq_supplier_quote_detail_package (
    id BIGINT NOT NULL AUTO_INCREMENT,
    quote_detail_id BIGINT NOT NULL,
    package_name VARCHAR(255) DEFAULT NULL,
    package_dimension TEXT DEFAULT NULL,
    package_weight TEXT DEFAULT NULL,
    package_capacity TEXT DEFAULT NULL,
    sort_order INT DEFAULT NULL,
    created_date DATETIME(6) DEFAULT NULL,
    updated_date DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_rfq_supplier_quote_detail_package_detail (quote_detail_id),
    CONSTRAINT fk_rfq_supplier_quote_detail_package_detail
        FOREIGN KEY (quote_detail_id) REFERENCES rfq_supplier_quote_detail (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rfq_tier (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rfq_detail_id BIGINT NOT NULL,
    quantity DECIMAL(18,0) NOT NULL,
    product_price DECIMAL(18,4) NOT NULL,
    commission DECIMAL(18,4) DEFAULT NULL,
    currency VARCHAR(10) DEFAULT NULL,
    container_size VARCHAR(20) DEFAULT NULL,
    exchange_rate DECIMAL(18,4) DEFAULT NULL,
    land_freight_cost DECIMAL(18,4) DEFAULT NULL,
    sea_freight_cost DECIMAL(18,4) DEFAULT NULL,
    land_total_price DECIMAL(18,4) DEFAULT NULL,
    sea_total_price DECIMAL(18,4) DEFAULT NULL,
    supplier_quote_tier_id BIGINT DEFAULT NULL,
    sort_order INT DEFAULT NULL,
    supplier_id VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_vi_0900_ai_ci DEFAULT NULL,
    created_date DATETIME(6) DEFAULT NULL,
    updated_date DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_rfq_tier_detail_id (rfq_detail_id),
    KEY idx_rfq_tier_supplier_id (supplier_id),
    CONSTRAINT fk_rfq_tier_detail
        FOREIGN KEY (rfq_detail_id) REFERENCES rfq_detail (id),
    CONSTRAINT fk_rfq_tier_supplier
        FOREIGN KEY (supplier_id) REFERENCES supplier (id),
    CONSTRAINT uk_rfq_tier_detail_qty
        UNIQUE (rfq_detail_id, quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rfq_tier_split (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rfq_detail_id BIGINT NOT NULL,
    supplier_id VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_vi_0900_ai_ci DEFAULT NULL,
    quantity DECIMAL(18,0) NOT NULL,
    sell_price DECIMAL(18,4) NOT NULL,
    commission DECIMAL(18,4) DEFAULT NULL,
    currency VARCHAR(10) DEFAULT NULL,
    container_size VARCHAR(20) DEFAULT NULL,
    land_freight_cost DECIMAL(18,4) DEFAULT NULL,
    land_freight_qty DECIMAL(18,4) DEFAULT NULL,
    sea_freight_qty DECIMAL(18,4) DEFAULT NULL,
    sea_freight_cost DECIMAL(18,4) DEFAULT NULL,
    is_fcl BIT(1) DEFAULT NULL,
    is_share_fcl BIT(1) DEFAULT NULL,
    created_date DATETIME(6) DEFAULT NULL,
    updated_date DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_rfq_tier_split_detail_id (rfq_detail_id),
    KEY idx_rfq_tier_split_supplier_id (supplier_id),
    CONSTRAINT fk_rfq_tier_split_detail
        FOREIGN KEY (rfq_detail_id) REFERENCES rfq_detail (id),
    CONSTRAINT fk_rfq_tier_split_supplier
        FOREIGN KEY (supplier_id) REFERENCES supplier (id),
    CONSTRAINT uk_rfq_tier_split_detail_qty
        UNIQUE (rfq_detail_id, quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rfq_additional_cost (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rfq_header_id VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    unit VARCHAR(50) DEFAULT NULL,
    value VARCHAR(255) DEFAULT NULL,
    sort_order INT DEFAULT NULL,
    supplier_id VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_vi_0900_ai_ci DEFAULT NULL,
    created_date DATETIME(6) DEFAULT NULL,
    updated_date DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_rfq_additional_cost_header_id (rfq_header_id),
    KEY idx_rfq_additional_cost_supplier_id (supplier_id),
    CONSTRAINT fk_rfq_additional_cost_header
        FOREIGN KEY (rfq_header_id) REFERENCES rfq_header (id),
    CONSTRAINT fk_rfq_additional_cost_supplier
        FOREIGN KEY (supplier_id) REFERENCES supplier (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rfq_supplier_inquiry (
    id VARCHAR(255) NOT NULL,
    rfq_header_id VARCHAR(255) NOT NULL,
    version_no INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    thai_message LONGTEXT NOT NULL,
    chinese_message LONGTEXT DEFAULT NULL,
    source_snapshot LONGTEXT DEFAULT NULL,
    created_by VARCHAR(255) DEFAULT NULL,
    updated_by VARCHAR(255) DEFAULT NULL,
    created_date DATETIME(6) DEFAULT NULL,
    updated_date DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_rfq_supplier_inquiry_header_id (rfq_header_id),
    CONSTRAINT uk_rfq_supplier_inquiry_rfq_version
        UNIQUE (rfq_header_id, version_no),
    CONSTRAINT fk_rfq_supplier_inquiry_rfq
        FOREIGN KEY (rfq_header_id) REFERENCES rfq_header (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rfq_status_timeline (
    rfq_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    status_datetime DATETIME(6) NOT NULL,
    PRIMARY KEY (rfq_id, status),
    CONSTRAINT fk_rfq_status_timeline_rfq
        FOREIGN KEY (rfq_id) REFERENCES rfq_header (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
