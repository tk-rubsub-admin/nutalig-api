CREATE TABLE IF NOT EXISTS rfq_supplier_inquiry (
    id VARCHAR(255) NOT NULL,
    rfq_header_id VARCHAR(255) NOT NULL,
    supplier_id VARCHAR(255) NOT NULL,
    version_no INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    thai_message LONGTEXT NOT NULL,
    chinese_message LONGTEXT DEFAULT NULL,
    source_snapshot LONGTEXT DEFAULT NULL,
    created_by VARCHAR(255) DEFAULT NULL,
    updated_by VARCHAR(255) DEFAULT NULL,
    created_date DATETIME DEFAULT NULL,
    updated_date DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rfq_supplier_inquiry_rfq_version
        UNIQUE (rfq_header_id, version_no),
    CONSTRAINT fk_rfq_supplier_inquiry_rfq
        FOREIGN KEY (rfq_header_id) REFERENCES rfq_header (id),
    CONSTRAINT fk_rfq_supplier_inquiry_supplier
        FOREIGN KEY (supplier_id) REFERENCES supplier (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
