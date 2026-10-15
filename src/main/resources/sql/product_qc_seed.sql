-- Source: MAN-MGMT-001 Rev.00 (06/10/2026), Taxonomy Rev.10.
-- Existing-master snapshot: 11/10/2026. Never infer QC from polymer names at runtime.
INSERT INTO qc_route (code, name_th, name_en, route_type, qc_material_group_code, created_by, updated_by) VALUES
    ('MAT-QC-01', 'QC วัสดุแก้ว', 'Glass QC', 'MATERIAL', 'GLASS', 'taxonomy-rev10', 'taxonomy-rev10'),
    ('MAT-QC-02', 'QC วัสดุพลาสติก', 'Plastic QC', 'MATERIAL', 'PLASTIC', 'taxonomy-rev10', 'taxonomy-rev10'),
    ('MAT-QC-03', 'QC วัสดุโลหะ', 'Metal QC', 'MATERIAL', 'METAL', 'taxonomy-rev10', 'taxonomy-rev10'),
    ('MAT-QC-04', 'QC กระดาษและกระดาษแข็ง', 'Paper & Board QC', 'MATERIAL', 'PAPER_BOARD', 'taxonomy-rev10', 'taxonomy-rev10'),
    ('MAT-QC-05', 'QC ฟิล์มและวัสดุลามิเนต', 'Film & Laminate QC', 'MATERIAL', 'FILM_LAMINATE', 'taxonomy-rev10', 'taxonomy-rev10'),
    ('MAT-QC-06', 'QC ผ้า นอนวูฟเวน และวัสดุเส้นใยแบบแผ่น', 'Fabric, Non-woven & Fiber Sheet QC', 'MATERIAL', 'FABRIC_FIBER_SHEET', 'taxonomy-rev10', 'taxonomy-rev10'),
    ('MAT-QC-07', 'QC ยาง ซิลิโคน และวัสดุอีลาสโตเมอร์', 'Rubber, Silicone & Elastomer QC', 'MATERIAL', 'RUBBER_ELASTOMER', 'taxonomy-rev10', 'taxonomy-rev10'),
    ('MAT-QC-08', 'QC วัสดุแข็งเฉพาะทาง', 'Specialty Hard Materials QC', 'MATERIAL', 'SPECIALTY_HARD', 'taxonomy-rev10', 'taxonomy-rev10'),
    ('MAT-QC-09', 'QC ฟองน้ำและวัสดุโฟม', 'Sponge & Foam QC', 'MATERIAL', 'SPONGE_FOAM', 'taxonomy-rev10', 'taxonomy-rev10'),
    ('SPECIAL-F13', 'แผนตรวจเฉพาะมาส์ก แพด และแผ่นแปะ', 'Family 13 / Special Inspection Plan', 'SPECIAL', NULL, 'taxonomy-rev10', 'taxonomy-rev10'),
    ('QC-HOLD', 'รอจัดซื้อหรือฝ่ายเทคนิคยืนยัน QC', 'Purchasing / Technical confirmation required', 'HOLD', NULL, 'taxonomy-rev10', 'taxonomy-rev10');

-- Preserve uncertain materials as DRAFT; they cannot act as runtime defaults.
INSERT INTO product_material_qc_rule
    (taxonomy_revision, product_family_code, product_material_code, qc_route_code,
     purchasing_confirmation_policy, status, created_by, updated_by)
SELECT 'REV10', m.product_family_code, m.code, 'QC-HOLD', 'ALWAYS', 'DRAFT', 'taxonomy-rev10', 'taxonomy-rev10'
FROM product_material m;

-- Family identity is matched explicitly, not by the ordinal number in the manual.
INSERT INTO product_family_qc_policy
    (taxonomy_revision, product_family_code, special_inspection_plan_required,
     special_qc_route_code, created_by, updated_by)
SELECT 'REV10', f.code, TRUE, 'SPECIAL-F13', 'taxonomy-rev10', 'taxonomy-rev10'
FROM product_family f
WHERE LOWER(TRIM(f.name_en)) = 'mask / pad / patch'
  AND (SELECT COUNT(*) FROM product_family WHERE LOWER(TRIM(name_en)) = 'mask / pad / patch') = 1;

-- Only exact, reviewed source/master matches become ACTIVE. Source materials are
-- expanded from the manual's Technical Material examples for each product format.
INSERT INTO product_material_qc_rule
    (taxonomy_revision, product_family_code, product_material_code, product_subtype1_code,
     qc_route_code, purchasing_confirmation_policy, status, created_by, updated_by)
SELECT 'REV10', f.code, m.code, s.code, src.route_code, src.confirmation_policy,
       'ACTIVE', 'taxonomy-rev10', 'taxonomy-rev10'
