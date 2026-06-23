CREATE TABLE sensor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    suhu DOUBLE,
    kelembaban DOUBLE,
    heater INT,
    kipas INT,
    created_at DATETIME
);
