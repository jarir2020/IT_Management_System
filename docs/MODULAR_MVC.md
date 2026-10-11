# Modular MVC Guide

The project groups code by feature. When following a user action, start in that feature's AngularJS route and view, then trace its controller and API model to the Spring MVC controller and domain service.

## Backend modules

Java modules live in `backend/src/main/java/com/itmanagement/servicedesk/modules/`:

```text
modules/
├── auth/{controller,model,routes}/
├── users/{controller,model,routes,repository,service}/
├── departments/{controller,model,routes,repository,service}/
├── access/{controller,model,routes,repository,service}/
├── tickets/{controller,model,routes,repository,service}/
├── assets/{controller,model,routes,repository,service}/
├── requests/{controller,model,routes,repository,service}/
├── reports/{controller,model,routes,service}/
└── shared/
```

- `controller/` maps HTTP requests, validates input, applies endpoint permissions, and delegates to services.
- `routes/` holds that module's API path constants. Spring MVC mappings stay beside the controller that handles them.
- `model/` holds persistence entities, enums, and request/response DTOs.
- `repository/` holds database queries used by the module's services.
- `service/` holds domain rules and coordinates persistence and other module dependencies.
- `shared/` is for code that is genuinely used across feature boundaries.

For example, Tickets owns `TicketController`, `TicketService`, ticket entities and DTOs, ticket repositories, and `TicketRoutes`. Assets owns its inventory, assignment, maintenance, and document records. Requests owns the equipment request state machine, approval and procurement actions, and the link from an allocation to its source request. Both modules reference shared user accounts while enforcing their own access rules.

## AngularJS modules

Frontend feature modules live in `frontend/src/modules/`:

```text
modules/
├── auth/{controllers.js,model.js,routes.js,views/}
├── users/{controllers.js,model.js,routes.js,views/}
├── departments/{controllers.js,model.js,routes.js,views/}
├── access/{controllers.js,model.js,routes.js,views/}
├── tickets/{controllers.js,model.js,routes.js,views/}
├── assets/{controllers.js,model.js,routes.js,views/}
├── requests/{controllers.js,model.js,routes.js,views/}
├── reports/{controllers.js,model.js,routes.js,views/}
├── workspace/{controllers.js,routes.js,views/}
├── dashboard/{controllers.js,routes.js,views/}
├── registry.js
├── index.js
└── routes.js
```

- Each feature's `routes.js` registers its UI-Router states and chooses its template and controller.
- `controllers.js` owns view behavior and form actions.
- `model.js` owns that feature's `$http` calls and response handling.
- `views/` contains that feature's HTML templates.
- `registry.js` declares feature AngularJS modules. The small root `app` composes them and contains only application-wide route guards.

## Request flow

```text
Feature view
  → UI-Router feature route
  → AngularJS feature controller
  → feature model ($http)
  → Spring MVC feature controller
  → feature service
  → feature repository and model
  → response DTO
  → feature view
```

For a ticket attachment or asset document, that feature's AngularJS model sends multipart form data to its Spring controller. The module service checks record access, stores a private file, and records its metadata. Downloads pass through the same access check; stored files are never exposed as static web content.

The Assets API is rooted at `/api/v1/assets`. Its controller exposes inventory list/detail/create/update, assignment and return, maintenance start/status changes, document upload/download/removal, and retirement. Employees receive only assets currently assigned to their account; asset managers and administrators can inspect the inventory. Retired records remain available in history.

The Equipment Requests API is rooted at `/api/v1/requests`. It supports submission and requester cancellation, department-scoped approval/rejection, stock-unavailable routing, purchase recording, receipt, and inventory allocation. A request-based assignment is stored in the asset assignment history with its equipment request ID.

Reports and Imports owns `/api/v1/reports`, PDF/Excel generation, ZIP bundles, and workbook parsing. Its service reads feature repositories for report data and delegates imported inventory rows to the Assets service so duplicate and normalization rules stay in one place.
