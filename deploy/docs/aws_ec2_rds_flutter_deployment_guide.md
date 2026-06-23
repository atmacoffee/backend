# Panduan Deploy ATMA IoT ke AWS EC2, RDS, dan Flutter

Dokumen ini adalah checklist pelaksanaan deploy production untuk arsitektur:

- EC2 2 vCPU / 4 GB RAM sebagai host Docker untuk backend, nginx, Mosquitto, Prometheus, dan Grafana.
- Amazon RDS MySQL sebagai database.
- `api.atma.biz.id` untuk REST API Flutter.
- `mqtt.atma.biz.id` untuk MQTT TLS ESP32.

## 1. Pola Jaringan

Public port EC2 yang dibuka:

| Port | Tujuan | Source |
|---|---|---|
| 22 | SSH | IP pribadi/admin saja |
| 80 | HTTP redirect/certificate bootstrap | `0.0.0.0/0` |
| 443 | HTTPS API `api.atma.biz.id` | `0.0.0.0/0` |
| 8883 | MQTT TLS `mqtt.atma.biz.id` | `0.0.0.0/0` atau IP device jika statis |

Port yang tidak boleh dipublish ke internet:

- `1883` MQTT internal Docker
- `8081`, `8082`, `8085`, `8761`
- `9090`, `3000`
- `3306`

Pola MQTT:

```text
ESP32 -> mqtt.atma.biz.id:8883 TLS -> Mosquitto
sensor_service -> mosquitto:1883 TCP internal Docker -> Mosquitto
```

## 2. DNS

Buat record DNS:

```text
api.atma.biz.id   A   <EC2_PUBLIC_IP>
mqtt.atma.biz.id  A   <EC2_PUBLIC_IP>
```

Tunggu propagasi DNS sebelum membuat certificate.

## 3. Security Group

Security group EC2 inbound:

```text
TCP 22    <IP admin>/32
TCP 80    0.0.0.0/0
TCP 443   0.0.0.0/0
TCP 8883  0.0.0.0/0
```

Security group RDS inbound:

```text
TCP 3306  source: security group EC2
```

Jangan buka RDS ke publik.

## 4. Persiapan EC2

Install paket dasar:

```bash
sudo apt update
sudo apt install -y ca-certificates curl git unzip openssl mosquitto-clients
```

Install Docker dan Compose plugin:

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
```

Logout lalu login lagi supaya group Docker aktif.

Clone atau upload project ke EC2. Contoh lokasi:

```bash
mkdir -p /opt/atma
cd /opt/atma
git clone <repo-url> ATMA-TECH
cd ATMA-TECH/Backend/deploy
```

## 5. Setup RDS MySQL

Buat RDS MySQL dengan:

- Engine: MySQL 8
- Public access: disabled jika EC2 satu VPC
- Database name: `atma_dryer`
- User aplikasi: `atma_app`

Jika perlu membuat user manual, sesuaikan file:

```bash
Backend/deploy/sql/create_app_user.sql
```

Minimal privilege aplikasi:

```sql
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX ON atma_dryer.* TO 'atma_app'@'%';
```

Flyway akan membuat dan memigrasikan tabel saat service start.

## 6. Certificate

Nginx API membutuhkan:

```text
Backend/deploy/nginx/certs/fullchain.pem
Backend/deploy/nginx/certs/privkey.pem
```

Mosquitto MQTT TLS membutuhkan:

```text
Backend/deploy/mosquitto/certs/ca.crt
Backend/deploy/mosquitto/certs/server.crt
Backend/deploy/mosquitto/certs/server.key
```

Pastikan certificate MQTT valid untuk hostname:

```text
mqtt.atma.biz.id
```

Jika memakai self-signed CA, isi root CA yang sama ke `MQTT_CA_CERT` di firmware ESP32.

Permission rekomendasi:

```bash
chmod 755 nginx/certs mosquitto/certs
chmod 644 nginx/certs/fullchain.pem mosquitto/certs/ca.crt mosquitto/certs/server.crt
chmod 600 nginx/certs/privkey.pem mosquitto/certs/server.key
```

## 7. File `.env`

Buat `.env` dari template:

```bash
cp .env.example .env
chmod 600 .env
```

Isi nilai production. Bagian penting:

```env
SPRING_PROFILES_ACTIVE=prod

