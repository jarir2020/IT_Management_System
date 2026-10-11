# Spring Boot and AngularJS: Theory Notes for Interviews

This guide introduces the ideas used by this project, from basic vocabulary to common interview topics. It connects Spring Boot to Laravel and AngularJS to React so you can use concepts you already know as a starting point.

## The project in one picture

```text
Browser
  └── AngularJS app served by Vite at :5173
        ├── /api requests ──> Spring MVC API on embedded Tomcat at :8080
        └── /ws chat ───────> Spring WebSocket endpoint
                                  │
                                  └── Spring Data JPA / Hibernate
                                          │
                                          └── MySQL
```

For a ticket request, the usual path is:

```text
ticket view → UI-Router state → AngularJS controller → TicketModel ($http)
→ Spring Security filters → Spring MVC controller → TicketService
→ TicketRepository → JPA/Hibernate → MySQL
→ response DTO → JSON → AngularJS view
```

The frontend and API are separate development servers. Vite proxies `/api` and `/ws` to Spring Boot, which keeps browser requests on the same origin during development.

## Quick Laravel-to-Spring comparison

These are useful parallels, not exact one-to-one replacements.

| Laravel idea | Spring Boot idea in this project |
| --- | --- |
| Composer | Maven and `pom.xml` |
| `artisan serve` or `php -S` | Embedded Tomcat started by Spring Boot |
| `bootstrap/app.php` and framework startup | `ItServiceDeskApplication` and `SpringApplication.run` |
| Service container | Spring `ApplicationContext` and its beans |
| Service provider registration/boot | Component scanning, auto-configuration, `@Configuration`, and `@Bean` |
| Route declaration | `@RequestMapping`, `@GetMapping`, `@PostMapping`, etc. |
| HTTP Kernel middleware | Servlet filters; Spring Security is implemented in a filter chain |
| Form Request validation | Request DTO plus Jakarta Bean Validation |
| Service class | Spring-managed `@Service` class |
| Eloquent model/query | JPA entity plus repository; related, but not identical |
| API Resource / response shaping | Response DTO serialized to JSON |
| Laravel migration | Flyway SQL migration |
| `DB::transaction` | `@Transactional` |
| PHPUnit feature test | Spring test, MockMvc, and Testcontainers |

Spring Boot hides much of the server setup, but the same broad web-app stages still exist: start the application, pass requests through middleware/security, route them to a controller, run business logic, access storage, and return a response.

# Part I — Java and Spring Boot

## Level 1: Java vocabulary

### 1. JVM, bytecode, and Java

Java source files (`.java`) are compiled into JVM bytecode (`.class`). The Java Virtual Machine runs that bytecode. Maven can compile the code, resolve dependencies, run tests, and launch the application.

This differs from PHP’s usual request model: PHP code is commonly interpreted by a PHP runtime for each request, while this Spring Boot app starts a long-lived Java process and keeps the application context alive while it serves requests.

### 2. What is a package?

A Java package is a namespace for classes. It prevents unrelated classes with the same short name from colliding and gives the code an organizational boundary. Package names are usually lowercase and reverse a domain-like name. Dots in a package name map to directories:

```java
package com.itmanagement.servicedesk.modules.tickets.controller;
```

maps to:

```text
com/itmanagement/servicedesk/modules/tickets/controller/
```

The long path is partly Java convention (`com.itmanagement.servicedesk`) and partly this project's feature/layer layout (`modules/tickets/controller`). It is not a requirement that every project use this exact depth.

### 3. Class, object, interface, and record

- A **class** defines data and behavior. An **object** is an instance of a class.
- An **interface** describes operations a class must provide. Spring Data can create repository implementations from interfaces.
- A **record** is a concise Java data carrier with immutable components, generated accessors, and value-style methods. It is useful for DTOs.
- An **annotation** such as `@Service` or `@Entity` attaches metadata that frameworks use at runtime or build time.

### 4. Maven and dependencies

Maven is Java’s build and dependency tool here. `pom.xml` declares the project, dependencies, Java version, and build plugins. Spring Boot “starters” are dependency bundles: for example, the Web MVC starter brings in the pieces needed to build HTTP endpoints. The Spring Boot parent manages compatible dependency versions.

