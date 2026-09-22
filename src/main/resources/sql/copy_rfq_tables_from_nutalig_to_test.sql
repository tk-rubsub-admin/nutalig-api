select * from purchase_order where purchase_order_no = 'NTL-PO2026090003';
select * from nutalig.customer where id = 'NTL-CUST-04463';
INSERT INTO nutalig_test.customer (id, customer_name, status, email, customer_type, customer_credit_term, tax_id, company_name, branch_number, branch_name, sales_account, created_date, created_by, updated_date, updated_by, co_sales_account, customer_segment, customer_tier, customer_payment_term, total_sales_order_amount, customer_billing_condition, customer_payment_cycle) VALUES('NTL-CUST-04463', 'บริษัท อาร์เจ  คอสเมติก  (ประเทศไทย)', 'ACTIVE', '', 'COMPANY', 'NON', '0745568005200', NULL, '00', 'สำนักงานใหญ่', 'NTL-SA-BO', '2026-09-10 05:12:49', 'USER-000009', '2026-09-10 05:12:49', 'USER-000009', '', 'MANUFACTURING', 'TIER_4', 'DEP50', NULL, NULL, NULL);
select * from nutalig.supplier where id = 'NTL-SUP-0267';
INSERT INTO nutalig_test.supplier (id, supplier_name, supplier_code, supplier_email, status, full_address, full_address_en, country_code, province, city, district, town, street, detail_address, postal_code, additional, created_date, updated_date) VALUES('NTL-SUP-0267', 'Sanmu Plastic Industry', NULL, NULL, 'ACTIVE', NULL, NULL, 'CN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-09-16 03:56:02.643271', '2026-09-16 03:56:02.643271');
select * from purchase_order_payment pops ;
select * from system_config sc where 1=1;
SET @purchase_order_no := 'NTL-PO2026090003';

  START TRANSACTION;

  -- ตรวจสอบ PO ก่อนลบ
  SELECT
      purchase_order_no,
      sales_order_no,
      supplier_id,
      status,
      grand_total
  FROM purchase_order
  WHERE purchase_order_no = @purchase_order_no;

  -- ลบไฟล์หลักฐานการชำระเงิน
  DELETE payment_attachment
  FROM purchase_order_payment_attachment payment_attachment
  INNER JOIN purchase_order_payment payment
      ON payment.id = payment_attachment.purchase_order_payment_id
  WHERE payment.purchase_order_no = @purchase_order_no;

  -- ลบรายการชำระเงิน
  DELETE FROM purchase_order_payment
  WHERE purchase_order_no = @purchase_order_no;

  -- ลบ Package และ CBM ของรายการสินค้า
  DELETE detail_package
  FROM purchase_order_detail_package detail_package
  INNER JOIN purchase_order_detail detail
      ON detail.id = detail_package.purchase_order_detail_id
  WHERE detail.purchase_order_no = @purchase_order_no;

  -- ลบรายละเอียดสินค้า
  DELETE FROM purchase_order_detail
  WHERE purchase_order_no = @purchase_order_no;

  -- ลบไฟล์แนบ PO
  DELETE FROM purchase_order_attachment
  WHERE purchase_order_no = @purchase_order_no;

  -- ลบหัว PO
  DELETE FROM purchase_order
  WHERE purchase_order_no = @purchase_order_no;

  -- ตรวจสอบว่าลบแล้ว
  SELECT *
  FROM purchase_order
  WHERE purchase_order_no = @purchase_order_no;

  COMMIT;