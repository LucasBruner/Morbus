package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PatientRequestDTO(
        @NotBlank(message = "O CPF é obrigatório")
        String cpf,
        String cns,
        @NotBlank(message = "É obrigatório informar o nome do paciente!")
        String nome,
        @NotBlank(message = "É obrigatório informar o sobrenome do paciente!")
        String sobrenome,
        @NotNull(message = "A data de nascimento é obrigatória")
        LocalDate dataNascimento,
        EGender gender,
        String contato,
        EPriorityGroup grupoLegal
) {}