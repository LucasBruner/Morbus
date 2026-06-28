package br.com.morbus.regulacao.domain.dto;

import br.com.morbus.regulacao.adapters.security.UserPrincipal;

import java.util.UUID;

public record UsuarioContexto(String role, UUID pacienteId) {
    public static UsuarioContexto userPrincipalToContexto(UserPrincipal principal) {
        return new UsuarioContexto(principal.role(), principal.userId());
    }
}
