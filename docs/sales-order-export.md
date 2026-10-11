# Sales Order export

`POST /v1/sales-orders/export`

Requires the same authentication as the Sales Order search API. Send a JSON body
using `SearchSalesOrderRequest`, exactly as for `POST /v1/sales-orders/search`:

```json
{
  "docDateStart": "2026-10-01",
  "docDateEnd": "2026-10-31",
  "customerId": "NTL-CUST-00070",
  "salesId": "EMP-001",
  "statuses": ["CREATED", "ISSUED"],
  "procurementStatus": ["READY_FOR_PO", "READY_FOR_PO_OVERRIDE"],
  "shippingType": "LAND",
  "keyword": "ขวด"
}
```

All fields are optional. Also supports `salesOrderNo`, `status`, and
`urgentRequestStatus`. Combining filters has the same behavior as `/search`;
when both `status` and `statuses` are supplied, both conditions apply.
Date ranges are inclusive and can have just a start or an end.
An empty object or omitted body exports all records.

The response is raw XLSX bytes, without the usual JSON response wrapper:

- `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- `Content-Disposition: attachment; filename="sales-order-export.xlsx"`

The `Sales Orders` sheet contains one row per matching SO, ordered by `docDate`
descending and then `salesOrderNo` descending. There is no pagination.
An empty result produces a valid workbook with only the header row.

Columns include document number/dates/status, quotation number, customer ID/name/tier,
sales ID/name, shipping type, procurement/payment status, currency, subtotal,
discount, freight, VAT, grand total, amount, paid/outstanding total,
payment schedule date, and remark. Customer name uses the document snapshot when
available; otherwise it uses the current customer name/company name. Tier uses the
current customer's `customerTier.nameEn`. Statuses and currency use enum codes.
Dates display as `dd/MM/yyyy`. Amounts are numeric Excel cells; missing amounts
are zero and missing text/date fields are blank.