API_DOMAIN=api.atma.biz.id
CORS_ALLOWED_ORIGINS=https://www.atma.biz.id

DB_HOST=<rds-endpoint>
DB_PORT=3306
DB_NAME=atma_dryer
DB_USERNAME=atma_app
DB_PASSWORD=<password-rds>

MQTT_HOST=mosquitto
MQTT_PORT=1883
MQTT_USE_TLS=false
MQTT_BROKER_HOSTNAME=mqtt.atma.biz.id
MQTT_PUBLIC_PORT=8883
MQTT_USERNAME=sensor_service
MQTT_PASSWORD=<password-user-sensor-service>
MQTT_SENSOR_TOPIC=atma/device/atma-dryer-001/telemetry
MQTT_COMMAND_TOPIC_PREFIX=atma/device

DEVICE_ID=atma-dryer-001
```

Catatan:

- `MQTT_HOST=mosquitto`, `MQTT_PORT=1883`, `MQTT_USE_TLS=false` hanya untuk backend internal Docker.
- ESP32 tetap memakai `mqtt.atma.biz.id:8883` dengan TLS.
- Jangan sisakan nilai `CHANGE_ME`.
- `JWT_SECRET` minimal 32 karakter acak.
- `INTERNAL_SERVICE_TOKEN` harus acak panjang.

Generate secret contoh:

```bash
openssl rand -base64 48
```

## 8. Mosquitto User dan ACL

ACL sudah disiapkan di:

```text
Backend/deploy/mosquitto/acl
```

User:

- `atma_device`: write telemetry, read command.
- `sensor_service`: read telemetry, write command.

Buat passwordfile:

```bash
cd Backend/deploy
touch mosquitto/passwordfile
chmod 600 mosquitto/passwordfile
docker run --rm -it -v "$PWD/mosquitto:/mosquitto" eclipse-mosquitto:2.0 \
  mosquitto_passwd -c /mosquitto/passwordfile sensor_service
docker run --rm -it -v "$PWD/mosquitto:/mosquitto" eclipse-mosquitto:2.0 \
  mosquitto_passwd /mosquitto/passwordfile atma_device
```

Gunakan password `sensor_service` yang sama dengan `MQTT_PASSWORD` di `.env`.

Password `atma_device` harus dimasukkan ke firmware ESP32.

## 9. Preflight

Dari folder `Backend/deploy`:

```bash
../deploy/scripts/preflight_check.sh
```

Jika script dijalankan dari repo root:

```bash
Backend/deploy/scripts/preflight_check.sh
```

Preflight akan memastikan:

- `.env` ada dan permission `600`.
- Tidak ada placeholder.
- RDS bukan localhost/container.
- Compose tidak mendefinisikan MySQL container.
- MQTT backend memakai listener internal `mosquitto:1883`.
- Port publik `80`, `443`, `8883` belum dipakai.
- `1883` tidak dipublish ke host.
- JAR service sudah ada.

Jika JAR belum ada, build masing-masing service dulu:

```bash
cd Backend/eureka_server && ./mvnw clean package -DskipTests
cd ../api_gateway && ./mvnw clean package -DskipTests
cd ../auth_service && ./mvnw clean package -DskipTests
cd ../sensor_service && ./mvnw clean package -DskipTests
```

## 10. Deploy Backend

Dari folder `Backend/deploy`:

```bash
docker compose --env-file .env build
docker compose --env-file .env up -d
docker compose --env-file .env ps
```

Pantau log:

```bash
docker compose --env-file .env logs -f nginx api_gateway auth_service sensor_service mosquitto
```

## 11. Smoke Test API

Health endpoint:

```bash
curl -fsS https://api.atma.biz.id/actuator/health
```

Login/register harus dites dari Flutter atau curl dengan user valid.

Endpoint sensor membutuhkan JWT:

```bash
curl -H "Authorization: Bearer <JWT>" https://api.atma.biz.id/sensor/latest
curl -H "Authorization: Bearer <JWT>" https://api.atma.biz.id/sensor/device/status
curl -H "Authorization: Bearer <JWT>" https://api.atma.biz.id/sensor/actuator/status
```

## 12. Smoke Test MQTT

Tes login MQTT TLS dari EC2:

```bash
mosquitto_pub \
  -h mqtt.atma.biz.id \
  -p 8883 \
  --cafile mosquitto/certs/ca.crt \
  -u atma_device \
  -P '<password-device>' \
  -t atma/device/atma-dryer-001/telemetry \
  -m '{"suhu":52,"kelembaban":50,"heater":0,"kipas":0,"exhaust":false}'
