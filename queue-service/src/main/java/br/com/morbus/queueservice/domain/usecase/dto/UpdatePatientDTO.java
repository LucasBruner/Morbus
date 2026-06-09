package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record UpdatePatientDTO(@NotNull(message = "É obrigatório informar o ID do paciente!")
                               UUID id,
                               String cns,
                               @NotBlank(message = "É obrigatório informar o nome do paciente!")
                               String nome,
                               @NotBlank(message = "É obrigatório informar o sobrenome do paciente!")
                               String sobrenome,
                               @NotNull(message = "É obrigatório informar a data de nascimento do paciente!")
                               LocalDate dataNascimento,
                               EGender gender,
                               @Email
                               String contato,
                               EPriorityGroup grupoLegal) {
}
