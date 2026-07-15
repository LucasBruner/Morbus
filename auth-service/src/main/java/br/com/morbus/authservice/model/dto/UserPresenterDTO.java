package br.com.morbus.authservice.model.dto;

import java.time.LocalDateTime;

public record UserPresenterDTO(String id, String username, String email, String role, String unitId, LocalDateTime createdAt) {
}
