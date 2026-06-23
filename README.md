# ATMA Springboot Microservices

Selamat datang di repositori backend ATMA! Proyek ini adalah sistem backend berbasis **Microservices** yang dirancang khusus untuk menangani kebutuhan perangkat keras IoT (seperti sensor suhu dan kelembaban) dan dikombinasikan dengan aplikasi klien (seperti Android/Flutter).

Proyek ini dibangun menggunakan **Spring Boot** dan diarsiteki supaya memiliki performa tinggi, aman, dan mudah di-_scale_ (dikembangkan) ketika jumlah pengguna dan data sensor semakin banyak.

## Tech Stack

- **Java 17** & **Spring Boot 3**
- **Spring Cloud** (Eureka & Gateway)
- **MySQL 8** (Database Relasional)
- **Eclipse Mosquitto** (MQTT Broker untuk IoT)
- **Flyway** (Database Migration)
- **Docker & Docker Compose** (Infrastruktur & Deployment)

---

## Struktur Microservices

Alih-alih digabung jadi satu aplikasi besar (monolith), proyek ini dipecah menjadi 4 service utama yang saling bekerja sama:

1. **Eureka Server (`port: 8761`)**
   Ibarat buku telepon. Semua service lain bakal laporan ke sini supaya mereka bisa saling kenal dan berkomunikasi satu sama lain tanpa perlu menghafal IP address masing-masing.

2. **API Gateway (`port: 8085`)**
   Satu-satunya pintu masuk untuk aplikasi mobile/web. Gateway ini bertugas memfilter keamanan (memvalidasi JWT Token) dan mengarahkan _request_ kamu ke service yang benar.

3. **Auth Service (`port: 8081`)**
   Tukang jaga pintu. Mengurus registrasi user, login, enkripsi password (menggunakan algoritma BCrypt), dan menerbitkan tiket masuk otomatis alias **JWT Token**.

4. **Sensor Service (`port: 8082`)**
   Pekerja keras yang mengurus masuknya data IoT. Service ini otomatis _subscribe_ ke broker MQTT untuk menangkap data sensor secara _real-time_ dan menyimpannya ke database dengan fitur Pagination supaya server tidak kehabisan memori.

---

## Cara Menjalankan Project (Local Development)

Kamu tidak perlu repot menginstall database atau broker MQTT secara manual. Semuanya sudah dibungkus rapi menggunakan Docker. Pastikan kamu sudah menginstall **Docker** dan **Docker Compose** di komputermu ya.

**Langkah-langkah:**

1. Buka terminal dan arahkan ke folder utama proyek ini.
2. Jalankan perintah magis ini:
   ```bash
   docker-compose up --build -d
   ```
3. Tunggu sekitar 1-2 menit. Docker bakal otomatis men-download environment Java, menyalakan MySQL, menyalakan Mosquitto, dan menjalankan keempat service di atas secara berurutan.
4. Untuk mengecek apakah semuanya sudah jalan lancar, kamu bisa melihat log-nya dengan perintah:
   ```bash
   docker-compose logs -f
   ```

_(Tips: Kamu bisa berhenti memantau log dengan menekan tombol `Ctrl + C` di terminal. Server akan tetap berjalan di background)._

---

## Cara Komunikasi dengan Backend

### 1. Dari Aplikasi Android/Flutter (Via HTTP/REST API)

Semua request HTTP dari UI/Aplikasi **HARUS** diarahkan ke API Gateway di port `8085`. Jangan tembak langsung ke port internal service.

- **Register User Baru**: `POST http://localhost:8085/auth/register` (Kirim Body JSON: nama, email, password)
- **Login**: `POST http://localhost:8085/auth/login` -> Akan mengembalikan string JWT Token.
- **Lihat Data Sensor**: `GET http://localhost:8085/sensor?page=0&size=10` _(Wajib menyertakan Header: `Authorization: Bearer <Token Kamu>`)_

### 2. Dari Perangkat IoT/Sensor (Via Protokol MQTT)

Untuk mengirim data sensor secara _real-time_ dan berkecepatan tinggi, perangkat IoT kamu (misalnya ESP32 atau Arduino) cukup mengirim pesan (_Publish_) menggunakan protokol MQTT:

- **Broker Public ESP32 (TLS)**: `mqtt.atma.biz.id:8883`
- **Broker Internal Docker**: `mosquitto:1883` hanya untuk komunikasi antar container di EC2
- **Telemetry Topic**: `atma/device/atma-dryer-001/telemetry`
- **Command Topics**:
  - `atma/device/atma-dryer-001/command/actuator`
  - `atma/device/atma-dryer-001/command/mode`
- **Payload (Format JSON)**:
  ```json
  {
    "suhu": 30.5,
    "kelembaban": 70.2,
    "heater": 0,
    "kipas": 1,
    "exhaust": false
  }
  ```
  Begitu data tersebut di-publish, _Sensor Service_ akan otomatis menangkap pesan itu di _background_ dan menyimpannya ke database tanpa harus menunggu respon HTTP yang membebani jaringan.

---

## Punya Kendala?

Jika kamu menemukan _error_ atau server tiba-tiba mati saat pertama kali dijalankan, coba pastikan port `3306` (MySQL), `1883` (MQTT), dan `8085` (Gateway) di komputermu belum terpakai oleh aplikasi lain.

Selamat bereksplorasi dan _happy coding_! ☕
