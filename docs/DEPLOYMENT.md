# Test and deployment guide

## Backend tests

Run the repeatable unit and integration suite from the backend directory:

```sh
cd backend
mvn test
```

Unit tests cover priority SLA deadlines, ticket visibility and workflow rules, report permission checks, and row-level asset workbook validation. `DatabaseIntegrationTest` starts MySQL 8.4 with Testcontainers, applies every Flyway migration, checks seeded role permissions, and exercises the audit endpoint with and without its required permission.

Docker must be reachable by the current user to run the MySQL integration checks. The Testcontainers annotation skips that integration class when no Docker daemon is available; CI runs it on a Docker-enabled GitHub Actions runner. A skip is not evidence that the MySQL integration tests passed.

The GitHub Actions workflow runs `mvn verify`, the frontend production build, production Compose validation, and both image builds for pushes and pull requests to `main`.

## Production deployment

The deployment uses one public frontend container. Nginx serves the AngularJS files and forwards `/api/` and `/ws` to the Spring API. MySQL is on a private data network. Neither MySQL nor the API publishes a host port. Docker named volumes retain the database and uploaded ticket/asset files across container rebuilds.

1. Copy `.env.production.example` to `.env.production` and replace every placeholder. Keep the file readable only by the deployment account (`chmod 600 .env.production`). Do not commit it.
2. Set `PUBLIC_APP_ORIGIN` to the exact browser origin, including `https://` and without a trailing slash. Set a unique database user password, root password, and initial administrator password. Configure SMTP only if ticket email is required.
3. Set `APP_HTTP_PORT` to an unused host port. The default is `8088`.
4. Configure a TLS ingress or load balancer for the public hostname. Forward HTTPS traffic to `APP_HTTP_PORT`, preserve the original `Origin` header, and allow WebSocket upgrades (`Upgrade` and `Connection` headers) for `/ws`. The secure session and CSRF cookies require HTTPS in the browser.
5. Validate and start the stack:

```sh
docker compose --env-file .env.production -f compose.production.yaml config --quiet
docker compose --env-file .env.production -f compose.production.yaml up --build -d
docker compose --env-file .env.production -f compose.production.yaml ps
```

Browse the public HTTPS hostname. For a direct host check, request `http://127.0.0.1:8088/healthz`; Nginx proxies this to the backend Actuator health endpoint. Check service logs with `docker compose --env-file .env.production -f compose.production.yaml logs --tail=100 backend frontend mysql`.

Set both bootstrap administrator values on the first startup. After signing in and confirming the account, remove `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` from `.env.production`, then recreate the backend. The database account remains; the application does not overwrite an existing administrator.

### Persistent data and backups

The named volumes are `itsm_production_mysql` and `itsm_production_uploads`. Back up the database and uploads as a matching pair, store the archives encrypted away from the deployment host, and periodically verify restoration in a separate environment. Do not put backup files in Git.

Example backup commands:

```sh
mkdir -p backups
chmod 700 backups
BACKUP_STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
docker compose --env-file .env.production -f compose.production.yaml exec -T mysql sh -ec 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump --single-transaction --user="$MYSQL_USER" "$MYSQL_DATABASE"' > "backups/mysql-$BACKUP_STAMP.sql"
docker compose --env-file .env.production -f compose.production.yaml exec -T backend tar -C /var/itsm/uploads -czf - . > "backups/uploads-$BACKUP_STAMP.tar.gz"
```

To restore, stop the backend first so it cannot write while data is replaced. Restore the SQL into the existing application database, then restore the uploads archive into the named volume using a one-off backend container:

```sh
docker compose --env-file .env.production -f compose.production.yaml stop backend
docker compose --env-file .env.production -f compose.production.yaml exec -T mysql sh -ec 'MYSQL_PWD="$MYSQL_PASSWORD" mysql --user="$MYSQL_USER" "$MYSQL_DATABASE"' < backups/mysql-YYYYMMDDTHHMMSSZ.sql
docker compose --env-file .env.production -f compose.production.yaml run --rm --no-deps -T --entrypoint sh backend -ec 'tar -C /var/itsm/uploads -xzf -' < backups/uploads-YYYYMMDDTHHMMSSZ.tar.gz
docker compose --env-file .env.production -f compose.production.yaml start backend
```

Use matching archive names from the same backup run. Confirm `/healthz`, sign-in, a representative ticket attachment, and an asset document after restoring.

## Release and rollback

Before a release, review CI, make a database/upload backup, and keep the exact source revision or built image tags. Apply releases through the production Compose file so Flyway migrations run on startup. Do not edit applied migration files. For rollback, use a code version known to be compatible with the current schema; if a migration cannot be rolled back safely, restore the pre-release database and uploads as a pair.

For a production change, build and inspect the images before updating the running service:

```sh
docker compose --env-file .env.production -f compose.production.yaml build
docker compose --env-file .env.production -f compose.production.yaml up -d
docker compose --env-file .env.production -f compose.production.yaml ps
```

Production secrets belong in a host-only environment file with restricted permissions or a deployment secret manager. The checked-in example contains placeholders only.
