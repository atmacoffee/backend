CREATE TABLE pengguna (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nama VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    lokasi VARCHAR(255),
    jenis_kopi VARCHAR(255),
    nama_alat VARCHAR(255)
);

CREATE TABLE token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pengguna_id BIGINT,
    token VARCHAR(500),
    created_at DATETIME
);
