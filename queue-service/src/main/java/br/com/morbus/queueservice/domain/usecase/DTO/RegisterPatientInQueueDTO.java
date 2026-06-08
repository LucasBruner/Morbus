package br.com.morbus.queueservice.domain.usecase.DTO;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.enums.ERiskColor;

import java.util.UUID;

public record RegisterPatientInQueueDTO(Patient patient,
                                        UUID procedureId,
                                        ERiskColor riskColor) {
}
