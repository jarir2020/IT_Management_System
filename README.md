# IT_Management_System

IT Service Desk and Asset Management application.

The application uses a Spring Boot REST API and an AngularJS single-page application. Authentication, role-based access, user accounts, departments, and the ticketing workflow are implemented.

## Local setup

1. Copy `.env.example` to `.env` and replace each example password with a strong local value.
2. Start MySQL with `docker compose up -d mysql`.
3. In a terminal, load the local variables with `set -a; source .env; set +a`.
4. Start the API with `cd backend && mvn spring-boot:run`.
5. In another terminal, start the client with `cd frontend && npm install && npm run dev`.
6. Open `http://localhost:5173`. Sign in with the bootstrap admin email and password from `.env`.

On first startup, Flyway creates the schema and the application seeds its built-in roles and permissions. It creates the initial administrator only when both bootstrap admin variables are set. Existing administrator credentials are not overwritten on later starts.

Ticket attachments and asset documents are stored outside the web root in `backend/var/uploads/tickets` and `backend/var/uploads/assets` by default. Set `TICKET_UPLOAD_DIR` and `ASSET_UPLOAD_DIR` to persistent, private directories in production and include them in the deployment backup policy.

The Vite development server proxies `/api` requests to the backend, so session cookies and CSRF requests remain same-origin in development. For production, put the frontend and API behind one HTTPS origin and set `SESSION_COOKIE_SECURE=true`.

## Frontend security note

AngularJS 1.8.3 is end of life. The current dependency tree also reports two advisories with `npm audit`; review them before exposing a production build publicly. Keep application templates in source control and never compile user-provided content as an AngularJS template. A future phase should evaluate a supported migration or a security-maintained replacement.

## Current phase

Phases 1–3 are complete: login/logout, database-backed roles and permissions, users and departments, ticket workflows and private attachments, and asset inventory with assignment/return history, maintenance records, and private purchase/warranty documents. Phase 4 will add equipment requests and approvals; reports, chat, notifications, and audit logs follow later.

## Module structure

Features keep their own controllers, models, route definitions, and views. Shared application code is limited to cross-feature concerns such as session security and common response mapping. See [the modular MVC guide](docs/MODULAR_MVC.md) for the folder map and request flow.
