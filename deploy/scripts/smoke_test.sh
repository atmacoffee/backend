#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${1:-http://localhost}"

curl -fsS "${API_BASE_URL}/actuator/health" || true
curl -fsS "${API_BASE_URL}/sensor/latest" || true
curl -fsS "${API_BASE_URL}/sensor/device/status" || true
curl -fsS "${API_BASE_URL}/sensor/actuator/status" || true
curl -fsS "http://127.0.0.1:9090/-/healthy" || true
curl -fsS "http://127.0.0.1:3000/api/health" || true
