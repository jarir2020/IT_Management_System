# IT Service Desk and Asset Management System

Build a web application covering all 13 common web development tasks for IT support, inventory, equipment requests, approvals, reporting, and administration.

## Core modules

### 1. IT Service Desk

- Ticket CRUD, priorities, categories, and attachments
- Assign tickets to support agents
- Status workflow: Open → In Progress → Resolved → Closed
- Internal notes and employee-agent chat

### 2. Asset Management

- Manage laptops, desktops, monitors, printers, and software licenses
- Assign assets to employees
- Upload purchase receipts and warranty documents
- Track repairs, returns, warranties, and asset history

### 3. Requests and Approvals

- Employees request equipment
- Department managers approve requests
- IT staff allocate available assets
- Procurement handles purchases when stock is unavailable

### 4. Reports and Administration

- PDF/Excel reports and bulk imports
- Asset depreciation and maintenance expenses
- Ticket resolution time and SLA tracking
- Email notifications, access control, and audit logs

## User roles

| Role | Responsibilities |
| --- | --- |
| Admin | Users, roles, permissions, settings, and audit logs |
| Employee | Create tickets, request assets, and view assigned assets |
| Support Agent | Handle tickets, troubleshoot, and resolve issues |
| IT Asset Manager | Inventory, assignments, repairs, and returns |
| Department Manager | Approve equipment and purchase requests |
| Procurement/Finance | Purchase orders, invoices, and expenses |

One user can hold multiple roles when permitted.

## Technology and package choices

- **Backend:** Java with the current stable, supported Spring Boot 4.x release; Maven for dependency management
- **Frontend:** AngularJS 1.x single-page application
- **Database:** MySQL
- **API:** Spring MVC REST endpoints under `/api/v1`; use `spring-boot-starter-webmvc` and serve the AngularJS application and API from one origin where practical
- **Persistence and database:** `spring-boot-starter-data-jpa` with Hibernate, `com.mysql:mysql-connector-j`, and Flyway migrations via `spring-boot-starter-flyway`
- **Authentication:** Spring Security with server-side sessions, secure cookies, and CSRF protection for the browser application
- **Authorization/RBAC:** `spring-boot-starter-security`, Spring Security authorities, database-backed roles and permissions, and method-level checks with `@EnableMethodSecurity`; do not use Laravel Sanctum or Spatie
- **Validation and monitoring:** `spring-boot-starter-validation` and `spring-boot-starter-actuator`, with sensitive Actuator endpoints restricted
- **Reports and imports:** Apache POI (`org.apache.poi:poi-ooxml`) for Excel; Apache PDFBox (`org.apache.pdfbox:pdfbox`) for PDF generation; `spring-boot-starter-batch` for large or repeatable imports
- **Email:** `spring-boot-starter-mail`
- **Live chat:** `spring-boot-starter-websocket` with STOMP; persist chat messages and enforce ticket access rules
- **Background work:** Spring `@Async` for small notification tasks; Spring Batch for bulk jobs; use Spring scheduling or Quartz for recurring SLA work when needed
- **Backend tests:** `spring-boot-starter-test`, `spring-security-test`, and Testcontainers with MySQL for integration tests
- **Frontend packages:** `angular` 1.8.x, `@uirouter/angularjs` for routing, `angular-messages` for form validation, and `angular-mocks` with Jasmine/Karma for unit tests
- **Frontend build:** npm with Vite for development and production bundling; use AngularJS `$http` and browser `FormData` for API and file uploads

Use Spring Boot's dependency management for compatible Spring and third-party versions. Keep database connection details in environment variables or an ignored local configuration file. Provide a local MySQL database through Docker Compose; do not put root credentials in source control.

**AngularJS lifecycle note:** AngularJS support ended in January 2022. This plan keeps AngularJS as requested; pin and review dependencies, avoid unmaintained add-ons where possible, and record a future migration path.

## Development order

1. Authentication, RBAC, users, and departments.
2. Ticket CRUD, support assignment, status workflow, public comments, internal notes, and private attachments in the self-contained Tickets MVC module.
3. Asset inventory, employee assignments, maintenance, and returns.
4. Equipment requests and multi-level approvals.
5. PDF/Excel reports, imports, exports, and ZIP downloads.
6. Email, live chat, SLA calculations, and audit logs.
7. Automated tests, deployment configuration, and deployment documentation.

## Implementation status

| Phase | Status | Delivered scope |
| --- | --- | --- |
| 1. Authentication and administration | Complete | Session login/logout, database-backed roles and permissions, users, and departments. |
| 2. Ticketing | Complete | Ticket CRUD, support assignment, Open → In Progress → Resolved → Closed workflow, public comments, support-only internal notes, and access-checked private attachments. |
| 3. Asset management | Complete | Inventory CRUD, assignment and return history, maintenance records, and private purchase/warranty documents. |
| 4. Equipment requests and approvals | Complete | Employee requests, department-scoped approval, IT inventory allocation, and procurement when stock is unavailable. |
| 5. Reports and data exchange | Complete | Role-scoped PDF/Excel reports, asset workbook import, maintenance expense totals, straight-line depreciation, and permission-scoped ZIP bundles. |
| 6. Operations | Planned | Email, live chat, SLA calculations, and audit logs. |
| 7. Quality and release | Planned | Automated tests, deployment configuration, and deployment documentation. |

