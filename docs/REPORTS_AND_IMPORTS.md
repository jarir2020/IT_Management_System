# Reports and Asset Imports

## Report access

Every export endpoint checks database-backed report permissions:

| Permission | Built-in roles | Scope |
| --- | --- | --- |
| `reports:assets` | Administrator, IT Asset Manager | Full asset inventory |
| `reports:tickets` | Administrator, Support Agent | Service desk ticket queue |
| `reports:requests` | Administrator, IT Asset Manager, Procurement/Finance | Requests across departments |
| `reports:requests_department` | Department Manager | Requests submitted by members of their department |
| `reports:assets_import` | Administrator, IT Asset Manager | Asset workbook import and template |

The Reports page appears only for users with `reports:read`. A ZIP bundle contains PDF and Excel files only for report types the signed-in user can access.

## Exports

The API endpoints are:

- `GET /api/v1/reports/assets.xlsx` and `/assets.pdf`
- `GET /api/v1/reports/tickets.xlsx` and `/tickets.pdf`
- `GET /api/v1/reports/requests.xlsx` and `/requests.pdf`
- `GET /api/v1/reports/bundle.zip`

Excel files have a frozen header row and filters. Reports include identifiers, statuses, people, and key operational dates; ticket descriptions, request justifications, uploaded files, and credentials are excluded.

## Asset workbook import

Download `/api/v1/reports/assets/import-template.xlsx` from the Reports page. The first worksheet must include these column headers; the headers are case-insensitive and spaces or hyphens are accepted in place of underscores.

| Column | Required | Notes |
| --- | --- | --- |
| `asset_tag` | Yes | 2–48 letters, digits, dashes, or underscores; normalized to uppercase |
| `name` | Yes | Up to 140 characters |
| `category` | Yes | `LAPTOP`, `DESKTOP`, `MONITOR`, `PRINTER`, `SOFTWARE_LICENSE`, or `OTHER` |
| `description` | No | Up to 5,000 characters |
| `manufacturer` | No | Up to 100 characters |
| `model_number` | No | Up to 100 characters |
| `serial_number` | No | Up to 100 characters; must be unique |
| `purchase_date` | No | `yyyy-MM-dd`; cannot be in the future |
| `purchase_cost` | No | Non-negative decimal with at most two fractional digits |
| `warranty_expires_on` | No | `yyyy-MM-dd` |
| `useful_life_months` | No | Whole number from 1 to 1,200; used for straight-line depreciation |
| `salvage_value` | No | Non-negative decimal that cannot exceed purchase cost |

The asset Excel report includes recorded useful life and salvage value, accumulated depreciation, net book value, and total recorded maintenance costs. Depreciation uses `(purchase cost - salvage value) × min(elapsed months, useful life months) / useful life months`. It is blank when purchase cost, purchase date, or useful life is missing. Maintenance expenses sum the costs recorded on the asset's maintenance history.

Imports accept `.xlsx` workbooks up to 10 MB and 1,000 data rows. Rows are committed independently: valid rows are created, while invalid or duplicate rows are skipped and returned with their spreadsheet row number and reason. Formula cells are rejected. The import endpoint is `POST /api/v1/reports/assets/import` with multipart field `file`.
