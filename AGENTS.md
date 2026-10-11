# Repository Guide

## Start here

1. Read `plan.md` and this guide before implementing a phase.
2. Work on one roadmap phase at a time and keep `README.md` and `plan.md` aligned with delivered behavior.
3. Keep feature code inside its module and explain non-obvious domain rules with short comments.

## Stack and layout

- Backend: Java 25, Spring Boot 4, Spring MVC, Spring Security, Spring Data JPA, MySQL, Flyway, and Maven.
- Frontend: AngularJS 1.8, UI-Router, `angular-messages`, and Vite.
- Backend feature modules live at `backend/src/main/java/com/itmanagement/servicedesk/modules/<module>/` and group `controller/`, `model/`, `routes/`, `repository/`, and `service/` as needed.
- Frontend feature modules live at `frontend/src/modules/<module>/` and group `controllers.js`, `model.js`, `routes.js`, and `views/` as needed.
- Keep application-wide composition and security guards small. Put feature workflows in their owning module.
- Read `docs/MODULAR_MVC.md` for the request flow and examples.

## Persistence and security

- Add schema changes as a new numbered Flyway migration. Do not edit an already-applied migration.
- Keep JPA entities inside their owning feature's `model/` package; use request/response DTOs at API boundaries.
- Enforce permissions in Spring Security and enforce record-level ownership/assignment rules in the feature service.
- Keep credentials in `.env` or deployment secrets. Never commit real environment files, passwords, API keys, private keys, or local databases.
- Store uploaded files outside static web roots and serve them only through an authorized controller.

## Local commands

```sh
cp .env.example .env    # replace the example database and administrator passwords
./start-backend.sh     # terminal 1: starts MySQL and the Spring Boot API
./start-frontend.sh    # terminal 2: starts the AngularJS/Vite client
# browse http://127.0.0.1:5173
```

To stop MySQL while retaining its data, run `docker compose stop mysql`. Build the backend with `cd backend && mvn -DskipTests package` and the frontend with `cd frontend && npm run build`. Automated tests are a later roadmap phase; do not describe a build as a test run.

## Repository hygiene

- Update `plan.md` when phase status or scope changes and update `README.md` for user-visible setup or feature changes.
- Keep `.env.example` limited to placeholders and commit only source, documentation, and intentional project configuration.
- Do not commit `backend/target`, any `var/uploads` directory, `frontend/node_modules`, or `frontend/dist`.
