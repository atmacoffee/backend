package com.atma.auth_service.dto;

import com.atma.auth_service.model.Pengguna;

public record UserResponse(
        Long id,
        String nama,
        String email,
        String lokasi,
        String jenisKopi,
        String namaAlat) {

    public static UserResponse from(Pengguna pengguna) {
        return new UserResponse(
                pengguna.getId(),
                pengguna.getNama(),
                pengguna.getEmail(),
                pengguna.getLokasi(),
                pengguna.getJenisKopi(),
                pengguna.getNamaAlat());
    }
}
