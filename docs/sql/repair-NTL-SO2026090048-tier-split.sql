-- MySQL: restore only rfq_tier_split_id for SO NTL-SO2026090048.
-- Source quotation: NTL-CUST-03095-002 / RFQ: NTL-RFQ202607000428.
-- Expected: SO detail 182 -> split 9 (LAND), detail 183 -> split 10 (SEA).
-- Run the entire script in one connection using the nutalig database.
-- Includes COMMIT. Prices, quantities, totals and shipping methods are untouched.

START TRANSACTION;

SELECT id, sales_order_no, line_no, quotation_detail_id,
       rfq_detail_id, rfq_tier_id, rfq_tier_split_id,
       supplier_id, shipping_method, quantity
FROM sales_order_detail
WHERE sales_order_no = 'NTL-SO2026090048' AND id IN (182, 183)
ORDER BY id
FOR UPDATE;

UPDATE sales_order_detail AS sod
JOIN sales_order AS so ON so.sales_order_no = sod.sales_order_no
JOIN quotation_detail AS qd
  ON qd.id = sod.quotation_detail_id
 AND qd.quotation_no = so.quotation_no
JOIN rfq_tier_split AS split
  ON split.id = qd.tier_split_id
 AND split.rfq_detail_id = qd.rfq_detail_id
JOIN rfq_detail AS detail
  ON detail.id = split.rfq_detail_id
 AND detail.rfq_header_id = qd.source_rfq_id
SET sod.rfq_tier_split_id = split.id
WHERE sod.sales_order_no = 'NTL-SO2026090048'
  AND so.quotation_no = 'NTL-CUST-03095-002'
  AND qd.source_rfq_id = 'NTL-RFQ202607000428'
  AND sod.rfq_detail_id = 689
  AND qd.rfq_detail_id = sod.rfq_detail_id
  AND sod.supplier_id = 'NTL-SUP-0005'
  AND split.supplier_id = sod.supplier_id
  AND sod.rfq_tier_split_id IS NULL
  AND sod.rfq_tier_id IS NULL
  AND (qd.tier_id IS NULL OR qd.tier_id = '')
  AND detail.is_archived = 0
  AND split.is_archived = 0
  AND (
    (sod.id = 182 AND sod.line_no = 1 AND qd.id = 473
     AND split.id = 9 AND sod.shipping_method = 'LAND'
     AND split.shipping_method = 'LAND')
    OR
    (sod.id = 183 AND sod.line_no = 2 AND qd.id = 474
     AND split.id = 10 AND sod.shipping_method = 'SEA'
     AND split.shipping_method = 'SEA_SHARE_FCL_40HQ')
  );

-- First run: expected 2 if both rows still have NULL split IDs.
-- Rerunning after repair: 0. If another count, inspect the results below.
SELECT ROW_COUNT() AS updated_rows;

SELECT id, sales_order_no, line_no, quotation_detail_id,
       rfq_detail_id, rfq_tier_id, rfq_tier_split_id,
       supplier_id, shipping_method, quantity
FROM sales_order_detail
WHERE sales_order_no = 'NTL-SO2026090048'
ORDER BY line_no, id;

COMMIT;