FROM (
    SELECT 'PF001' AS family_code, 'Bottles' AS family_name, 'PMAT0001' AS material_code, 'Glass' AS material_en, 'แก้ว' AS material_th, 'PST0001' AS subtype_code, 'Pump Bottle' AS subtype_name, 'MAT-QC-01' AS route_code, 'IF_UNCERTAIN' AS confirmation_policy
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0002', 'PET', 'พลาสติก PET', 'PST0001', 'Pump Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0001', 'Pump Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0004', 'PE', 'พลาสติก PE', 'PST0001', 'Pump Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0001', 'Pump Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0001', 'Pump Bottle', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0042', 'Metal', 'โลหะ', 'PST0001', 'Pump Bottle', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0001', 'Glass', 'แก้ว', 'PST0002', 'Spray Bottle', 'MAT-QC-01', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0002', 'PET', 'พลาสติก PET', 'PST0002', 'Spray Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0002', 'Spray Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0004', 'PE', 'พลาสติก PE', 'PST0002', 'Spray Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0002', 'Spray Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0002', 'Spray Bottle', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0042', 'Metal', 'โลหะ', 'PST0002', 'Spray Bottle', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0002', 'PET', 'พลาสติก PET', 'PST0003', 'Foam Pump Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0003', 'Foam Pump Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0004', 'PE', 'พลาสติก PE', 'PST0003', 'Foam Pump Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0001', 'Glass', 'แก้ว', 'PST0004', 'Reducer Bottle', 'MAT-QC-01', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0002', 'PET', 'พลาสติก PET', 'PST0004', 'Reducer Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0004', 'Reducer Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0004', 'PE', 'พลาสติก PE', 'PST0004', 'Reducer Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0004', 'Reducer Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0004', 'Reducer Bottle', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0042', 'Metal', 'โลหะ', 'PST0004', 'Reducer Bottle', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0001', 'Glass', 'แก้ว', 'PST0005', 'Pour Bottle', 'MAT-QC-01', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0002', 'PET', 'พลาสติก PET', 'PST0005', 'Pour Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0005', 'Pour Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0004', 'PE', 'พลาสติก PE', 'PST0005', 'Pour Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0005', 'Pour Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0005', 'Pour Bottle', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0042', 'Metal', 'โลหะ', 'PST0005', 'Pour Bottle', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0001', 'Glass', 'แก้ว', 'PST0006', 'Roll-on Bottle', 'MAT-QC-01', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0002', 'PET', 'พลาสติก PET', 'PST0006', 'Roll-on Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0006', 'Roll-on Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0004', 'PE', 'พลาสติก PE', 'PST0006', 'Roll-on Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0006', 'Roll-on Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0006', 'Roll-on Bottle', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0042', 'Metal', 'โลหะ', 'PST0006', 'Roll-on Bottle', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0002', 'PET', 'พลาสติก PET', 'PST0007', 'Squeezable Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0007', 'Squeezable Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0004', 'PE', 'พลาสติก PE', 'PST0007', 'Squeezable Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0002', 'PET', 'พลาสติก PET', 'PST0075', 'Packer Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0075', 'Packer Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0004', 'PE', 'พลาสติก PE', 'PST0075', 'Packer Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0001', 'Glass', 'แก้ว', 'PST0075', 'Packer Bottle', 'MAT-QC-01', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0001', 'Glass', 'แก้ว', 'PST0076', 'Dual-Compartment Bottle', 'MAT-QC-01', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0002', 'PET', 'พลาสติก PET', 'PST0076', 'Dual-Compartment Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0076', 'Dual-Compartment Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0004', 'PE', 'พลาสติก PE', 'PST0076', 'Dual-Compartment Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0076', 'Dual-Compartment Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0076', 'Dual-Compartment Bottle', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF001', 'Bottles', 'PMAT0042', 'Metal', 'โลหะ', 'PST0076', 'Dual-Compartment Bottle', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF003', 'Dropper Packaging', 'PMAT0007', 'Glass', 'แก้ว', 'PST0008', 'Bulb Dropper Packaging', 'MAT-QC-01', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF003', 'Dropper Packaging', 'PMAT0002', 'PET', 'พลาสติก PET', 'PST0008', 'Bulb Dropper Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF003', 'Dropper Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0008', 'Bulb Dropper Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF003', 'Dropper Packaging', 'PMAT0011', 'PETG', 'พลาสติก PETG', 'PST0008', 'Bulb Dropper Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF003', 'Dropper Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0008', 'Bulb Dropper Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF003', 'Dropper Packaging', 'PMAT0007', 'Glass', 'แก้ว', 'PST0009', 'Push Button Dropper Packaging', 'MAT-QC-01', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF003', 'Dropper Packaging', 'PMAT0002', 'PET', 'พลาสติก PET', 'PST0009', 'Push Button Dropper Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF003', 'Dropper Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0009', 'Push Button Dropper Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF003', 'Dropper Packaging', 'PMAT0011', 'PETG', 'พลาสติก PETG', 'PST0009', 'Push Button Dropper Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF003', 'Dropper Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0009', 'Push Button Dropper Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0043', '', 'PP (พลาสติก PP)', 'PST0077', 'Airless Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0044', '', 'PET (พลาสติก PET)', 'PST0077', 'Airless Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0045', '', 'AS (พลาสติก AS)', 'PST0077', 'Airless Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0046', '', 'SAN (พลาสติก SAN)', 'PST0077', 'Airless Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0047', '', 'PETG (พลาสติก PETG)', 'PST0077', 'Airless Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0048', '', 'PE (พลาสติก PE)', 'PST0077', 'Airless Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0049', '', 'Acrylic (อะคริลิก)', 'PST0077', 'Airless Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0050', '', 'Aluminum (อลูมิเนียม)', 'PST0077', 'Airless Bottle', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0051', '', 'Glass (แก้ว)', 'PST0077', 'Airless Bottle', 'MAT-QC-01', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0043', '', 'PP (พลาสติก PP)', 'PST0078', 'Airless Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0044', '', 'PET (พลาสติก PET)', 'PST0078', 'Airless Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0045', '', 'AS (พลาสติก AS)', 'PST0078', 'Airless Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0046', '', 'SAN (พลาสติก SAN)', 'PST0078', 'Airless Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0047', '', 'PETG (พลาสติก PETG)', 'PST0078', 'Airless Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0048', '', 'PE (พลาสติก PE)', 'PST0078', 'Airless Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0049', '', 'Acrylic (อะคริลิก)', 'PST0078', 'Airless Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0050', '', 'Aluminum (อลูมิเนียม)', 'PST0078', 'Airless Jar', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0051', '', 'Glass (แก้ว)', 'PST0078', 'Airless Jar', 'MAT-QC-01', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0043', '', 'PP (พลาสติก PP)', 'PST0079', 'Airless Tube', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0044', '', 'PET (พลาสติก PET)', 'PST0079', 'Airless Tube', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0045', '', 'AS (พลาสติก AS)', 'PST0079', 'Airless Tube', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0046', '', 'SAN (พลาสติก SAN)', 'PST0079', 'Airless Tube', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0047', '', 'PETG (พลาสติก PETG)', 'PST0079', 'Airless Tube', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF004', 'Airless Packaging', 'PMAT0048', '', 'PE (พลาสติก PE)', 'PST0079', 'Airless Tube', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0014', 'Glass', 'แก้ว', 'PST0013', 'Jar', 'MAT-QC-01', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0002', 'PET', 'พลาสติก PET', 'PST0013', 'Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0013', 'Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0010', 'SAN', 'พลาสติก SAN', 'PST0013', 'Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0015', 'AS', 'พลาสติก AS', 'PST0013', 'Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0052', '', 'PETG (พลาสติก PETG)', 'PST0013', 'Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0053', '', 'PS (พลาสติก PS)', 'PST0013', 'Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0013', 'Jar', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0013', 'Jar', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0054', '', 'Tinplate (แผ่นเหล็กเคลือบดีบุก)', 'PST0013', 'Jar', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0014', 'Glass', 'แก้ว', 'PST0014', 'Canister', 'MAT-QC-01', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0002', 'PET', 'พลาสติก PET', 'PST0014', 'Canister', 'MAT-QC-02', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0014', 'Canister', 'MAT-QC-02', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0010', 'SAN', 'พลาสติก SAN', 'PST0014', 'Canister', 'MAT-QC-02', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0015', 'AS', 'พลาสติก AS', 'PST0014', 'Canister', 'MAT-QC-02', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0052', '', 'PETG (พลาสติก PETG)', 'PST0014', 'Canister', 'MAT-QC-02', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0053', '', 'PS (พลาสติก PS)', 'PST0014', 'Canister', 'MAT-QC-02', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0014', 'Canister', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF005', 'Jars & Canisters', 'PMAT0054', '', 'Tinplate (แผ่นเหล็กเคลือบดีบุก)', 'PST0014', 'Canister', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF006', 'Tubes', 'PMAT0004', 'PE', 'พลาสติก PE', 'PST0016', 'Extruded Tube', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF006', 'Tubes', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0018', 'Aluminum Tube', 'MAT-QC-03', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0014', 'Glass', 'แก้ว', 'PST0095', 'Perfume Vial / Mini Bottle', 'MAT-QC-01', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0095', 'Perfume Vial / Mini Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0095', 'Perfume Vial / Mini Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0095', 'Perfume Vial / Mini Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0014', 'Glass', 'แก้ว', 'PST0020', 'Reed Diffuser Bottle', 'MAT-QC-01', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0020', 'Reed Diffuser Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0020', 'Reed Diffuser Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0020', 'Reed Diffuser Bottle', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0020', 'Ceramic', 'เซรามิก', 'PST0020', 'Reed Diffuser Bottle', 'MAT-QC-08', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0014', 'Glass', 'แก้ว', 'PST0021', 'Car Fragrance Packaging', 'MAT-QC-01', 'IF_UNCERTAIN'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0021', 'Car Fragrance Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0021', 'Car Fragrance Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0021', 'Car Fragrance Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0021', 'Car Fragrance Packaging', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0021', 'Zinc Alloy', 'โลหะผสมสังกะสี', 'PST0021', 'Car Fragrance Packaging', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0018', 'Wood', 'ไม้', 'PST0021', 'Car Fragrance Packaging', 'MAT-QC-08', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0020', 'Ceramic', 'เซรามิก', 'PST0021', 'Car Fragrance Packaging', 'MAT-QC-08', 'ALWAYS'
    UNION ALL
    SELECT 'PF002', 'Perfume & Fragrance Packaging', 'PMAT0001', 'Synthetic Fiber', 'เส้นใยสังเคราะห์', 'PST0112', 'Reed Diffuser Stick', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0008', '', 'PP (พลาสติก PP)', 'PST0022', 'Compact', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0056', '', 'ABS (พลาสติก ABS)', 'PST0022', 'Compact', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0057', '', 'AS (พลาสติก AS)', 'PST0022', 'Compact', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0058', '', 'SAN (พลาสติก SAN)', 'PST0022', 'Compact', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0059', '', 'PETG (พลาสติก PETG)', 'PST0022', 'Compact', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0060', '', 'PETG (พลาสติก PETG)', 'PST0022', 'Compact', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0022', 'Compact', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0022', 'Compact', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0008', '', 'PP (พลาสติก PP)', 'PST0023', 'Cushion', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0056', '', 'ABS (พลาสติก ABS)', 'PST0023', 'Cushion', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0057', '', 'AS (พลาสติก AS)', 'PST0023', 'Cushion', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0058', '', 'SAN (พลาสติก SAN)', 'PST0023', 'Cushion', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0059', '', 'PETG (พลาสติก PETG)', 'PST0023', 'Cushion', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0060', '', 'PETG (พลาสติก PETG)', 'PST0023', 'Cushion', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0023', 'Cushion', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0023', 'Cushion', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0008', '', 'PP (พลาสติก PP)', 'PST0024', 'Loose Powder Container', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0056', '', 'ABS (พลาสติก ABS)', 'PST0024', 'Loose Powder Container', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0057', '', 'AS (พลาสติก AS)', 'PST0024', 'Loose Powder Container', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0058', '', 'SAN (พลาสติก SAN)', 'PST0024', 'Loose Powder Container', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0059', '', 'PETG (พลาสติก PETG)', 'PST0024', 'Loose Powder Container', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0060', '', 'PETG (พลาสติก PETG)', 'PST0024', 'Loose Powder Container', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0024', 'Loose Powder Container', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0024', 'Loose Powder Container', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0014', 'Glass', 'แก้ว', 'PST0024', 'Loose Powder Container', 'MAT-QC-01', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0008', '', 'PP (พลาสติก PP)', 'PST0025', 'Palette', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0056', '', 'ABS (พลาสติก ABS)', 'PST0025', 'Palette', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0057', '', 'AS (พลาสติก AS)', 'PST0025', 'Palette', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0058', '', 'SAN (พลาสติก SAN)', 'PST0025', 'Palette', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0059', '', 'PETG (พลาสติก PETG)', 'PST0025', 'Palette', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0060', '', 'PETG (พลาสติก PETG)', 'PST0025', 'Palette', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0025', 'Palette', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0025', 'Palette', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF008', 'Makeup Case Packaging', 'PMAT0061', '', 'Paperboard (กระดาษกล่อง)', 'PST0025', 'Palette', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0026', 'Liquid Lip Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0026', 'Liquid Lip Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0011', 'PETG', 'พลาสติก PETG', 'PST0026', 'Liquid Lip Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0026', 'Liquid Lip Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0026', 'Liquid Lip Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0026', 'Liquid Lip Packaging', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0014', 'Glass', 'แก้ว', 'PST0026', 'Liquid Lip Packaging', 'MAT-QC-01', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0027', 'Mascara Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0027', 'Mascara Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0011', 'PETG', 'พลาสติก PETG', 'PST0027', 'Mascara Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0027', 'Mascara Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0027', 'Mascara Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0027', 'Mascara Packaging', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0014', 'Glass', 'แก้ว', 'PST0027', 'Mascara Packaging', 'MAT-QC-01', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0028', 'Concealer Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0028', 'Concealer Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0011', 'PETG', 'พลาสติก PETG', 'PST0028', 'Concealer Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0028', 'Concealer Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0028', 'Concealer Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0028', 'Concealer Packaging', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0014', 'Glass', 'แก้ว', 'PST0028', 'Concealer Packaging', 'MAT-QC-01', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0029', 'Brow Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0029', 'Brow Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0011', 'PETG', 'พลาสติก PETG', 'PST0029', 'Brow Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0029', 'Brow Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0029', 'Brow Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0029', 'Brow Packaging', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF015', 'Makeup Applicator Packaging', 'PMAT0014', 'Glass', 'แก้ว', 'PST0029', 'Brow Packaging', 'MAT-QC-01', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0031', 'Eyeliner Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0031', 'Eyeliner Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0031', 'Eyeliner Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0031', 'Eyeliner Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0031', 'Eyeliner Pen Packaging', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0032', 'Concealer Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0032', 'Concealer Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0032', 'Concealer Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0032', 'Concealer Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0032', 'Concealer Pen Packaging', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0033', 'Brow Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0033', 'Brow Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0033', 'Brow Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0033', 'Brow Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0033', 'Brow Pen Packaging', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0034', 'Highlight Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0034', 'Highlight Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0034', 'Highlight Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0034', 'Highlight Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0034', 'Highlight Pen Packaging', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0035', 'Contour Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0035', 'Contour Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0035', 'Contour Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0035', 'Contour Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0035', 'Contour Pen Packaging', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0036', 'Lip Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0036', 'Lip Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0036', 'Lip Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0036', 'Lip Pen Packaging', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0036', 'Lip Pen Packaging', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0091', 'Multi-use Makeup Pen', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0091', 'Multi-use Makeup Pen', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0091', 'Multi-use Makeup Pen', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0005', 'Acrylic', 'อะคริลิก', 'PST0091', 'Multi-use Makeup Pen', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF016', 'Makeup Pen Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0091', 'Multi-use Makeup Pen', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0037', 'Lipstick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0008', '', 'POM (พลาสติก POM)', 'PST0037', 'Lipstick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0037', 'Lipstick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0062', '', 'POM (พลาสติก POM)', 'PST0037', 'Lipstick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0037', 'Lipstick', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0063', '', 'Paperboard (กระดาษกล่อง)', 'PST0037', 'Lipstick', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0038', 'Foundation Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0008', '', 'POM (พลาสติก POM)', 'PST0038', 'Foundation Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0038', 'Foundation Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0062', '', 'POM (พลาสติก POM)', 'PST0038', 'Foundation Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0038', 'Foundation Stick', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0063', '', 'Paperboard (กระดาษกล่อง)', 'PST0038', 'Foundation Stick', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0039', 'Concealer Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0008', '', 'POM (พลาสติก POM)', 'PST0039', 'Concealer Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0039', 'Concealer Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0062', '', 'POM (พลาสติก POM)', 'PST0039', 'Concealer Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0039', 'Concealer Stick', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0063', '', 'Paperboard (กระดาษกล่อง)', 'PST0039', 'Concealer Stick', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0040', 'Highlight Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0008', '', 'POM (พลาสติก POM)', 'PST0040', 'Highlight Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0040', 'Highlight Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0062', '', 'POM (พลาสติก POM)', 'PST0040', 'Highlight Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0040', 'Highlight Stick', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0063', '', 'Paperboard (กระดาษกล่อง)', 'PST0040', 'Highlight Stick', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0041', 'Contour Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0008', '', 'POM (พลาสติก POM)', 'PST0041', 'Contour Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0041', 'Contour Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0062', '', 'POM (พลาสติก POM)', 'PST0041', 'Contour Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0041', 'Contour Stick', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0063', '', 'Paperboard (กระดาษกล่อง)', 'PST0041', 'Contour Stick', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0042', 'Blush Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0008', '', 'POM (พลาสติก POM)', 'PST0042', 'Blush Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0042', 'Blush Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0062', '', 'POM (พลาสติก POM)', 'PST0042', 'Blush Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0042', 'Blush Stick', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0063', '', 'Paperboard (กระดาษกล่อง)', 'PST0042', 'Blush Stick', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0043', 'Multi-use Makeup Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0008', '', 'POM (พลาสติก POM)', 'PST0043', 'Multi-use Makeup Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0043', 'Multi-use Makeup Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0062', '', 'POM (พลาสติก POM)', 'PST0043', 'Multi-use Makeup Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0043', 'Multi-use Makeup Stick', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF009', 'Makeup Stick Packaging', 'PMAT0063', '', 'Paperboard (กระดาษกล่อง)', 'PST0043', 'Multi-use Makeup Stick', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0077', '', 'Rubycell (ฟองน้ำรูบี้เซลล์)', 'PST0080', 'Puffs & Makeup Sponges', 'MAT-QC-09', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0078', '', 'NBR (ยางสังเคราะห์ NBR)', 'PST0080', 'Puffs & Makeup Sponges', 'MAT-QC-09', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0079', '', 'SBR (ยางสังเคราะห์ SBR)', 'PST0080', 'Puffs & Makeup Sponges', 'MAT-QC-09', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0080', '', 'NR Latex (ยางธรรมชาติ)', 'PST0080', 'Puffs & Makeup Sponges', 'MAT-QC-09', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0081', '', 'PU (โพลียูรีเทน)', 'PST0080', 'Puffs & Makeup Sponges', 'MAT-QC-09', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0082', '', 'Silicone (ซิลิโคน)', 'PST0080', 'Puffs & Makeup Sponges', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0083', '', 'Velour (ผ้ากำมะหยี่)', 'PST0080', 'Puffs & Makeup Sponges', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0084', '', 'Microfiber (ไมโครไฟเบอร์)', 'PST0080', 'Puffs & Makeup Sponges', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0085', '', 'Synthetic Fiber (เส้นใยสังเคราะห์)', 'PST0080', 'Puffs & Makeup Sponges', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0086', '', 'Cotton (ผ้าฝ้าย)', 'PST0080', 'Puffs & Makeup Sponges', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0083', '', 'Velour (ผ้ากำมะหยี่)', 'PST0081', 'False Eyelashes', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0084', '', 'Microfiber (ไมโครไฟเบอร์)', 'PST0081', 'False Eyelashes', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0085', '', 'Synthetic Fiber (เส้นใยสังเคราะห์)', 'PST0081', 'False Eyelashes', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0086', '', 'Cotton (ผ้าฝ้าย)', 'PST0081', 'False Eyelashes', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0087', '', 'Plastic (พลาสติก)', 'PST0081', 'False Eyelashes', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0083', '', 'Velour (ผ้ากำมะหยี่)', 'PST0082', 'Makeup Brushes', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0084', '', 'Microfiber (ไมโครไฟเบอร์)', 'PST0082', 'Makeup Brushes', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0085', '', 'Synthetic Fiber (เส้นใยสังเคราะห์)', 'PST0082', 'Makeup Brushes', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0086', '', 'Cotton (ผ้าฝ้าย)', 'PST0082', 'Makeup Brushes', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0087', '', 'Plastic (พลาสติก)', 'PST0082', 'Makeup Brushes', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0088', '', 'Aluminum (อลูมิเนียม)', 'PST0082', 'Makeup Brushes', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0090', '', 'Wood (ไม้)', 'PST0082', 'Makeup Brushes', 'MAT-QC-08', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0083', '', 'Velour (ผ้ากำมะหยี่)', 'PST0083', 'Makeup Applicators', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0084', '', 'Microfiber (ไมโครไฟเบอร์)', 'PST0083', 'Makeup Applicators', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0085', '', 'Synthetic Fiber (เส้นใยสังเคราะห์)', 'PST0083', 'Makeup Applicators', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0086', '', 'Cotton (ผ้าฝ้าย)', 'PST0083', 'Makeup Applicators', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0082', '', 'Silicone (ซิลิโคน)', 'PST0083', 'Makeup Applicators', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0087', '', 'Plastic (พลาสติก)', 'PST0083', 'Makeup Applicators', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0088', '', 'Aluminum (อลูมิเนียม)', 'PST0083', 'Makeup Applicators', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0090', '', 'Wood (ไม้)', 'PST0083', 'Makeup Applicators', 'MAT-QC-08', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0082', '', 'Silicone (ซิลิโคน)', 'PST0084', 'Beauty Tools', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0087', '', 'Plastic (พลาสติก)', 'PST0084', 'Beauty Tools', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0088', '', 'Aluminum (อลูมิเนียม)', 'PST0084', 'Beauty Tools', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF017', 'Beauty Accessories & Tools', 'PMAT0090', '', 'Wood (ไม้)', 'PST0084', 'Beauty Tools', 'MAT-QC-08', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0047', 'Sunscreen Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0047', 'Sunscreen Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0047', 'Sunscreen Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0075', 'Acrylic', 'อะคริลิก', 'PST0047', 'Sunscreen Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0047', 'Sunscreen Stick', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0076', 'Paperboard', 'กระดาษกล่อง', 'PST0047', 'Sunscreen Stick', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0048', 'Balm Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0048', 'Balm Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0048', 'Balm Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0075', 'Acrylic', 'อะคริลิก', 'PST0048', 'Balm Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0048', 'Balm Stick', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0076', 'Paperboard', 'กระดาษกล่อง', 'PST0048', 'Balm Stick', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0049', 'Skincare Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0049', 'Skincare Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0049', 'Skincare Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0075', 'Acrylic', 'อะคริลิก', 'PST0049', 'Skincare Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0049', 'Skincare Stick', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0076', 'Paperboard', 'กระดาษกล่อง', 'PST0049', 'Skincare Stick', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0050', 'Deodorant Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0050', 'Deodorant Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0050', 'Deodorant Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0075', 'Acrylic', 'อะคริลิก', 'PST0050', 'Deodorant Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0050', 'Deodorant Stick', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0076', 'Paperboard', 'กระดาษกล่อง', 'PST0050', 'Deodorant Stick', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0003', 'PP', 'พลาสติก PP', 'PST0051', 'Perfume Balm Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0008', 'Plastic', 'พลาสติก', 'PST0051', 'Perfume Balm Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0019', 'ABS', 'พลาสติก ABS', 'PST0051', 'Perfume Balm Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0075', 'Acrylic', 'อะคริลิก', 'PST0051', 'Perfume Balm Stick', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0006', 'Aluminum', 'อลูมิเนียม', 'PST0051', 'Perfume Balm Stick', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF014', 'Stick Packaging', 'PMAT0076', 'Paperboard', 'กระดาษกล่อง', 'PST0051', 'Perfume Balm Stick', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0032', 'Plastic Film', 'ฟิล์มพลาสติก', 'PST0055', 'Sachet', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0033', 'Aluminum Foil', 'ฟอยล์อลูมิเนียม', 'PST0055', 'Sachet', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0034', 'Paper', 'กระดาษ', 'PST0055', 'Sachet', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0035', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0055', 'Sachet', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0032', 'Plastic Film', 'ฟิล์มพลาสติก', 'PST0056', 'Stick Pack', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0033', 'Aluminum Foil', 'ฟอยล์อลูมิเนียม', 'PST0056', 'Stick Pack', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0034', 'Paper', 'กระดาษ', 'PST0056', 'Stick Pack', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0035', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0056', 'Stick Pack', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0032', 'Plastic Film', 'ฟิล์มพลาสติก', 'PST0057', 'Stand-up Pouch', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0033', 'Aluminum Foil', 'ฟอยล์อลูมิเนียม', 'PST0057', 'Stand-up Pouch', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0034', 'Paper', 'กระดาษ', 'PST0057', 'Stand-up Pouch', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0035', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0057', 'Stand-up Pouch', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0032', 'Plastic Film', 'ฟิล์มพลาสติก', 'PST0058', 'Spout Pouch', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0033', 'Aluminum Foil', 'ฟอยล์อลูมิเนียม', 'PST0058', 'Spout Pouch', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0034', 'Paper', 'กระดาษ', 'PST0058', 'Spout Pouch', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0035', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0058', 'Spout Pouch', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0032', 'Plastic Film', 'ฟิล์มพลาสติก', 'PST0059', 'Side Gusset Pouch', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0033', 'Aluminum Foil', 'ฟอยล์อลูมิเนียม', 'PST0059', 'Side Gusset Pouch', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0034', 'Paper', 'กระดาษ', 'PST0059', 'Side Gusset Pouch', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0035', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0059', 'Side Gusset Pouch', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0032', 'Plastic Film', 'ฟิล์มพลาสติก', 'PST0060', 'Quad Seal Pouch', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0033', 'Aluminum Foil', 'ฟอยล์อลูมิเนียม', 'PST0060', 'Quad Seal Pouch', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0034', 'Paper', 'กระดาษ', 'PST0060', 'Quad Seal Pouch', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0035', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0060', 'Quad Seal Pouch', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0032', 'Plastic Film', 'ฟิล์มพลาสติก', 'PST0061', 'Shaped Pouch', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0033', 'Aluminum Foil', 'ฟอยล์อลูมิเนียม', 'PST0061', 'Shaped Pouch', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0034', 'Paper', 'กระดาษ', 'PST0061', 'Shaped Pouch', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0035', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0061', 'Shaped Pouch', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0032', 'Plastic Film', 'ฟิล์มพลาสติก', 'PST0062', 'Roll Film', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0033', 'Aluminum Foil', 'ฟอยล์อลูมิเนียม', 'PST0062', 'Roll Film', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0034', 'Paper', 'กระดาษ', 'PST0062', 'Roll Film', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF007', 'Sachet & Flexible Packaging', 'PMAT0035', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0062', 'Roll Film', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0036', 'Paperboard', 'กระดาษกล่อง', 'PST0063', 'Folding Carton', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0037', 'Greyboard', 'กระดาษจั่วปัง', 'PST0063', 'Folding Carton', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0038', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0063', 'Folding Carton', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0064', '', 'Art Card (อาร์ตการ์ด)', 'PST0063', 'Folding Carton', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0065', '', 'Corrugated Paper (กระดาษลูกฟูก)', 'PST0063', 'Folding Carton', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0066', '', 'Specialty Paper (กระดาษพิเศษ)', 'PST0063', 'Folding Carton', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0036', 'Paperboard', 'กระดาษกล่อง', 'PST0067', 'Lid and Base Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0037', 'Greyboard', 'กระดาษจั่วปัง', 'PST0067', 'Lid and Base Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0038', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0067', 'Lid and Base Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0064', '', 'Art Card (อาร์ตการ์ด)', 'PST0067', 'Lid and Base Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0065', '', 'Corrugated Paper (กระดาษลูกฟูก)', 'PST0067', 'Lid and Base Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0066', '', 'Specialty Paper (กระดาษพิเศษ)', 'PST0067', 'Lid and Base Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0036', 'Paperboard', 'กระดาษกล่อง', 'PST0069', 'Book Style Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0037', 'Greyboard', 'กระดาษจั่วปัง', 'PST0069', 'Book Style Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0038', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0069', 'Book Style Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0064', '', 'Art Card (อาร์ตการ์ด)', 'PST0069', 'Book Style Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0065', '', 'Corrugated Paper (กระดาษลูกฟูก)', 'PST0069', 'Book Style Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0066', '', 'Specialty Paper (กระดาษพิเศษ)', 'PST0069', 'Book Style Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0036', 'Paperboard', 'กระดาษกล่อง', 'PST0066', 'Hinged / Clamshell Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0037', 'Greyboard', 'กระดาษจั่วปัง', 'PST0066', 'Hinged / Clamshell Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0038', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0066', 'Hinged / Clamshell Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0064', '', 'Art Card (อาร์ตการ์ด)', 'PST0066', 'Hinged / Clamshell Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0065', '', 'Corrugated Paper (กระดาษลูกฟูก)', 'PST0066', 'Hinged / Clamshell Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0066', '', 'Specialty Paper (กระดาษพิเศษ)', 'PST0066', 'Hinged / Clamshell Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0036', 'Paperboard', 'กระดาษกล่อง', 'PST0064', 'Drawer Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0037', 'Greyboard', 'กระดาษจั่วปัง', 'PST0064', 'Drawer Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0038', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0064', 'Drawer Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0064', '', 'Art Card (อาร์ตการ์ด)', 'PST0064', 'Drawer Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0065', '', 'Corrugated Paper (กระดาษลูกฟูก)', 'PST0064', 'Drawer Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0066', '', 'Specialty Paper (กระดาษพิเศษ)', 'PST0064', 'Drawer Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0036', 'Paperboard', 'กระดาษกล่อง', 'PST0065', 'Sleeve Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0037', 'Greyboard', 'กระดาษจั่วปัง', 'PST0065', 'Sleeve Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0038', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0065', 'Sleeve Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0064', '', 'Art Card (อาร์ตการ์ด)', 'PST0065', 'Sleeve Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0065', '', 'Corrugated Paper (กระดาษลูกฟูก)', 'PST0065', 'Sleeve Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0066', '', 'Specialty Paper (กระดาษพิเศษ)', 'PST0065', 'Sleeve Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0036', 'Paperboard', 'กระดาษกล่อง', 'PST0068', 'Mailer Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0037', 'Greyboard', 'กระดาษจั่วปัง', 'PST0068', 'Mailer Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0038', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0068', 'Mailer Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0064', '', 'Art Card (อาร์ตการ์ด)', 'PST0068', 'Mailer Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0065', '', 'Corrugated Paper (กระดาษลูกฟูก)', 'PST0068', 'Mailer Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0066', '', 'Specialty Paper (กระดาษพิเศษ)', 'PST0068', 'Mailer Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0036', 'Paperboard', 'กระดาษกล่อง', 'PST0070', 'Display Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0037', 'Greyboard', 'กระดาษจั่วปัง', 'PST0070', 'Display Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0038', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0070', 'Display Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0064', '', 'Art Card (อาร์ตการ์ด)', 'PST0070', 'Display Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0065', '', 'Corrugated Paper (กระดาษลูกฟูก)', 'PST0070', 'Display Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0066', '', 'Specialty Paper (กระดาษพิเศษ)', 'PST0070', 'Display Box', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0036', 'Paperboard', 'กระดาษกล่อง', 'PST0071', 'Shipping Carton', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0037', 'Greyboard', 'กระดาษจั่วปัง', 'PST0071', 'Shipping Carton', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0038', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0071', 'Shipping Carton', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0064', '', 'Art Card (อาร์ตการ์ด)', 'PST0071', 'Shipping Carton', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0065', '', 'Corrugated Paper (กระดาษลูกฟูก)', 'PST0071', 'Shipping Carton', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF011', 'Boxes', 'PMAT0066', '', 'Specialty Paper (กระดาษพิเศษ)', 'PST0071', 'Shipping Carton', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0039', 'Coated Paper', 'กระดาษเคลือบ', 'PST0072', 'Paper Shopping Bag', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0040', 'Kraft Paper', 'กระดาษคราฟท์', 'PST0072', 'Paper Shopping Bag', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0041', 'Recycled Paper', 'กระดาษรีไซเคิล', 'PST0072', 'Paper Shopping Bag', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0069', '', 'Plastic Film (ฟิล์มพลาสติก)', 'PST0092', 'Plastic Shopping Bag', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0068', '', 'Nylon (ไนลอน)', 'PST0073', 'Non-woven Shopping Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0071', '', 'Woven PP (พลาสติก PP สาน)', 'PST0073', 'Non-woven Shopping Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0072', '', 'Canvas (ผ้าแคนวาส)', 'PST0073', 'Non-woven Shopping Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0073', '', 'Cotton (ผ้าฝ้าย)', 'PST0073', 'Non-woven Shopping Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0068', '', 'Nylon (ไนลอน)', 'PST0074', 'Fabric Tote / Shopping Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0071', '', 'Woven PP (พลาสติก PP สาน)', 'PST0074', 'Fabric Tote / Shopping Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0072', '', 'Canvas (ผ้าแคนวาส)', 'PST0074', 'Fabric Tote / Shopping Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0073', '', 'Cotton (ผ้าฝ้าย)', 'PST0074', 'Fabric Tote / Shopping Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0068', '', 'Nylon (ไนลอน)', 'PST0093', 'Insulated Carry Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0071', '', 'Woven PP (พลาสติก PP สาน)', 'PST0093', 'Insulated Carry Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0072', '', 'Canvas (ผ้าแคนวาส)', 'PST0093', 'Insulated Carry Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0073', '', 'Cotton (ผ้าฝ้าย)', 'PST0093', 'Insulated Carry Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0069', '', 'Plastic Film (ฟิล์มพลาสติก)', 'PST0093', 'Insulated Carry Bag', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0068', '', 'Nylon (ไนลอน)', 'PST0094', 'Cooler Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0071', '', 'Woven PP (พลาสติก PP สาน)', 'PST0094', 'Cooler Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0072', '', 'Canvas (ผ้าแคนวาส)', 'PST0094', 'Cooler Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF013', 'Carry Bags & Shopping Bags', 'PMAT0073', '', 'Cotton (ผ้าฝ้าย)', 'PST0094', 'Cooler Bag', 'MAT-QC-06', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0091', '', 'Paper (กระดาษ)', 'PST0085', 'Adhesive Label', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0092', '', 'Synthetic Paper (กระดาษสังเคราะห์)', 'PST0085', 'Adhesive Label', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0093', '', 'PET (พลาสติก PET)', 'PST0085', 'Adhesive Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0094', '', 'PP / BOPP', 'PST0085', 'Adhesive Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0095', '', 'PE (พลาสติก PE)', 'PST0085', 'Adhesive Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0096', '', 'PVC / Vinyl', 'PST0085', 'Adhesive Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0097', '', 'PETG Shrink Film (ฟิล์มหด PETG)', 'PST0085', 'Adhesive Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0098', '', 'PVC Shrink Film (ฟิล์มหด PVC)', 'PST0085', 'Adhesive Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0100', '', 'Transfer Carrier Film (ฟิล์มพาหะสำหรับงานทรานเฟอร์)', 'PST0085', 'Adhesive Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0102', '', 'Holographic Security Film (ฟิล์มโฮโลแกรมกันปลอม)', 'PST0085', 'Adhesive Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0093', '', 'PET (พลาสติก PET)', 'PST0086', 'Shrink Sleeve Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0094', '', 'PP / BOPP', 'PST0086', 'Shrink Sleeve Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0095', '', 'PE (พลาสติก PE)', 'PST0086', 'Shrink Sleeve Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0096', '', 'PVC / Vinyl', 'PST0086', 'Shrink Sleeve Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0097', '', 'PETG Shrink Film (ฟิล์มหด PETG)', 'PST0086', 'Shrink Sleeve Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0098', '', 'PVC Shrink Film (ฟิล์มหด PVC)', 'PST0086', 'Shrink Sleeve Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0100', '', 'Transfer Carrier Film (ฟิล์มพาหะสำหรับงานทรานเฟอร์)', 'PST0086', 'Shrink Sleeve Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0102', '', 'Holographic Security Film (ฟิล์มโฮโลแกรมกันปลอม)', 'PST0086', 'Shrink Sleeve Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0093', '', 'PET (พลาสติก PET)', 'PST0087', 'Transfer Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0094', '', 'PP / BOPP', 'PST0087', 'Transfer Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0095', '', 'PE (พลาสติก PE)', 'PST0087', 'Transfer Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0096', '', 'PVC / Vinyl', 'PST0087', 'Transfer Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0097', '', 'PETG Shrink Film (ฟิล์มหด PETG)', 'PST0087', 'Transfer Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0098', '', 'PVC Shrink Film (ฟิล์มหด PVC)', 'PST0087', 'Transfer Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0100', '', 'Transfer Carrier Film (ฟิล์มพาหะสำหรับงานทรานเฟอร์)', 'PST0087', 'Transfer Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0102', '', 'Holographic Security Film (ฟิล์มโฮโลแกรมกันปลอม)', 'PST0087', 'Transfer Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0093', '', 'PET (พลาสติก PET)', 'PST0088', 'In-Mold Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0094', '', 'PP / BOPP', 'PST0088', 'In-Mold Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0095', '', 'PE (พลาสติก PE)', 'PST0088', 'In-Mold Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0096', '', 'PVC / Vinyl', 'PST0088', 'In-Mold Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0097', '', 'PETG Shrink Film (ฟิล์มหด PETG)', 'PST0088', 'In-Mold Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0098', '', 'PVC Shrink Film (ฟิล์มหด PVC)', 'PST0088', 'In-Mold Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0100', '', 'Transfer Carrier Film (ฟิล์มพาหะสำหรับงานทรานเฟอร์)', 'PST0088', 'In-Mold Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0102', '', 'Holographic Security Film (ฟิล์มโฮโลแกรมกันปลอม)', 'PST0088', 'In-Mold Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0101', '', 'Aluminum (อลูมิเนียม)', 'PST0089', 'Aluminum Plate', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0091', '', 'Paper (กระดาษ)', 'PST0090', 'Security / Tamper-Evident Label', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0092', '', 'Synthetic Paper (กระดาษสังเคราะห์)', 'PST0090', 'Security / Tamper-Evident Label', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0093', '', 'PET (พลาสติก PET)', 'PST0090', 'Security / Tamper-Evident Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0094', '', 'PP / BOPP', 'PST0090', 'Security / Tamper-Evident Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0095', '', 'PE (พลาสติก PE)', 'PST0090', 'Security / Tamper-Evident Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0096', '', 'PVC / Vinyl', 'PST0090', 'Security / Tamper-Evident Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0097', '', 'PETG Shrink Film (ฟิล์มหด PETG)', 'PST0090', 'Security / Tamper-Evident Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0098', '', 'PVC Shrink Film (ฟิล์มหด PVC)', 'PST0090', 'Security / Tamper-Evident Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0100', '', 'Transfer Carrier Film (ฟิล์มพาหะสำหรับงานทรานเฟอร์)', 'PST0090', 'Security / Tamper-Evident Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF018', 'Labels & Decorative Stickers', 'PMAT0102', '', 'Holographic Security Film (ฟิล์มโฮโลแกรมกันปลอม)', 'PST0090', 'Security / Tamper-Evident Label', 'MAT-QC-05', 'ALWAYS'
    UNION ALL
    SELECT 'PF019', 'Sampling & Testing Materials', 'PMAT0103', '', 'Paper (กระดาษ)', 'PST0097', 'Sample Card', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF019', 'Sampling & Testing Materials', 'PMAT0104', '', 'Coated Paper (กระดาษเคลือบ)', 'PST0097', 'Sample Card', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF019', 'Sampling & Testing Materials', 'PMAT0105', '', 'Blotter Paper (กระดาษบล็อตเตอร์)', 'PST0097', 'Sample Card', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF019', 'Sampling & Testing Materials', 'PMAT0106', '', 'Synthetic Paper (กระดาษสังเคราะห์)', 'PST0097', 'Sample Card', 'MAT-QC-04', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0108', '', 'PP', 'PST0101', 'Closure / Cap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0109', '', 'PE', 'PST0101', 'Closure / Cap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0110', '', 'PET', 'PST0101', 'Closure / Cap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0111', '', 'PETG', 'PST0101', 'Closure / Cap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0112', '', 'ABS (พลาสติก ABS)', 'PST0101', 'Closure / Cap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0113', '', 'POM (พลาสติก POM)', 'PST0101', 'Closure / Cap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0114', '', 'Acrylic (อะคริลิก)', 'PST0101', 'Closure / Cap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0021', 'Zinc Alloy', 'โลหะผสมสังกะสี', 'PST0101', 'Closure / Cap', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0115', '', 'Aluminum (อลูมิเนียม)', 'PST0101', 'Closure / Cap', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0117', '', 'Zinc Alloy (โลหะผสมสังกะสี)', 'PST0101', 'Closure / Cap', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0119', '', 'Wood (ไม้)', 'PST0101', 'Closure / Cap', 'MAT-QC-08', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0108', '', 'PP', 'PST0102', 'Pump', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0109', '', 'PE', 'PST0102', 'Pump', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0110', '', 'PET', 'PST0102', 'Pump', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0111', '', 'PETG', 'PST0102', 'Pump', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0112', '', 'ABS (พลาสติก ABS)', 'PST0102', 'Pump', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0113', '', 'POM (พลาสติก POM)', 'PST0102', 'Pump', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0021', 'Zinc Alloy', 'โลหะผสมสังกะสี', 'PST0102', 'Pump', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0115', '', 'Aluminum (อลูมิเนียม)', 'PST0102', 'Pump', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0117', '', 'Zinc Alloy (โลหะผสมสังกะสี)', 'PST0102', 'Pump', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0121', '', 'Silicone (ซิลิโคน)', 'PST0102', 'Pump', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0122', '', 'TPE', 'PST0102', 'Pump', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0123', '', 'NBR (ยางสังเคราะห์ NBR)', 'PST0102', 'Pump', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0108', '', 'PP', 'PST0103', 'Sprayer', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0109', '', 'PE', 'PST0103', 'Sprayer', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0110', '', 'PET', 'PST0103', 'Sprayer', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0111', '', 'PETG', 'PST0103', 'Sprayer', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0112', '', 'ABS (พลาสติก ABS)', 'PST0103', 'Sprayer', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0113', '', 'POM (พลาสติก POM)', 'PST0103', 'Sprayer', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0021', 'Zinc Alloy', 'โลหะผสมสังกะสี', 'PST0103', 'Sprayer', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0115', '', 'Aluminum (อลูมิเนียม)', 'PST0103', 'Sprayer', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0117', '', 'Zinc Alloy (โลหะผสมสังกะสี)', 'PST0103', 'Sprayer', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0121', '', 'Silicone (ซิลิโคน)', 'PST0103', 'Sprayer', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0122', '', 'TPE', 'PST0103', 'Sprayer', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0123', '', 'NBR (ยางสังเคราะห์ NBR)', 'PST0103', 'Sprayer', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0108', '', 'PP', 'PST0104', 'Foamer Pump', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0109', '', 'PE', 'PST0104', 'Foamer Pump', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0110', '', 'PET', 'PST0104', 'Foamer Pump', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0111', '', 'PETG', 'PST0104', 'Foamer Pump', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0112', '', 'ABS (พลาสติก ABS)', 'PST0104', 'Foamer Pump', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0113', '', 'POM (พลาสติก POM)', 'PST0104', 'Foamer Pump', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0021', 'Zinc Alloy', 'โลหะผสมสังกะสี', 'PST0104', 'Foamer Pump', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0115', '', 'Aluminum (อลูมิเนียม)', 'PST0104', 'Foamer Pump', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0117', '', 'Zinc Alloy (โลหะผสมสังกะสี)', 'PST0104', 'Foamer Pump', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0121', '', 'Silicone (ซิลิโคน)', 'PST0104', 'Foamer Pump', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0122', '', 'TPE', 'PST0104', 'Foamer Pump', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0123', '', 'NBR (ยางสังเคราะห์ NBR)', 'PST0104', 'Foamer Pump', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0118', '', 'Glass (แก้ว)', 'PST0105', 'Dropper Component', 'MAT-QC-01', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0108', '', 'PP', 'PST0105', 'Dropper Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0109', '', 'PE', 'PST0105', 'Dropper Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0110', '', 'PET', 'PST0105', 'Dropper Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0111', '', 'PETG', 'PST0105', 'Dropper Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0112', '', 'ABS (พลาสติก ABS)', 'PST0105', 'Dropper Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0113', '', 'POM (พลาสติก POM)', 'PST0105', 'Dropper Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0021', 'Zinc Alloy', 'โลหะผสมสังกะสี', 'PST0105', 'Dropper Component', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0115', '', 'Aluminum (อลูมิเนียม)', 'PST0105', 'Dropper Component', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0117', '', 'Zinc Alloy (โลหะผสมสังกะสี)', 'PST0105', 'Dropper Component', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0121', '', 'Silicone (ซิลิโคน)', 'PST0105', 'Dropper Component', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0122', '', 'TPE', 'PST0105', 'Dropper Component', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0123', '', 'NBR (ยางสังเคราะห์ NBR)', 'PST0105', 'Dropper Component', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0118', '', 'Glass (แก้ว)', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-01', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0108', '', 'PP', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0109', '', 'PE', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0110', '', 'PET', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0111', '', 'PETG', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0112', '', 'ABS (พลาสติก ABS)', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0113', '', 'POM (พลาสติก POM)', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0021', 'Zinc Alloy', 'โลหะผสมสังกะสี', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0115', '', 'Aluminum (อลูมิเนียม)', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0117', '', 'Zinc Alloy (โลหะผสมสังกะสี)', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0120', '', 'Ceramic (เซรามิก)', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-08', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0121', '', 'Silicone (ซิลิโคน)', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0122', '', 'TPE', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0123', '', 'NBR (ยางสังเคราะห์ NBR)', 'PST0106', 'Roller / Applicator Component', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0108', '', 'PP', 'PST0107', 'Inner Plug / Reducer', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0109', '', 'PE', 'PST0107', 'Inner Plug / Reducer', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0110', '', 'PET', 'PST0107', 'Inner Plug / Reducer', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0111', '', 'PETG', 'PST0107', 'Inner Plug / Reducer', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0112', '', 'ABS (พลาสติก ABS)', 'PST0107', 'Inner Plug / Reducer', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0113', '', 'POM (พลาสติก POM)', 'PST0107', 'Inner Plug / Reducer', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0121', '', 'Silicone (ซิลิโคน)', 'PST0107', 'Inner Plug / Reducer', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0122', '', 'TPE', 'PST0107', 'Inner Plug / Reducer', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0123', '', 'NBR (ยางสังเคราะห์ NBR)', 'PST0107', 'Inner Plug / Reducer', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0108', '', 'PP', 'PST0108', 'Collar / Overcap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0109', '', 'PE', 'PST0108', 'Collar / Overcap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0110', '', 'PET', 'PST0108', 'Collar / Overcap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0111', '', 'PETG', 'PST0108', 'Collar / Overcap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0112', '', 'ABS (พลาสติก ABS)', 'PST0108', 'Collar / Overcap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0113', '', 'POM (พลาสติก POM)', 'PST0108', 'Collar / Overcap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0114', '', 'Acrylic (อะคริลิก)', 'PST0108', 'Collar / Overcap', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0021', 'Zinc Alloy', 'โลหะผสมสังกะสี', 'PST0108', 'Collar / Overcap', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0115', '', 'Aluminum (อลูมิเนียม)', 'PST0108', 'Collar / Overcap', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0117', '', 'Zinc Alloy (โลหะผสมสังกะสี)', 'PST0108', 'Collar / Overcap', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0119', '', 'Wood (ไม้)', 'PST0108', 'Collar / Overcap', 'MAT-QC-08', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0108', '', 'PP', 'PST0109', 'Other Packaging Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0109', '', 'PE', 'PST0109', 'Other Packaging Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0110', '', 'PET', 'PST0109', 'Other Packaging Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0111', '', 'PETG', 'PST0109', 'Other Packaging Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0112', '', 'ABS (พลาสติก ABS)', 'PST0109', 'Other Packaging Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0113', '', 'POM (พลาสติก POM)', 'PST0109', 'Other Packaging Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0114', '', 'Acrylic (อะคริลิก)', 'PST0109', 'Other Packaging Component', 'MAT-QC-02', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0021', 'Zinc Alloy', 'โลหะผสมสังกะสี', 'PST0109', 'Other Packaging Component', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0115', '', 'Aluminum (อลูมิเนียม)', 'PST0109', 'Other Packaging Component', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0117', '', 'Zinc Alloy (โลหะผสมสังกะสี)', 'PST0109', 'Other Packaging Component', 'MAT-QC-03', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0118', '', 'Glass (แก้ว)', 'PST0109', 'Other Packaging Component', 'MAT-QC-01', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0119', '', 'Wood (ไม้)', 'PST0109', 'Other Packaging Component', 'MAT-QC-08', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0120', '', 'Ceramic (เซรามิก)', 'PST0109', 'Other Packaging Component', 'MAT-QC-08', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0121', '', 'Silicone (ซิลิโคน)', 'PST0109', 'Other Packaging Component', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0122', '', 'TPE', 'PST0109', 'Other Packaging Component', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF020', 'Closures, Dispensers & Packaging Components', 'PMAT0123', '', 'NBR (ยางสังเคราะห์ NBR)', 'PST0109', 'Other Packaging Component', 'MAT-QC-07', 'ALWAYS'
    UNION ALL
    SELECT 'PF022', 'Oral Care Accessories & Tools', 'PMAT0001', 'Plastic', 'พลาสติก', 'PST0113', 'Toothbrush', 'MAT-QC-02', 'ALWAYS'
) src
JOIN product_family f ON f.code = src.family_code AND f.is_active = TRUE
    AND f.name_en = src.family_name