Phase 2 uses the module layout below for both Java and AngularJS. Future feature phases should keep their controllers, models, routes, repositories/services where applicable, and views inside their own module directories.

### Phase 3 implementation scope

- Track each inventory item by a unique asset tag, category, description/specifications, vendor/model/serial details, purchase date and cost, and warranty end date.
- Support asset states `AVAILABLE`, `ASSIGNED`, `UNDER_MAINTENANCE`, and `RETIRED`.
- Give asset managers an inventory list, filters, asset detail, create/edit, and lifecycle actions. Employees can view only assets assigned to their own account.
- Record assignment and return events with employee, actor, timestamps, and condition notes. Preserve past assignments as history.
- Record maintenance/repair work with status, dates, notes, and cost; returning an item to service makes it available unless it remains assigned.
- Store purchase receipts and warranty documents outside the public web root, associate each file with its asset and document type, and check asset access on upload, download, and removal.
- Add additive Flyway migrations and database-backed permissions for asset read, own-asset read, create/update, assignment, maintenance, document, and delete actions.

### Phase 4 implementation scope

- Let employees request a device by category, item name, and business justification; show their request history and allow cancellation while awaiting approval.
- Route each request to the requester's department manager. Managers can approve or reject requests from their own department, cannot approve their own request, and must provide a decision note when rejecting. Administrators can review requests across departments.
- Give IT asset managers a cross-department request queue. They can allocate a matching available asset to the requester or record that stock is unavailable and send the approved request to procurement.
- Let Procurement/Finance record vendor, purchase reference, cost, expected delivery date, and notes, then mark the purchase received. IT asset managers register received inventory in Assets and allocate it to the request.
- Keep the request lifecycle explicit (`PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `PENDING_PROCUREMENT`, `PURCHASING`, `READY_FOR_ALLOCATION`, `FULFILLED`, and `CANCELLED`) and retain requester, approver, procurement, allocation, and timestamp details.
- Associate request-based asset assignments with their equipment request, and use database-backed permissions plus service-level ownership and department checks for every action.
- Keep the complete request workflow inside backend and AngularJS `requests` modules, each with its own routes, controller, model, and views; document the module in the modular MVC guide.

### Phase 5 implementation scope

- Provide role-gated PDF and Excel reports for asset inventory, service tickets, and equipment requests. ZIP downloads include the report types the signed-in user's role permits.
- Include asset maintenance expense totals and straight-line depreciation in inventory reporting. Store useful life and salvage value per asset so the exported book values use recorded assumptions instead of a hard-coded lifespan.
- Provide an asset `.xlsx` import template and a bulk import action. Require asset tag, name, and category; accept optional manufacturer, model, serial, purchase date/cost, warranty date, description, useful life months, and salvage value columns. Validate each row, cap imports at 1,000 rows and 10 MB, and return imported/skipped counts with row-level errors.
- Route imported assets through the Assets module service so bulk import uses the same normalization and duplicate checks as normal inventory entry.
- Keep report endpoints and the AngularJS report screen in their own `reports` MVC module, and assign report/import permissions to the relevant built-in roles.
- Add simple local start scripts for MySQL/API and the AngularJS dev server, and document the browser URL and first-run bootstrap login setup.

## Key design decision

Keep ticket management and asset management as separate modules connected through equipment requests and maintenance tickets. This gives the system realistic relationships while keeping the first implementation manageable.

## Modular MVC architecture

Organize each full-stack feature as a module so its HTTP endpoints, business logic, data model, and screens can be followed together.

- Backend modules live under `backend/src/main/java/com/itmanagement/servicedesk/modules/<module>/` and own `controller/`, `model/`, `routes/`, `repository/`, and `service/` packages.
- A module's `routes/` package defines its API path constants. Its Spring MVC controller maps those paths, validates requests, checks permissions, and delegates business rules to its service.
- A module's `model/` package contains its entities, enums, and request/response DTOs. Repositories handle persistence queries; services enforce domain rules.
- AngularJS modules live under `frontend/src/modules/<module>/` and own `controllers.js`, `model.js`, `routes.js`, and `views/` templates. The root app only registers feature modules and cross-cutting authentication guards.
- Shared code stays in `modules/shared` only when more than one feature genuinely uses it. Ticket workflows and ticket-specific UI stay inside the Tickets module.
- For REST requests, backend MVC returns JSON DTOs; AngularJS routes select the module's HTML view and controller.

Request flow: `AngularJS route → module controller → module model/API client → Spring MVC route/controller → module service → repository/model → JSON DTO → module view`.

## Official references

- [Spring Boot reference: starters and build systems](https://docs.spring.io/spring-boot/reference/using/build-systems.html)
- [Spring Security reference: servlet security and method security](https://docs.spring.io/spring-boot/reference/web/spring-security.html)
- [Spring Security reference: OAuth2 support, if token-based API access is added later](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)
- [AngularJS download and end-of-support notice](https://docs.angularjs.org/misc/downloading)
- [UI-Router migration guide for AngularJS](https://ui-router.github.io/guide/ng1/migrate-to-1_0)
