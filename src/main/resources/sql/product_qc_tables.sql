CREATE TABLE IF NOT EXISTS qc_route (
    code VARCHAR(50) NOT NULL,
    name_th VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    route_type VARCHAR(20) NOT NULL,
    qc_material_group_code VARCHAR(50) DEFAULT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255) DEFAULT NULL,
    updated_by VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (code),
    CONSTRAINT ck_qc_route_type CHECK (route_type IN ('MATERIAL', 'SPECIAL', 'HOLD')),
    CONSTRAINT ck_qc_route_group CHECK (
        (route_type = 'MATERIAL' AND qc_material_group_code IS NOT NULL AND TRIM(qc_material_group_code) <> '')
        OR (route_type IN ('SPECIAL', 'HOLD') AND qc_material_group_code IS NULL)
    )
);

CREATE TABLE IF NOT EXISTS qc_document (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_no VARCHAR(100) NOT NULL,
    qc_route_code VARCHAR(50) NOT NULL,
    name_th VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    current_revision_id BIGINT DEFAULT NULL,
    current_revision_status VARCHAR(20) NOT NULL DEFAULT 'RELEASED',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255) DEFAULT NULL,
    updated_by VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_qc_document_no UNIQUE (document_no),
    CONSTRAINT fk_qc_document_route FOREIGN KEY (qc_route_code) REFERENCES qc_route (code),
    CONSTRAINT ck_qc_document_type CHECK (document_type IN ('MATERIAL_QC', 'SPECIAL_PLAN_TEMPLATE')),
    CONSTRAINT ck_qc_document_current_status CHECK (current_revision_status = 'RELEASED'),
    CONSTRAINT ck_qc_document_not_hold CHECK (qc_route_code <> 'QC-HOLD')
);

CREATE TABLE IF NOT EXISTS qc_document_revision (
    id BIGINT NOT NULL AUTO_INCREMENT,
    qc_document_id BIGINT NOT NULL,
    revision_no VARCHAR(30) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    effective_date DATE DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255) DEFAULT NULL,
    updated_by VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_qc_document_revision UNIQUE (qc_document_id, revision_no),
    CONSTRAINT uk_qc_revision_owner_status UNIQUE (id, qc_document_id, status),
    CONSTRAINT fk_qc_revision_document FOREIGN KEY (qc_document_id) REFERENCES qc_document (id),
    CONSTRAINT ck_qc_revision_status CHECK (status IN ('DRAFT', 'RELEASED', 'RETIRED')),
    CONSTRAINT ck_qc_revision_size CHECK (file_size >= 0)
);

CREATE TABLE IF NOT EXISTS product_material_qc_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    taxonomy_revision VARCHAR(20) NOT NULL,
    product_family_code VARCHAR(50) NOT NULL,
    product_material_code VARCHAR(50) NOT NULL,
    product_subtype1_code VARCHAR(50) DEFAULT NULL,
    qc_route_code VARCHAR(50) NOT NULL,
    purchasing_confirmation_policy VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255) DEFAULT NULL,
    updated_by VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_qc_rule_material FOREIGN KEY (product_material_code, product_family_code)
        REFERENCES product_material (code, product_family_code),
    CONSTRAINT fk_product_qc_rule_subtype FOREIGN KEY (product_subtype1_code, product_family_code)
        REFERENCES product_subtype1 (code, product_family_code),
    CONSTRAINT fk_product_qc_rule_route FOREIGN KEY (qc_route_code) REFERENCES qc_route (code),
    CONSTRAINT ck_product_qc_rule_subtype CHECK (product_subtype1_code IS NULL OR TRIM(product_subtype1_code) <> ''),
    CONSTRAINT ck_product_qc_rule_status CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
    CONSTRAINT ck_product_qc_rule_confirmation CHECK (purchasing_confirmation_policy IN ('ALWAYS', 'IF_UNCERTAIN'))
);

CREATE TABLE IF NOT EXISTS product_family_qc_policy (
    taxonomy_revision VARCHAR(20) NOT NULL,
    product_family_code VARCHAR(50) NOT NULL,
    special_inspection_plan_required BOOLEAN NOT NULL DEFAULT FALSE,
    special_qc_route_code VARCHAR(50) DEFAULT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255) DEFAULT NULL,
    updated_by VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (taxonomy_revision, product_family_code),
    CONSTRAINT fk_product_qc_policy_family FOREIGN KEY (product_family_code) REFERENCES product_family (code),
    CONSTRAINT fk_product_qc_policy_route FOREIGN KEY (special_qc_route_code) REFERENCES qc_route (code),
    CONSTRAINT ck_product_qc_policy_plan CHECK (
        (special_inspection_plan_required = TRUE AND special_qc_route_code IS NOT NULL)
        OR (special_inspection_plan_required = FALSE AND special_qc_route_code IS NULL)
    ),
    CONSTRAINT ck_product_qc_policy_special CHECK (special_qc_route_code IS NULL OR special_qc_route_code = 'SPECIAL-F13')
);