JOIN product_material m ON m.code = src.material_code AND m.product_family_code = f.code
    AND COALESCE(m.name_en, '') = src.material_en AND COALESCE(m.name_th, '') = src.material_th
JOIN product_subtype1 s ON s.code = src.subtype_code AND s.product_family_code = f.code
    AND s.name_en = src.subtype_name;

-- Explicit family/material defaults established by the manual, including the
-- Family 13 exceptions whose subtype tables are empty in the source document.
UPDATE product_material_qc_rule
SET qc_route_code = 'MAT-QC-02', status = 'ACTIVE'
WHERE taxonomy_revision = 'REV10' AND product_subtype1_code IS NULL
  AND EXISTS (
      SELECT 1 FROM product_material m JOIN product_family f ON f.code = m.product_family_code
      WHERE m.code = product_material_qc_rule.product_material_code
        AND m.product_family_code = product_material_qc_rule.product_family_code
        AND f.is_active = TRUE AND f.name_en = 'Bottles' AND m.name_en = 'PE'
  );

UPDATE product_material_qc_rule
SET qc_route_code = 'SPECIAL-F13', status = 'ACTIVE'
WHERE taxonomy_revision = 'REV10' AND product_subtype1_code IS NULL
  AND EXISTS (
      SELECT 1 FROM product_material m JOIN product_family f ON f.code = m.product_family_code
      WHERE m.code = product_material_qc_rule.product_material_code
        AND m.product_family_code = product_material_qc_rule.product_family_code
        AND f.is_active = TRUE AND f.name_en = 'Mask / Pad / Patch' AND m.name_en = 'Hydrogel'
  );

UPDATE product_material_qc_rule
SET qc_route_code = 'SPECIAL-F13', status = 'ACTIVE'
WHERE taxonomy_revision = 'REV10' AND product_subtype1_code IS NULL
  AND EXISTS (
      SELECT 1 FROM product_material m JOIN product_family f ON f.code = m.product_family_code
      WHERE m.code = product_material_qc_rule.product_material_code
        AND m.product_family_code = product_material_qc_rule.product_family_code
        AND f.is_active = TRUE AND f.name_en = 'Mask / Pad / Patch' AND m.name_en = 'Hydrocolloid'
  );

