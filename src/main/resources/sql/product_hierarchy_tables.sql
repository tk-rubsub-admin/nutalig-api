CREATE TABLE IF NOT EXISTS product_subtype1 (
    code VARCHAR(50) NOT NULL,
    product_family_code VARCHAR(50) NOT NULL,
    name_th VARCHAR(255) DEFAULT NULL,
    name_en VARCHAR(255) DEFAULT NULL,
    subtype2_required TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (code),
    CONSTRAINT fk_product_subtype1_family
        FOREIGN KEY (product_family_code) REFERENCES product_family (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS product_subtype2 (
    code VARCHAR(50) NOT NULL,
    product_subtype1_code VARCHAR(50) NOT NULL,
    name_th VARCHAR(255) DEFAULT NULL,
    name_en VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (code),
    CONSTRAINT fk_product_subtype2_subtype1
        FOREIGN KEY (product_subtype1_code) REFERENCES product_subtype1 (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
