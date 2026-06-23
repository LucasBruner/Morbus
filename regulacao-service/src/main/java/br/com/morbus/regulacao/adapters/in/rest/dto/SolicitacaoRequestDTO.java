package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.ports.in.CriarSolicitacaoCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SolicitacaoRequestDTO(@NotNull(message = "pacienteId é obrigatório") UUID pacienteId,
                                    @NotNull(message = "procedureId é obrigatório") UUID procedureId,
                                    @NotNull(message = "unidadeSolicitanteId é obrigatório") UUID unidadeSolicitanteId,
                                    ERiscoSolicitado riscoSolicitado,
                                    String observacoes) {

    public CriarSolicitacaoCommand toCommand(String solicitadoPor) {
        try {
            return new CriarSolicitacaoCommand(
                    pacienteId,
                    procedureId,
                    unidadeSolicitanteId,
                    riscoSolicitado,
                    observacoes,
                    UUID.fromString(solicitadoPor)
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Principal do token não é um UUID válido", e);
        }
    }
}
