CREATE VIEW v_product_material_qc_rule AS
SELECT r.id AS rule_id,
       r.taxonomy_revision,
       r.product_family_code,
       f.name_th AS family_name_th,
       f.name_en AS family_name_en,
       r.product_material_code,
       m.name_th AS material_name_th,
       m.name_en AS material_name_en,
       r.product_subtype1_code,
       s.name_th AS subtype1_name_th,
       s.name_en AS subtype1_name_en,
       q.qc_material_group_code AS qc_material_group,
       q.code AS qc_attachment_code,
       q.route_type,
       r.purchasing_confirmation_policy,
       COALESCE(p.special_inspection_plan_required, FALSE) AS special_inspection_plan_required,
       p.special_qc_route_code
FROM product_material_qc_rule r
JOIN product_family f ON f.code = r.product_family_code AND f.is_active = TRUE
JOIN product_material m ON m.code = r.product_material_code AND m.product_family_code = r.product_family_code
LEFT JOIN product_subtype1 s ON s.code = r.product_subtype1_code AND s.product_family_code = r.product_family_code
JOIN qc_route q ON q.code = r.qc_route_code AND q.is_active = TRUE
LEFT JOIN product_family_qc_policy p ON p.taxonomy_revision = r.taxonomy_revision
    AND p.product_family_code = r.product_family_code AND p.is_active = TRUE
WHERE r.status = 'ACTIVE';
