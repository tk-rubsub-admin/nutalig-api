-- MySQL: repair only sales_order_detail 167 (quotation_detail 411).
-- Source: NTL-CUST-00070-003 / NTL-RFQ202608000234 / detail 387 / tier 1026.
-- Run the entire script in one connection. It includes COMMIT.
-- Sales prices, quantities, totals, the first SO item and other documents are untouched.

START TRANSACTION;

SELECT id, sales_order_no, quotation_detail_id,
       rfq_detail_id, rfq_tier_id, supplier_quote_tier_id, supplier_id,
       supplier_currency, supplier_unit_price, supplier_shipping_cost,
       supplier_total_unit_cost, shipping_method, quantity, unit_price, amount
FROM sales_order_detail
WHERE id = 167 AND sales_order_no = 'NTL-SO2026090036'
FOR UPDATE;

UPDATE sales_order_detail AS sod
JOIN sales_order AS so ON so.sales_order_no = sod.sales_order_no
JOIN quotation_detail AS qd
  ON qd.id = sod.quotation_detail_id AND qd.quotation_no = so.quotation_no
JOIN rfq_tier AS tier
  ON tier.id = CAST(qd.tier_id AS UNSIGNED)
 AND tier.rfq_detail_id = qd.rfq_detail_id
JOIN rfq_detail AS detail
  ON detail.id = tier.rfq_detail_id AND detail.rfq_header_id = qd.source_rfq_id
JOIN supplier AS supplier ON supplier.id = detail.supplier_id
SET sod.rfq_detail_id = qd.rfq_detail_id,
    sod.rfq_tier_id = tier.id,
    sod.supplier_quote_tier_id = tier.supplier_quote_tier_id,
    sod.supplier_id = supplier.id,
    sod.shipping_method = tier.shipping_method,
    sod.supplier_currency = tier.currency,
    sod.supplier_unit_price = tier.product_price,
    sod.supplier_shipping_cost = COALESCE(tier.shipping_cost, 0),
    sod.supplier_total_unit_cost = tier.product_price + COALESCE(tier.shipping_cost, 0)
WHERE sod.id = 167
  AND sod.line_no = 2
  AND sod.sales_order_no = 'NTL-SO2026090036'
  AND so.quotation_no = 'NTL-CUST-00070-003'
  AND qd.id = 411
  AND qd.source_rfq_id = 'NTL-RFQ202608000234'
  AND qd.rfq_detail_id = 387
  AND tier.id = 1026
  AND tier.supplier_quote_tier_id = 1348
  AND supplier.id = 'NTL-SUP-0157'
  AND tier.shipping_method = 'LAND'
  AND detail.is_archived = 0 AND tier.is_archived = 0
  AND sod.rfq_tier_split_id IS NULL AND qd.tier_split_id IS NULL
  AND sod.rfq_detail_id IN (626, 387)
  AND sod.rfq_tier_id IN (3902, 1026)
  AND sod.supplier_quote_tier_id IN (1989, 1348)
  AND sod.supplier_id IN ('NTL-SUP-0262', 'NTL-SUP-0157');

-- First run: expected 1. Rerunning after repair: normally 0.
-- If 0, inspect the final SELECT: the row may be repaired or no longer match the guards.
SELECT ROW_COUNT() AS updated_rows;

SELECT id, sales_order_no, quotation_detail_id,
       rfq_detail_id, rfq_tier_id, supplier_quote_tier_id, supplier_id,
       supplier_currency, supplier_unit_price, supplier_shipping_cost,
       supplier_total_unit_cost, shipping_method, quantity, unit_price, amount
FROM sales_order_detail
WHERE sales_order_no = 'NTL-SO2026090036'
ORDER BY line_no;

COMMIT;
