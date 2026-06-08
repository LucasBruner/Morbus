package br.com.morbus.queueservice.domain.usecase.DTO;

import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

public record RegisterPatientDTO(@NotBlank(message = "É obrigatório informar o CPF!")
                                 @CPF
                                 String cpf,
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
