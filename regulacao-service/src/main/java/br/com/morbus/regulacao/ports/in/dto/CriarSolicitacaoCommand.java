package br.com.morbus.regulacao.ports.in.dto;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;

import java.util.UUID;

public record CriarSolicitacaoCommand(UUID pacienteId,
                                      UUID procedureId,
                                      UUID unidadeSolicitanteId,
                                      ERiscoSolicitado riscoSolicitado,
                                      String observacoes,
                                      UUID solicitadoPor) {
}
