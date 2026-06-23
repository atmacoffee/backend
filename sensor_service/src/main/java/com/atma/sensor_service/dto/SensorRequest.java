package com.atma.sensor_service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SensorRequest {

    @NotNull(message = "Suhu wajib diisi")
    @DecimalMin(value = "0.0", message = "Suhu tidak boleh negatif")
    @DecimalMax(value = "120.0", message = "Suhu maksimal 120")
    private Double suhu;

    @NotNull(message = "Kelembaban wajib diisi")
    @DecimalMin(value = "0.0", message = "Kelembaban tidak boleh negatif")
    @DecimalMax(value = "100.0", message = "Kelembaban maksimal 100")
    private Double kelembaban;

    @NotNull(message = "Status heater wajib diisi")
    @Min(value = 0, message = "Status heater harus 0 atau 1")
    @Max(value = 1, message = "Status heater harus 0 atau 1")
    private Integer heater;

    @NotNull(message = "Status kipas wajib diisi")
    @Min(value = 0, message = "Status kipas harus 0 atau 1")
    @Max(value = 1, message = "Status kipas harus 0 atau 1")
    private Integer kipas;

    private Boolean exhaust = false;

    public Double getSuhu() { return suhu; }
    public void setSuhu(Double suhu) { this.suhu = suhu; }
    public Double getKelembaban() { return kelembaban; }
    public void setKelembaban(Double kelembaban) { this.kelembaban = kelembaban; }
    public Integer getHeater() { return heater; }
    public void setHeater(Integer heater) { this.heater = heater; }
    public Integer getKipas() { return kipas; }
    public void setKipas(Integer kipas) { this.kipas = kipas; }
    public Boolean getExhaust() { return exhaust; }
    public void setExhaust(Boolean exhaust) { this.exhaust = exhaust; }
}