Laravel analogy: Composer installs PHP packages and manages `composer.json`; Maven does that for Java while also defining a standard build lifecycle (`compile`, `test`, `package`, and others).

## Level 2: Spring framework foundations

### 5. Spring Framework versus Spring Boot

**Spring Framework** provides the core container, dependency injection, web MVC, transactions, and other libraries. **Spring Boot** makes a Spring application easier to configure and run. It supplies sensible auto-configuration, starter dependencies, externalized configuration, and an embedded web server.

Boot does not replace Spring MVC or Spring Security. It configures and launches them based on dependencies and settings.

### 6. IoC, dependency injection, and beans

**Inversion of Control (IoC)** means application objects do not construct and manage every dependency themselves. Spring creates and connects application objects. **Dependency injection (DI)** is how Spring provides those dependencies.

An object managed by Spring is called a **bean**. Beans live in the Spring `ApplicationContext`, which is the application’s IoC container.

```java
@Service
public class TicketService {
    private final TicketRepository tickets;

    public TicketService(TicketRepository tickets) {
        this.tickets = tickets;
    }
}
```

Spring finds `TicketService`, finds a repository bean that implements `TicketRepository`, and supplies it to the constructor. Constructor injection makes required dependencies visible and makes the class easier to test.

Laravel analogy: the Spring container has a role similar to Laravel’s service container. Laravel service providers register or boot services; Spring commonly discovers classes using annotations, configures explicit objects with `@Configuration`/`@Bean`, and applies Boot auto-configuration. There is no single Spring class that exactly equals every Laravel service provider.

Common bean annotations:

- `@Component`: general Spring-managed class.
- `@Service`: application/domain service; also a component.
- `@Repository`: persistence class or repository marker; also participates in persistence exception translation.
- `@Controller` / `@RestController`: web controller.
- `@Configuration` and `@Bean`: explicit bean configuration.

### 7. Bootstrapping and auto-configuration

The application starts from `ItServiceDeskApplication.main`:

```java
@SpringBootApplication
public class ItServiceDeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(ItServiceDeskApplication.class, args);
    }
}
```

`@SpringBootApplication` combines configuration, auto-configuration, and component scanning. Scanning starts from the class’s package (`com.itmanagement.servicedesk`) and includes its child packages, which is why controllers and services are placed beneath it.

Auto-configuration conditionally configures beans when relevant libraries and settings are present. It is not arbitrary magic: dependencies, properties, and conditions determine what Boot configures, and application configuration can customize it.

### 8. Embedded Tomcat and the backend process

Spring Boot starts an embedded Tomcat server inside the Java process. The project’s `./start-backend.sh` first loads `.env`, starts MySQL through Docker Compose, waits for its health check, and then runs `mvn spring-boot:run`. Spring Boot’s embedded server listens on port `8080`.

`php -S` starts a PHP development server. The closest comparison is that Tomcat also accepts HTTP requests, but it runs as part of the Spring Boot application process and routes them through Spring MVC. No separately installed Tomcat server is needed for local development.

## Level 3: HTTP and MVC

### 9. The Spring request lifecycle

At a high level, an HTTP request goes through:

1. The servlet container (Tomcat) accepts the request.
2. Servlet filters run. Spring Security’s filter chain handles authentication, CSRF, and request authorization here.
3. Spring MVC’s `DispatcherServlet` acts as a front controller and finds the matching handler.
4. The controller validates the request and delegates business work.
5. The service applies domain rules and calls repositories.
6. The result is converted to JSON and returned with an HTTP status.

Laravel’s Kernel/middleware/router/controller sequence is a helpful mental model. Spring’s `DispatcherServlet` is the MVC front controller; the servlet filters run before MVC handler selection.

### 10. Controller, service, repository, and model

- **Controller:** HTTP boundary. Reads path/query/body/authentication data, applies endpoint-level rules, and calls a service.
- **Service:** business decisions and workflow coordination. This is usually the best place for transaction boundaries and record ownership checks.
- **Repository:** persistence queries. Spring Data derives common implementations from interfaces.
- **Entity:** object mapped to a database table by JPA.
- **DTO:** object designed for an API request or response.

Keeping the controller thin makes the domain rules reusable and keeps HTTP details out of persistence logic.

