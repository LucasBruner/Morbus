package br.com.morbus.regulacao.domain.dto;

import java.util.UUID;

public record UsuarioContexto(String role, UUID pacienteId) {
}
