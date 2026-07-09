CREATE TABLE IF NOT EXISTS supplier_product_capability (
    id VARCHAR(255) NOT NULL,
    supplier_id VARCHAR(255) NOT NULL,
    product_family_code VARCHAR(50) NOT NULL,
    product_material_code VARCHAR(50) DEFAULT NULL,
    status VARCHAR(50) NOT NULL,
    created_date DATETIME DEFAULT NULL,
    updated_date DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_supplier_product_capability_supplier (supplier_id),
    KEY idx_supplier_product_capability_family (product_family_code),
    KEY idx_supplier_product_capability_family_material (product_family_code, product_material_code),
    CONSTRAINT fk_supplier_product_capability_supplier
        FOREIGN KEY (supplier_id) REFERENCES supplier (id),
    CONSTRAINT fk_supplier_product_capability_family
        FOREIGN KEY (product_family_code) REFERENCES product_family (code),
    CONSTRAINT fk_supplier_product_capability_material
        FOREIGN KEY (product_material_code, product_family_code) REFERENCES product_material (code, product_family_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
