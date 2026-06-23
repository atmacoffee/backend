#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEPLOY_DIR="${ROOT_DIR}/deploy"
ENV_FILE="${DEPLOY_DIR}/.env"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.yml"
MOSQUITTO_PASSWORD_FILE="${DEPLOY_DIR}/mosquitto/passwordfile"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1"
    exit 1
  }
}

require_command docker
require_command ss
require_command unzip
docker compose version >/dev/null 2>&1 || {
  echo "docker compose plugin is required"
  exit 1
}

[[ -f "${ENV_FILE}" ]] || {
  echo "Missing ${ENV_FILE}"
  exit 1
}

env_mode="$(stat -c '%a' "${ENV_FILE}")"
[[ "${env_mode}" == "600" ]] || {
  echo "${ENV_FILE} must use permission 600"
  exit 1
}

if awk '
  /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
  /CHANGE_ME/ { found=1 }
  END { exit found ? 0 : 1 }
' "${ENV_FILE}"; then
  echo "Environment file still contains CHANGE_ME placeholders"
  exit 1
fi

if awk '
  /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
  /example[.]com|your-rds-endpoint/ { found=1 }
  END { exit found ? 0 : 1 }
' "${ENV_FILE}"; then
  echo "Environment file still contains example placeholder values"
  exit 1
fi

[[ -f "${MOSQUITTO_PASSWORD_FILE}" ]] || {
  echo "Missing Mosquitto password file: ${MOSQUITTO_PASSWORD_FILE}"
  exit 1
}

password_mode="$(stat -c '%a' "${MOSQUITTO_PASSWORD_FILE}")"
[[ "${password_mode}" == "600" || "${password_mode}" == "640" ]] || {
  echo "${MOSQUITTO_PASSWORD_FILE} must use permission 600 or 640"
  exit 1
}

password_owner="$(stat -c '%u:%g' "${MOSQUITTO_PASSWORD_FILE}")"
[[ -n "${password_owner}" ]] || {
  echo "Unable to determine Mosquitto password file owner"
  exit 1
}

for port in 80 443 8883; do
  if ss -ltn "( sport = :${port} )" | grep -q ":${port}"; then
    echo "Port ${port} is already in use"
    exit 1
  fi
done

[[ -f "${ROOT_DIR}/api_gateway/Dockerfile" ]]
[[ -f "${ROOT_DIR}/auth_service/Dockerfile" ]]
[[ -f "${ROOT_DIR}/sensor_service/Dockerfile" ]]
[[ -f "${ROOT_DIR}/eureka_server/Dockerfile" ]]
[[ -f "${DEPLOY_DIR}/monitoring/prometheus/prometheus.yml" ]]
[[ -f "${DEPLOY_DIR}/monitoring/grafana/provisioning/datasources/datasource.yml" ]]

required_env_keys=(
  SPRING_PROFILES_ACTIVE
  API_DOMAIN
  CORS_ALLOWED_ORIGINS
  JWT_SECRET
  INTERNAL_SERVICE_TOKEN
  DB_HOST
  DB_PORT
  DB_NAME
  DB_USERNAME
  DB_PASSWORD
  EUREKA_SERVER_URL
  API_GATEWAY_PORT
  AUTH_SERVICE_PORT
  SENSOR_SERVICE_PORT
  EUREKA_SERVER_PORT
  MQTT_HOST
  MQTT_PORT
  MQTT_USERNAME
  MQTT_PASSWORD
  MQTT_USE_TLS
  MQTT_TLS_CA_FILE
  MQTT_TLS_CERT_FILE
  MQTT_TLS_KEY_FILE
  MQTT_BROKER_HOSTNAME
  MQTT_PUBLIC_PORT
  MQTT_SENSOR_TOPIC
  MQTT_COMMAND_TOPIC_PREFIX
  DEVICE_ID
  DEVICE_OFFLINE_THRESHOLD_SECONDS
  NOTIFICATION_TEMPERATURE_IDEAL_MIN
  NOTIFICATION_TEMPERATURE_IDEAL_MAX
  NOTIFICATION_HUMIDITY_THRESHOLD
  NOTIFICATION_EVENT_COOLDOWN_SECONDS
  MAIL_HOST
  MAIL_PORT
  MAIL_USERNAME
  MAIL_PASSWORD
  MAIL_FROM
  MAIL_FROM_NAME
  PASSWORD_RESET_CODE_TTL_MINUTES
  PASSWORD_RESET_MAX_ATTEMPTS
  PASSWORD_RESET_REQUEST_COOLDOWN_SECONDS
  PASSWORD_RESET_SESSION_TTL_MINUTES
  GRAFANA_ADMIN_USER
  GRAFANA_ADMIN_PASSWORD
  MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE
  MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS
)

