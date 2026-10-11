#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

for command in node npm; do
    if ! command -v "$command" >/dev/null 2>&1; then
        printf 'Required command not found: %s\n' "$command" >&2
        exit 1
    fi
done

if [[ ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
    printf 'Installing frontend packages from package-lock.json...\n'
    npm --prefix "$ROOT_DIR/frontend" ci
fi

printf 'Starting AngularJS/Vite site at http://127.0.0.1:5173 (Ctrl+C stops it).\n'
cd "$ROOT_DIR/frontend"
exec npm run dev
