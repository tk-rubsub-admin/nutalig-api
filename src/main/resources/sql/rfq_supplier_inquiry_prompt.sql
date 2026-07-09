INSERT INTO ai_prompt (
    code,
    model,
    temperature,
    system_prompt,
    user_prompt_template,
    active
) VALUES (
    'RFQ_SUPPLIER_INQUIRY_TH',
    NULL,
    NULL,
    NULL,
    'ขอสอบถามราคาสินค้าตามรายละเอียดดังนี้
เลขที่ RFQ: {{rfqId}}
Product Family: {{productFamily}}
Product Subtype 1: {{productSubtype1}}
Product Subtype 2: {{productSubtype2}}
Material: {{material}}
Capacity: {{capacity}}
Description: {{description}}

รายละเอียดรายการ
{{detailSection}}

ค่าใช้จ่ายเพิ่มเติม
{{additionalCostSection}}

รบกวนเสนอราคา พร้อม MOQ, lead time และเงื่อนไขที่เกี่ยวข้องกลับมาด้วย
ขอบคุณค่ะ',
    TRUE
)
ON DUPLICATE KEY UPDATE
    user_prompt_template = VALUES(user_prompt_template),
    active = VALUES(active);
