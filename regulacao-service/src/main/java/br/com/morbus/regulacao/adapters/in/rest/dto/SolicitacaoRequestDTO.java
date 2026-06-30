package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.ports.in.dto.CriarSolicitacaoCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SolicitacaoRequestDTO(
        @NotNull(message = "patientId e obrigatorio") UUID patientId,
        @NotNull(message = "procedureId e obrigatorio") UUID procedureId,
        @NotBlank(message = "cid e obrigatorio") String cid,
        @NotBlank(message = "justificativaClinica e obrigatoria") String justificativaClinica,
        @NotBlank(message = "profissionalSolicitante e obrigatorio") String profissionalSolicitante,
        String crmProfissional,
        @NotNull(message = "unitSolicitanteId e obrigatorio") UUID unitSolicitanteId,
        @NotNull(message = "destino e obrigatorio") EDestino destino) {

    public CriarSolicitacaoCommand toCommand(UUID userId) {
        return new CriarSolicitacaoCommand(
                patientId,
                procedureId,
                unitSolicitanteId,
                cid,
                justificativaClinica,
                profissionalSolicitante,
                crmProfissional,
                destino,
                userId);
    }
}
