package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import java.time.LocalDate;
import java.util.UUID;

public record PatientResponseDTO(
        UUID id,
        String cpf,
        String nome,
        String sobrenome,
        LocalDate dataNascimento,
        String cns,
        EGender gender,
        String contato,
        boolean ativo,
        EPriorityGroup grupoLegal
) {
    public static PatientResponseDTO fromEntity(Patient patient) {
        if (patient == null) return null;
        return new PatientResponseDTO(
                patient.getId(),
                patient.getCpf(),
                patient.getNome(),
                patient.getSobrenome(),
                patient.getDataNascimento(),
                patient.getCns(),
                patient.getGender(),
                patient.getContato(),
                patient.isAtivo(),
                patient.getGrupoLegal()
        );
    }
}
