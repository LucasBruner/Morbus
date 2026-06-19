package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

@Schema(description = "Dados para cadastro de novo paciente")
public record RegisterPatientDTO(
        @NotBlank(message = "É obrigatório informar o CPF!")
        @CPF
        @Schema(description = "CPF do paciente (com ou sem formatação)", example = "123.456.789-09")
        String cpf,

        @Schema(description = "Número do Cartão Nacional de Saúde (CNS)", example = "123456789012345")
        String cns,

        @NotBlank(message = "É obrigatório informar o nome do paciente!")
        @Schema(description = "Nome do paciente", example = "Maria")
        String nome,

        @NotBlank(message = "É obrigatório informar o sobrenome do paciente!")
        @Schema(description = "Sobrenome do paciente", example = "Silva")
        String sobrenome,

        @NotNull(message = "É obrigatório informar a data de nascimento do paciente!")
        @Schema(description = "Data de nascimento (formato ISO 8601). Pacientes com ≥60 anos recebem prioridade IDOSO automaticamente.", example = "1990-05-15")
        LocalDate dataNascimento,

        @Schema(description = "Gênero", example = "FEMININO")
        EGender gender,

        @Email
        @Schema(description = "E-mail de contato para notificações", example = "maria.silva@email.com")
        String contato,

        @Schema(description = "Grupo de prioridade legal (Lei 10.048/2000). IDOSO é atribuído automaticamente por idade.", example = "GERAL")
        EPriorityGroup grupoLegal) {
}