#### How MVC is split in this project

Laravel often renders a Blade view from a controller. This app is a single-page application, so the browser renders the page and Spring Boot provides JSON APIs. The MVC responsibilities are split across the two applications:

| Responsibility | Where it lives here | Example |
| --- | --- | --- |
| Browser view | AngularJS feature `views/` | `frontend/src/modules/tickets/views/list.html` |
| Browser navigation routes | AngularJS feature `routes.js` | UI-Router maps `/tickets` to a template and controller |
| API routes and controller | Spring feature `routes/` and `controller/` | `TicketRoutes` defines the path constant; `TicketController` maps HTTP verbs to methods |
| Data models | Spring feature `model/` | `Ticket` is a JPA entity; `TicketCreateRequest` and `TicketDetailResponse` are DTOs |
| Business workflow | Spring feature `service/` | `TicketService` applies ticket rules and coordinates repositories |
| Database access | Spring feature `repository/` | `TicketRepository` loads and saves ticket entities |

The word **route** therefore has two meanings in the full-stack app: UI-Router selects a browser page, while Spring MVC maps an HTTP API request to a controller method. Spring MVC still uses the MVC request-handling pattern, but its response is usually JSON rather than a server-rendered HTML view. These folders are the project’s chosen organization; Spring does not require every project to use this exact modular layout.

### 11. What is a DTO, and why not return an entity?

A **Data Transfer Object (DTO)** defines the data crossing a boundary, such as JSON sent by a browser. Request DTOs describe accepted input; response DTOs describe what the API returns.

```java
public record TicketCreateRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 10_000) String description,
        @NotNull TicketCategory category,
        @NotNull TicketPriority priority) {
}
```

This request DTO is separate from the `Ticket` JPA entity. That separation lets the API validate and evolve its contract without exposing database relationships, internal fields, password hashes, or lazy-loading behavior.

Laravel analogy: a request DTO with validation is similar in purpose to a validated Form Request; a response DTO is similar to an API Resource. The shape is explicit in Java types rather than being defined only by an array or resource transformer.

