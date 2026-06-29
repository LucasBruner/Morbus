package br.com.morbus.agendamento.adapter.security;

import java.util.UUID;

public record UserPrincipal(String username,
                            UUID userId,
                            UUID unitId,
                            String role) {}
