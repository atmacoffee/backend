#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEPLOY_DIR="${ROOT_DIR}/deploy"
ENV_FILE="${DEPLOY_DIR}/.env"

docker compose -f "${DEPLOY_DIR}/docker-compose.yml" --env-file "${ENV_FILE}" build
docker compose -f "${DEPLOY_DIR}/docker-compose.yml" --env-file "${ENV_FILE}" up -d
docker compose -f "${DEPLOY_DIR}/docker-compose.yml" --env-file "${ENV_FILE}" ps