```

Lihat log sensor service:

```bash
docker compose --env-file .env logs -f sensor_service
```

Tes command dari backend dilakukan melalui API:

```bash
curl -X POST \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{"enabled":true}' \
  https://api.atma.biz.id/sensor/actuator/heater
```

## 13. ESP32

Update firmware:

```cpp
const char* WIFI_SSID = "<ssid>";
const char* WIFI_PASSWORD = "<wifi-password>";
const char* MQTT_HOST = "mqtt.atma.biz.id";
const uint16_t MQTT_PORT = 8883;
const char* MQTT_USERNAME = "atma_device";
const char* MQTT_PASSWORD = "<password-device>";
```

Isi `MQTT_CA_CERT` dengan CA yang menandatangani certificate MQTT server.

Topic harus tetap:

```cpp
const char* MQTT_TELEMETRY_TOPIC = "atma/device/atma-dryer-001/telemetry";
const char* MQTT_COMMAND_TOPIC = "atma/device/atma-dryer-001/command/#";
```

Setelah upload, serial monitor harus menunjukkan:

```text
Konek MQTT TLS... OK
Subscribed: atma/device/atma-dryer-001/command/#
MQTT OUT [atma/device/atma-dryer-001/telemetry]: ...
```

## 14. Build Flutter

Build release production:

```bash
cd Flutter/atma_app
flutter clean
flutter pub get
flutter test
flutter analyze
flutter build apk --release --dart-define=ATMA_API_BASE_URL=https://api.atma.biz.id
```

Untuk Android release production, siapkan signing env:

```bash
export ATMA_ANDROID_KEYSTORE_PATH=/path/to/atma-release.jks
export ATMA_ANDROID_KEYSTORE_PASSWORD=<password>
export ATMA_ANDROID_KEY_ALIAS=<alias>
export ATMA_ANDROID_KEY_PASSWORD=<password>
```

Jangan memakai `ATMA_ANDROID_DEMO_RELEASE=true` untuk production.

## 15. Operasional

Cek container:

```bash
docker compose --env-file .env ps
```

Cek resource:

```bash
docker stats
free -h
df -h
```

EC2 4 GB RAM cukup ketat untuk backend lengkap plus monitoring. Jika sering OOM:

- Tambahkan swap 2 GB.
- Turunkan retention Prometheus.
- Jalankan Grafana/Prometheus hanya saat observability dibutuhkan.
- Pertimbangkan upgrade EC2.

Restart:

```bash
docker compose --env-file .env restart
```

Update deploy:

```bash
git pull
docker compose --env-file .env build
docker compose --env-file .env up -d
```

## 16. Checklist Go Live

- DNS `api.atma.biz.id` dan `mqtt.atma.biz.id` mengarah ke EC2.
- EC2 security group hanya membuka `22`, `80`, `443`, `8883`.
- RDS hanya menerima `3306` dari security group EC2.
- `.env` permission `600`, tanpa placeholder.
- Mosquitto `passwordfile` ada dan permission `600` atau `640`.
- Certificate nginx dan Mosquitto tersedia.
- `preflight_check.sh` lulus.
- Semua container healthy.
- `https://api.atma.biz.id/actuator/health` OK.
- MQTT telemetry test masuk ke database.
- Flutter dibuild dengan `ATMA_API_BASE_URL=https://api.atma.biz.id`.
- ESP32 berhasil connect MQTT TLS dan publish telemetry.
