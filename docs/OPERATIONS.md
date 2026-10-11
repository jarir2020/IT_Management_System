# Operations

## Email notifications

Ticket email notifications run asynchronously after the database transaction commits. They are disabled by default, so local development does not require an SMTP server. To enable them, set these values in the ignored `.env` file:

| Variable | Purpose |
| --- | --- |
| `MAIL_ENABLED` | Set to `true` to enable ticket emails |
| `MAIL_HOST`, `MAIL_PORT` | SMTP server and port |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP credentials, if required by the provider |
| `MAIL_FROM` | Sender address accepted by the provider |
| `MAIL_SMTP_AUTH`, `MAIL_STARTTLS_ENABLE` | SMTP authentication and STARTTLS settings |
| `SUPPORT_EMAIL` | Optional support mailbox notified when a ticket is created |

Notifications cover ticket creation, assignment, status changes, and public comments. Internal notes and chat message contents are never emailed. Failed sends are logged without logging SMTP credentials or message contents.

## Live ticket chat

The Tickets module exposes recent chat history at `GET /api/v1/tickets/{ticketId}/chat` and STOMP over the authenticated `/ws` WebSocket endpoint. Clients send to `/app/tickets/{ticketId}/chat` and subscribe to `/topic/tickets/{ticketId}/chat`. The server stores every accepted message, returns the latest 100 in chronological order, and authorizes both sends and subscriptions using the same ticket ownership/support visibility rules as ticket details. Closed tickets remain readable but cannot receive messages.

The local Vite proxy accepts the development origins `http://127.0.0.1:5173` and `http://localhost:5173`. Set `WEBSOCKET_ALLOWED_ORIGINS` to the exact browser origins used by a deployment. The built-in message broker is in memory: persisted history survives restarts, but multiple API instances need a shared broker such as RabbitMQ for cross-instance live delivery.

## Service level targets

Targets count calendar time from ticket creation. First response is recorded when support moves a ticket to In Progress or posts its first public update. Internal notes do not satisfy the response target. Resolution uses the ticket's `resolved_at` timestamp.

| Priority | First response | Resolution |
| --- | ---: | ---: |
| Urgent | 60 minutes | 4 hours |
| High | 4 hours | 24 hours |
| Normal | 8 hours | 72 hours |
| Low | 24 hours | 120 hours |

Targets are configured in `app.sla.targets` in `backend/src/main/resources/application.yml`; set the corresponding environment properties in a deployment to override them. The ticket list and detail view show current due times and breach state.

## Audit trail

The `audit:read` permission is granted to the built-in Administrator role. The Audit trail screen shows the latest 300 events; `GET /api/v1/audit?limit=...` supports up to 500. Tracked actions include ticket workflow and chat activity, asset inventory/assignment/maintenance/document changes, equipment request decisions and procurement, and user account create/update/enable/disable actions. Audit summaries avoid passwords, message bodies, notes, and uploaded file contents. Events are append-only through the application and remain after the referenced record is deleted.
