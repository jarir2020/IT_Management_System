# IT_Management_System

IT Service Desk and Asset Management application.

The application uses a Spring Boot REST API and an AngularJS single-page application. Authentication, role-based access, user accounts, departments, ticketing, asset inventory, equipment requests, reports, and operational workflows are implemented.

## Local setup

1. Copy `.env.example` to `.env` and replace the example database and administrator passwords.
2. In a terminal at the repository root, run `./start-backend.sh`. It starts MySQL, waits for its health check, and runs the Spring Boot API.
3. In a second terminal at the repository root, run `./start-frontend.sh`. It installs locked npm packages on first run and starts Vite.
4. Open **http://127.0.0.1:5173** in a browser. Sign in with `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` from `.env`.

Use Ctrl+C in each terminal to stop its server. Stop the database separately with `docker compose stop mysql`; its Docker volume preserves local data. To start either server manually, see [AGENTS.md](AGENTS.md).

On first startup, Flyway creates the schema and the application seeds its built-in roles and permissions. It creates the initial administrator only when both bootstrap admin variables are set. Existing administrator credentials are not overwritten on later starts.

Ticket attachments and asset documents are stored outside the web root in `backend/var/uploads/tickets` and `backend/var/uploads/assets` by default. Set `TICKET_UPLOAD_DIR` and `ASSET_UPLOAD_DIR` to persistent, private directories in production and include them in the deployment backup policy.

The Vite development server proxies `/api` requests to the backend, so session cookies and CSRF requests remain same-origin in development. For production, put the frontend and API behind one HTTPS origin and set `SESSION_COOKIE_SECURE=true`.

## Tests and production deployment

Run backend unit and integration tests with `cd backend && mvn test`. MySQL integration tests use Testcontainers when Docker is available. Build the frontend with `cd frontend && npm ci && npm run build`.

Production Dockerfiles and a private-network deployment Compose file are provided. Copy `.env.production.example` to `.env.production`, replace its placeholders, and follow [the deployment guide](docs/DEPLOYMENT.md) for HTTPS ingress, health checks, backups, restores, and release steps. MySQL and uploaded files use named volumes and are not exposed as host ports.

## Frontend security note

AngularJS 1.8.3 is end of life. The current dependency tree also reports two advisories with `npm audit`; review them before exposing a production build publicly. Keep application templates in source control and never compile user-provided content as an AngularJS template. A future phase should evaluate a supported migration or a security-maintained replacement.

## Current phase

Phases 1–7 are complete: login/logout, database-backed roles and permissions, users and departments, ticket workflows and private attachments, asset inventory and maintenance, equipment requests with department approval and procurement tracking, role-scoped reports and imports, email notifications, persisted live ticket chat, priority-based SLAs, an administrator audit trail, automated unit and MySQL integration tests, CI, production containers, a same-origin reverse proxy, and deployment documentation.

## Equipment requests

Employees submit requests for an equipment category with a business justification. Department managers review requests from their own department and cannot approve their own requests. IT asset managers allocate matching available inventory or route approved requests to Procurement/Finance. Procurement records vendor, reference, cost, and expected delivery, then marks orders received; IT registers the delivered asset in Inventory and completes allocation. Request-linked assignments appear in asset history.

## Reports and imports

Authorized users can export PDF and Excel reports for assets, tickets, and equipment requests, and download a ZIP containing the reports available to their role. IT asset managers can import inventory from `.xlsx`; the importer validates rows individually, uses the same asset rules as manual entry, and returns row-level reasons for skipped records. See [the report and import guide](docs/REPORTS_AND_IMPORTS.md) for permissions and workbook columns.

## Operations

Ticket email notifications are disabled until SMTP values are configured in `.env`. Ticket chat uses authenticated WebSocket/STOMP and retains its message history. SLAs use priority-specific calendar-time targets. Administrators can review action summaries in the Audit trail; message bodies, passwords, and file contents are excluded. See [the operations guide](docs/OPERATIONS.md) for configuration and behavior.

## Module structure

Features keep their own controllers, models, route definitions, and views. Shared application code is limited to cross-feature concerns such as session security and common response mapping. See [the modular MVC guide](docs/MODULAR_MVC.md) for the folder map and request flow.
