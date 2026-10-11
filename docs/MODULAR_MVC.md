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
└── shared/
```

- `controller/` maps HTTP requests, validates input, applies endpoint permissions, and delegates to services.
- `routes/` holds that module's API path constants. Spring MVC mappings stay beside the controller that handles them.
- `model/` holds persistence entities, enums, and request/response DTOs.
- `repository/` holds database queries used by the module's services.
- `service/` holds domain rules and coordinates persistence and other module dependencies.
- `shared/` is for code that is genuinely used across feature boundaries.

For example, Tickets owns `TicketController`, `TicketService`, ticket entities and DTOs, ticket repositories, and `TicketRoutes`. Ticket records use the shared user accounts for requesters and assignees.

## AngularJS modules

Frontend feature modules live in `frontend/src/modules/`:

```text
modules/
├── auth/{controllers.js,model.js,routes.js,views/}
├── users/{controllers.js,model.js,routes.js,views/}
├── departments/{controllers.js,model.js,routes.js,views/}
├── access/{controllers.js,model.js,routes.js,views/}
├── tickets/{controllers.js,model.js,routes.js,views/}
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

For a ticket attachment, the Tickets model sends multipart form data to the Tickets controller. The service checks the ticket access rules, stores a private file, and records its metadata. Downloads pass through the same ticket access check; stored files are never exposed as static web content.
