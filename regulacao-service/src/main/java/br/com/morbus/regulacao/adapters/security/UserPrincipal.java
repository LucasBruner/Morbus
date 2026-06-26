package br.com.morbus.regulacao.adapters.security;

import java.util.UUID;

public record UserPrincipal(String username, UUID userId, UUID unitId, String role) {}