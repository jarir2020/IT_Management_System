#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$ROOT_DIR/.env"

if [[ ! -f "$ENV_FILE" ]]; then
    cp "$ROOT_DIR/.env.example" "$ENV_FILE"
    printf 'Created .env from .env.example. Edit its passwords, then run ./start-backend.sh again.\n'
    exit 1
fi

if grep -q 'replace-with' "$ENV_FILE"; then
    printf 'Replace the example passwords in .env before starting the application.\n' >&2
    exit 1
fi

for command in docker mvn; do
    if ! command -v "$command" >/dev/null 2>&1; then
        printf 'Required command not found: %s\n' "$command" >&2
        exit 1
    fi
done

cd "$ROOT_DIR"
set -a
source "$ENV_FILE"
set +a

docker compose up -d mysql
container_id="$(docker compose ps -q mysql)"
if [[ -z "$container_id" ]]; then
    printf 'MySQL container did not start. Check Docker Desktop or the Docker service.\n' >&2
    exit 1
fi

healthy=false
for attempt in {1..60}; do
    health="$(docker inspect --format '{{.State.Health.Status}}' "$container_id" 2>/dev/null || true)"
    if [[ "$health" == "healthy" ]]; then
        healthy=true
        break
    fi
    if [[ "$health" == "unhealthy" ]]; then
        printf 'MySQL reported an unhealthy state. Check: docker compose logs mysql\n' >&2
        exit 1
    fi
    sleep 2
done

if [[ "$healthy" != true ]]; then
    printf 'Timed out waiting for MySQL. Check: docker compose logs mysql\n' >&2
    exit 1
fi

printf 'Starting Spring Boot API at http://127.0.0.1:8080 (Ctrl+C stops the API).\n'
cd "$ROOT_DIR/backend"
exec mvn spring-boot:run
