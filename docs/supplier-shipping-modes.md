# Supplier Shipping modes and purchase order Car Code

Supplier Shipping retains `shippingMethod: LAND | SEA` and adds `shippingMode: STANDARD | FCL`.
Existing records and requests that omit `shippingMode` use `STANDARD`.

| PO / quotation shipping method | Supplier Shipping method | Mode | Car Code |
| --- | --- | --- | --- |
| LAND | LAND | STANDARD | Existing land code |
| SEA | SEA | STANDARD | TB… |
| SEA_FCL_20GP / SEA_FCL_40HQ | SEA | FCL | TZ… |
| SEA_SHARE_FCL_20GP / SEA_SHARE_FCL_40HQ | SEA | FCL | TZ… |

Shared containers use the same FCL mode. Container size remains part of the existing shipping-method code.
Each master record has one real Car Code, its own prices, destinations and lead times.

## Registering an FCL service

Use the existing authenticated `POST /v1/supplier-shippings` endpoint to create a separate FCL record.
Use `PATCH /v1/supplier-shippings/{id}` to edit a record and `GET /v1/supplier-shippings` to inspect records.
Replace the illustrative name, code, prices and destination below with the actual service details:

```json
{
  "shippingMethod": "SEA",
  "shippingMode": "FCL",
  "shippingName": "Example closed-container service",
  "carCode": "TZ12345",
  "originCountryCode": "CN",
  "currency": "THB",
  "baseCost": 0,
  "leadTimeDayMin": 15,
  "leadTimeDayMax": 20,
  "destinations": [
    {
      "destinationName": "Example destination",
      "countryCode": "TH",
      "fullAddress": "Actual destination address",
      "sortOrder": 1
    }
  ]
}
```

SEA/STANDARD records require a TB code; SEA/FCL records require a TZ code.
Codes are trimmed and normalized to uppercase. LAND supports STANDARD mode only.
New codes are supplied by the service provider; the application does not derive a TZ number from an existing TB number.

The PO creation page filters by both method and mode. If no FCL record is available, the user must register one before creating an FCL PO.
The backend rejects a Supplier Shipping that does not match the requested PO shipping method.
FCL PO requests must include a detailed `shippingMethodSnapshot`, such as `SEA_FCL_20GP`.
Omitting the method retains the legacy STANDARD behavior and cannot select an FCL master.
Quotation code lookup also uses both method and mode. ALL continues to select LAND/STANDARD and SEA/STANDARD.

## Migration and document history

`20261006-002-add-supplier-shipping-mode-and-po-car-code-snapshot.yaml` adds the mode column with default STANDARD,
adds `purchase_order.car_code_snapshot`, and fills old PO snapshots from the current linked Supplier Shipping.
It does not create FCL masters because actual TZ codes and service details must be supplied.

New POs copy the selected master Car Code into the snapshot. PO details and original/copy PDF/JPG exports use that snapshot.
Subsequent master edits do not change the PO Car Code. Existing null codes stay null rather than falling back to a later master value.
Backfilled old POs preserve the master value at migration time; earlier master changes cannot be reconstructed by this migration.
