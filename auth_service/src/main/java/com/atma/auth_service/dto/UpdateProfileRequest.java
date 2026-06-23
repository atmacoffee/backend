package com.atma.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotBlank(message = "Nama wajib diisi")
    @Size(max = 100, message = "Nama maksimal 100 karakter")
    private String nama;

    @Size(max = 120, message = "Lokasi maksimal 120 karakter")
    private String lokasi;

    @Size(max = 80, message = "Jenis kopi maksimal 80 karakter")
    private String jenisKopi;

    @Size(max = 80, message = "Nama alat maksimal 80 karakter")
    private String namaAlat;

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getLokasi() {
        return lokasi;
    }

    public void setLokasi(String lokasi) {
        this.lokasi = lokasi;
    }

    public String getJenisKopi() {
        return jenisKopi;
    }

    public void setJenisKopi(String jenisKopi) {
        this.jenisKopi = jenisKopi;
    }

    public String getNamaAlat() {
        return namaAlat;
    }

    public void setNamaAlat(String namaAlat) {
        this.namaAlat = namaAlat;
    }
}