for key in "${required_env_keys[@]}"; do
  grep -Eq "^${key}=.+" "${ENV_FILE}" || {
    echo "Missing required env key: ${key}"
    exit 1
  }
done

if awk -F= '
  /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
  $1 == "DB_HOST" {
    value=$2
    gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
    if (value == "localhost" || value == "127.0.0.1" || value == "mysql") bad=1
  }
  END { exit bad ? 0 : 1 }
' "${ENV_FILE}"; then
  echo "DB_HOST must point to Amazon RDS, not a local or container MySQL host"
  exit 1
fi

if grep -Eq '^\s*mysql:' "${COMPOSE_FILE}"; then
  echo "Production compose must not define a MySQL container when using Amazon RDS"
  exit 1
fi

get_env_value() {
  local key="$1"
  awk -F= -v wanted="${key}" '
    /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
    $1 == wanted {
      value=$0
      sub("^[^=]*=", "", value)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      print value
      exit
    }
  ' "${ENV_FILE}"
}

mqtt_host="$(get_env_value MQTT_HOST)"
mqtt_port="$(get_env_value MQTT_PORT)"
mqtt_use_tls="$(get_env_value MQTT_USE_TLS)"
mqtt_public_port="$(get_env_value MQTT_PUBLIC_PORT)"

if [[ "${mqtt_host}" != "mosquitto" || "${mqtt_port}" != "1883" || "${mqtt_use_tls}" != "false" ]]; then
  echo "Backend MQTT must use internal Docker listener: MQTT_HOST=mosquitto, MQTT_PORT=1883, MQTT_USE_TLS=false"
  exit 1
fi

if [[ "${mqtt_public_port}" != "8883" ]]; then
  echo "ESP32/public MQTT TLS listener must use MQTT_PUBLIC_PORT=8883"
  exit 1
fi

if grep -Eq '["'\'']1883:1883|-\s*1883:1883' "${COMPOSE_FILE}"; then
  echo "Mosquitto internal listener 1883 must not be published to the EC2 host"
  exit 1
fi

services=(eureka_server api_gateway auth_service sensor_service)

for service in "${services[@]}"; do
  service_dir="${ROOT_DIR}/${service}"
  pom_file="${service_dir}/pom.xml"
  dockerfile="${service_dir}/Dockerfile"
  jar_file="${service_dir}/target/app.jar"

  grep -q "<artifactId>spring-boot-maven-plugin</artifactId>" "${pom_file}" || {
    echo "${service}: missing spring-boot-maven-plugin"
    exit 1
  }

  grep -q "<goal>repackage</goal>" "${pom_file}" || {
    echo "${service}: missing Spring Boot repackage goal"
    exit 1
  }

  grep -q "<finalName>app</finalName>" "${pom_file}" || {
    echo "${service}: missing finalName app"
    exit 1
  }

  if grep -qE 'target/[*][.]jar|target/.*[*]' "${dockerfile}"; then
    echo "${service}: Dockerfile must copy /app/target/app.jar, not a wildcard JAR"
    exit 1
  fi

  grep -q "/app/target/app.jar" "${dockerfile}" || {
    echo "${service}: Dockerfile does not copy /app/target/app.jar"
    exit 1
  }

  [[ -f "${jar_file}" ]] || {
    echo "${service}: missing target/app.jar; run ./mvnw clean package -DskipTests in ${service}"
    exit 1
  }

  manifest="$(unzip -p "${jar_file}" META-INF/MANIFEST.MF 2>/dev/null || true)"
  [[ -n "${manifest}" ]] || {
    echo "${service}: target/app.jar has no readable manifest"
    exit 1
  }

  grep -q "Main-Class: org.springframework.boot.loader.launch.JarLauncher" <<<"${manifest}" || {
    echo "${service}: target/app.jar is not a Spring Boot executable JAR"
    exit 1
  }

  grep -q "^Start-Class: " <<<"${manifest}" || {
    echo "${service}: target/app.jar manifest is missing Start-Class"
    exit 1
  }
done

docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" config --quiet

if awk -F= '
  /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
  {
    value=$2
    gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
    if (value == "CHANGE_ME" || value == "admin") found=1
  }
  END { exit found ? 0 : 1 }
' "${ENV_FILE}"; then
  echo "One or more secrets still use insecure defaults"
  exit 1
fi

echo "Preflight check passed"