### 12. A controller endpoint

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
@PreAuthorize("hasAuthority('tickets:create')")
public TicketDetailResponse create(
        @Valid @RequestBody TicketCreateRequest request,
        @AuthenticationPrincipal UserAccount actor) {
    return tickets.createTicket(request, actor);
}
```

- `@PostMapping` maps HTTP POST to this method.
- `@RequestBody` asks Spring/Jackson to deserialize the JSON body.
- `@Valid` runs Jakarta validation annotations on the DTO.
- `@PreAuthorize` checks an authority before allowing the method.
- `@AuthenticationPrincipal` supplies the signed-in account.
- `@ResponseStatus` selects the success status (`201 Created`).

### 13. REST and HTTP status codes

REST APIs use HTTP methods and resource URLs to describe operations. In this app, the tickets resource is under `/api/v1/tickets`.

- `GET`: retrieve data; usually safe to repeat.
- `POST`: create or trigger an operation.
- `PUT`: replace/update a resource; typically idempotent.
- `PATCH`: partially update a resource.
- `DELETE`: remove a resource.

Common responses include `200 OK`, `201 Created`, `204 No Content`, `400 Bad Request` (invalid input), `401 Unauthorized` (not signed in), `403 Forbidden` (signed in but not permitted), `404 Not Found`, and `409 Conflict` (valid request that conflicts with current state).

### 14. Central error handling

Controllers and services should report meaningful HTTP errors rather than exposing stack traces. A project may use `ResponseStatusException` for simple cases or `@RestControllerAdvice` for a consistent JSON error format across endpoints. Never return internal exception details or secrets to the browser.

## Level 4: Database and transactions

### 15. JPA, Hibernate, entity, and repository

**JPA** is Java’s persistence specification. **Hibernate** is the implementation used by the app. An `@Entity` class maps to a table; a repository exposes operations and queries for entities.

```java
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    // Spring Data supplies common CRUD methods such as findById and save.
}
```

Laravel comparison: an entity plus repository overlaps with Eloquent model/query behavior, but the responsibilities are split more explicitly. A JPA entity is not automatically the API response, and a repository is an abstraction for persistence operations.

### 16. Migrations and schema ownership

Flyway applies numbered SQL migrations in order and records which versions ran. This is similar to Laravel migrations. In this app, Hibernate validates the schema; Flyway owns schema changes. Add a new migration for each change instead of editing one already applied to a database.

### 17. Transactions and `@Transactional`

A database transaction makes a group of operations commit together or roll back together. Spring’s `@Transactional` usually belongs on a service method that represents one business operation:

```java
@Transactional
public TicketDetailResponse createTicket(TicketCreateRequest request, UserAccount actor) {
    Ticket saved = tickets.save(new Ticket(actor, request.title(),
            request.description(), request.category(), request.priority()));
    return toDetail(saved, actor);
}
```

Laravel analogy: it plays a role similar to `DB::transaction(...)`. Spring commonly applies transactions through a proxy around a Spring bean. An advanced interview detail: a method calling another `@Transactional` method on `this` can bypass that proxy, so the inner transaction annotation may not take effect as expected.

### 18. ORM behavior and common pitfalls

- **Lazy loading:** a related entity can be loaded only when accessed. Accessing it after the persistence context closes may fail.
- **N+1 queries:** one query loads a list, then one additional query runs for each row’s relationship. Use deliberate query methods/entity graphs and DTO mapping.
- **Dirty checking:** Hibernate tracks managed entities and writes their changes at transaction commit; an explicit `save` is not required for every managed update.
- **Optimistic locking:** this project’s `Ticket` has `@Version`. An update based on an old version can be rejected instead of silently overwriting another user’s update.

## Level 5: Security and request filters

### 19. Authentication versus authorization

- **Authentication:** who is making the request?
- **Authorization:** may that account perform this action on this record?

The project uses server-side sessions and permission authorities. `@PreAuthorize("hasAuthority('tickets:create')")` is an endpoint-level check. The ticket service also checks record ownership and assignment; a broad permission alone must not grant access to every ticket.

### 20. Middleware, filters, and interceptors

Spring has several request extension points:

- A **Servlet Filter** sees requests before Spring MVC. Spring Security is built as an ordered filter chain. This is the closest general equivalent to Laravel middleware/Kernel middleware.
- A **Spring MVC HandlerInterceptor** runs around a selected MVC handler, after route selection.
- A **controller** handles the matched endpoint.

Use security filters for cross-cutting request security, interceptors for MVC-specific cross-cutting behavior, and controllers for endpoint behavior. Avoid hiding feature business rules in a global filter.

### 21. Sessions, cookies, CSRF, and CORS

This application uses session cookies, so the browser automatically sends the cookie with matching requests. CSRF protection requires a token on state-changing requests because browsers attach cookies automatically. A secure cookie should use `Secure` behind HTTPS, as configured for production.

**CORS** controls whether browser JavaScript can make cross-origin requests. It is not a CSRF defense. During local development, Vite proxies API requests, so the browser sees one origin (`127.0.0.1:5173`) while Vite forwards them to the API (`127.0.0.1:8080`).

### 22. Validation versus authorization

Validation asks whether input is well-formed (`title` is present and within length). Authorization asks whether the current user is allowed to do the operation (has `tickets:create`, owns the ticket, or is its assigned agent). Both are required; passing validation does not mean an action is permitted.

## Level 6: Testing and runtime

### 23. Test layers

- **Unit test:** tests a class’s logic without starting the whole application or a real database.
- **MVC/controller test:** checks HTTP routing, JSON, validation, and security behavior, commonly with MockMvc.
- **Integration test:** starts connected application components. Testcontainers can start a disposable real MySQL database so migrations and SQL behavior are tested against MySQL.

Laravel analogy: PHPUnit/Pest unit and feature tests are similar goals; MockMvc focuses on the Spring MVC boundary, while Testcontainers gives an isolated database service.

### 24. Health and deployment

Spring Boot Actuator exposes operational endpoints such as health and info. In production, expose only the endpoints needed by monitoring and do not reveal detailed environment or database information publicly. Production runs the backend as a container and uses Nginx to serve the frontend and proxy API/WebSocket traffic.

# Part II — AngularJS explained through React

## First: AngularJS is not modern Angular

This project uses **AngularJS 1.8.3**, also called AngularJS or Angular 1.x. It is different from modern Angular (2+), which is built around TypeScript and a different component architecture. AngularJS reached end of official support in 2022, so it is useful here for learning and maintaining this project, but new production systems should evaluate a supported frontend framework.

## Level 1: AngularJS app pieces

### 25. AngularJS module and dependency injection

An AngularJS module groups controllers, factories/services, directives, filters, and configuration. `itDeskApp` is the root module; feature modules such as `itDesk.tickets` are composed into it. This resembles registering feature areas and providers in an application container, not the Java package system.

AngularJS also has dependency injection. It supplies `$http`, `$state`, and app factories to controllers. This is conceptually similar to constructor injection in Spring, although AngularJS commonly annotates dependencies by name.

```js
featureModules.tickets.factory('TicketModel', ['$http', function ($http) {
  const root = '/api/v1/tickets';
  return {
    list: () => $http.get(root).then(response => response.data)
  };
}]);
```

The array form keeps dependency names safe when JavaScript is minified.

### 26. Controller and “controller as”

This project uses `TicketListController as vm`. The controller prepares view state and actions; its `vm` properties are read by the template. Think of it as a view-model object, not exactly a React function component.

```js
featureModules.tickets.controller('TicketListController', ['TicketModel', function (TicketModel) {
  const vm = this;
  vm.tickets = [];
  vm.load = () => TicketModel.list().then(rows => { vm.tickets = rows; });
  vm.load();
}]);
```

```html
<ul>
  <li ng-repeat="ticket in vm.tickets">{{ ticket.title }}</li>
