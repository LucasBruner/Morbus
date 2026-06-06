package br.com.morbus.queueservice.domain.dto;

import br.com.morbus.queueservice.domain.entity.Patient;

import java.util.UUID;

public record RegisterPatientDTO(Patient patient,
                                 UUID procedureId) {
}
