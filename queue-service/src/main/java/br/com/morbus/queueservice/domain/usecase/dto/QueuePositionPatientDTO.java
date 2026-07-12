package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.entity.Patient;

import java.util.UUID;

public record QueuePositionPatientDTO(UUID id, String nomeCompleto) {
    public static QueuePositionPatientDTO fromEntity(Patient patient) {
        if (patient == null) return null;
        String nomeCompleto = (patient.getNome() != null ? patient.getNome() : "")
                + (patient.getSobrenome() != null ? " " + patient.getSobrenome() : "");
        return new QueuePositionPatientDTO(patient.getId(), nomeCompleto.trim());
    }
}