</ul>
```

### 27. Templates and directives

AngularJS templates are HTML with directives and expressions:

- `ng-model`: bind an input to a model property.
- `ng-repeat`: render one element per item.
- `ng-if` / `ng-show`: conditionally display content.
- `ng-submit`: connect form submission to a controller action.
- `ui-view`: render the template selected by UI-Router.

Directives add behavior to DOM elements. Built-in directives are used in templates; custom directives/components can package reusable DOM behavior.

### 28. Services and models

An AngularJS factory returns a reusable object. In this project, feature `model.js` files call the API with `$http`; controllers call that model instead of building URLs and request options themselves. A factory/service can be injected in multiple controllers and is generally a singleton per AngularJS injector.

Laravel analogy: this is similar to using an application service/API client rather than putting HTTP details in a Blade template. The Java backend’s `@Service` is a different layer: one runs in the browser and calls HTTP; the other runs on the server and applies domain rules.

## Level 2: Comparing AngularJS with React

| React concept | AngularJS concept in this app | Important difference |
| --- | --- | --- |
| `useState` | Controller properties such as `vm.form.title`, often bound by `ng-model` | AngularJS uses a digest cycle and often two-way binding; React state updates schedule a render. |
| `useEffect` for loading | Controller calls a method such as `vm.load()` when created | There is no direct `useEffect` equivalent. Be explicit about when data loads and when listeners are cleaned up. |
| Props | Isolated component/directive bindings | This app mostly uses route templates and controller-as; routes are the main view boundary. |
| JSX | HTML templates with AngularJS directives | AngularJS expressions are evaluated by AngularJS; templates are not JSX. |
| `map()` rendering | `ng-repeat` | Both repeat UI from a collection, but AngularJS watches the collection/model. |
| React Router | UI-Router states | UI-Router supports named and nested states; `$state.go(...)` navigates between them. |
| Context/custom hooks | Injected factories/services | Both can share behavior; the mechanisms and lifecycles differ. |
| React rendering/reconciliation | AngularJS digest and watchers | AngularJS does not use React’s virtual DOM reconciliation model. |

A simple controlled React input:

```jsx
function TitleInput() {
  const [title, setTitle] = useState('');
  return <input value={title} onChange={event => setTitle(event.target.value)} />;
}
```

The AngularJS template binds the input to a controller property:

```html
<input ng-model="vm.form.title" />
```

Both keep input and UI state connected. React asks you to update state explicitly; AngularJS `ng-model` connects the DOM input and model through its binding system.

### 29. The digest cycle and `$scope`

AngularJS watches values used by the view. A **digest cycle** checks watched values and updates the DOM when they change. `$scope` is the context where model values and functions are exposed; this project generally uses the `controller as vm` syntax to avoid putting everything directly on `$scope`.

AngularJS-aware services such as `$http` notify AngularJS when asynchronous work completes, so the digest can update the view. A callback from an external library (such as a raw WebSocket/STOMP client) may run outside AngularJS. In that case, `$rootScope.$applyAsync(...)` schedules the change safely. The ticket chat controller uses this when messages arrive.

React analogy: both update the rendered UI after state changes, but the scheduling model differs. AngularJS’s digest/watch system is not the same thing as React’s state setter and render cycle. `$watch` is also not a direct equivalent of `useEffect`.

### 30. Routing and the API request

UI-Router maps a URL/state to a template and controller:

```js
$stateProvider.state('workspace.tickets', {
  url: '/tickets',
  template: ticketListTemplate,
  controller: 'TicketListController as vm'
});
```

The controller calls `TicketModel.list()`. That model uses `$http` to request `/api/v1/tickets`; Vite forwards the request to Spring Boot in development. The API returns JSON, and the controller puts the returned rows on `vm.tickets` for `ng-repeat` to render.

### 31. AngularJS bootstrap

AngularJS must load its root module before bootstrapping it. This project uses asynchronous imports for AngularJS add-ons, so `src/main.js` waits for the modules and then explicitly calls:

```js
angular.bootstrap(document.getElementById('app'), ['itDeskApp']);
```

The HTML therefore does not also use `ng-app="itDeskApp"`. Combining automatic `ng-app` bootstrap with delayed module imports can start the injector before the root module is registered.

# Interview questions with short answers

### Beginner

1. **What is a Java package?** A namespace for related classes; its dotted name maps to folders and helps avoid naming collisions.
2. **What is a DTO?** A typed request/response object that defines data crossing an application boundary.
3. **What is a Spring bean?** An object created or managed by Spring’s `ApplicationContext`.
4. **What does `@RestController` mean?** A Spring MVC controller whose method return values are written to the HTTP response body, commonly as JSON.
5. **What is a repository?** An abstraction for loading and saving persistent data; Spring Data can generate common implementations.

### Intermediate

6. **What do IoC and DI mean?** Spring controls object creation (IoC) and supplies dependencies to those objects (DI).
7. **Why use a service layer?** It keeps business rules and workflows out of HTTP controllers and makes them reusable/testable.
8. **Why use DTOs instead of entities in API methods?** DTOs control validation and the public API shape without exposing persistence internals.
9. **What is the difference between authentication and authorization?** Authentication identifies a user; authorization checks what that user may do.
10. **How does `@Transactional` help?** It surrounds a service operation with a database transaction so changes commit or roll back together.
11. **What is the difference between a filter and an MVC interceptor?** A filter runs in the servlet chain before handler selection; an interceptor wraps an MVC handler after routing.

### Advanced

12. **What does Spring Boot auto-configuration do?** It conditionally creates common beans based on the classpath, properties, and existing application beans.
13. **Why can a lazy relation cause an error or many queries?** Access after a persistence context closes may fail; accessing relations repeatedly may produce N+1 queries.
14. **Why might an internal call ignore `@Transactional`?** Spring transaction behavior is usually proxy-based; self-invocation can bypass the proxy.
15. **What is optimistic locking?** It detects a stale concurrent update using a version field rather than silently overwriting newer data.
16. **What is the AngularJS digest cycle?** A pass that checks watched model values and updates bindings in the DOM.
17. **Is AngularJS `ng-model` the same as React `useState`?** They both connect UI state and rendering, but AngularJS uses bindings/digests while React uses explicit state updates and renders.
18. **Why call `$applyAsync` from a WebSocket callback?** The callback can run outside AngularJS; `$applyAsync` schedules the change inside AngularJS’s update cycle.

## Suggested way to study this project

Trace one ticket from `frontend/src/modules/tickets/views/list.html` to `routes.js`, `controllers.js`, and `model.js`. Then follow the request into `TicketController`, `TicketService`, `TicketRepository`, and `Ticket`. This reveals the framework boundaries more clearly than memorizing annotations in isolation.

For the full folder map and request flow, see [the modular MVC guide](docs/MODULAR_MVC.md). For app startup and local run instructions, see [the README](README.md).
