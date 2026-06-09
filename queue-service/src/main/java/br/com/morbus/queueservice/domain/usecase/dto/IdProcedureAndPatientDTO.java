package br.com.morbus.queueservice.domain.usecase.dto;

import java.util.UUID;

public record IdProcedureAndPatientDTO(UUID patientId,
                                       UUID procedureId) {
}
