package br.com.fiap.auth_service.model.dto;

import br.com.fiap.auth_service.model.UserRole;

public record AuthResponseDTO(String token,
                              String type,
                              long expiresIn,
                              UserRole role) {

    public static AuthResponseDTO of (String token, long  expiresIn, UserRole role) {
        return new AuthResponseDTO(token, "Bearer", expiresIn, role);
    }
}
