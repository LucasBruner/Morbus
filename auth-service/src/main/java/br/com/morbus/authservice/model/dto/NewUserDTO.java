package br.com.morbus.authservice.model.dto;

import br.com.morbus.authservice.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record NewUserDTO(

        @NotBlank(message = "Username é obrigatório")
        @Size(min = 3, max = 100, message = "Username deve ter entre 3 e 100 caracteres")
        String username,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        String password,

        @NotNull(message = "Role é obrigatória. Valores aceitos: MEDICO, PACIENTE, SOLICITANTE, REGULADOR, EXECUTANTE")
        UserRole role,

        // Opcional: relevante apenas para EXECUTANTE/SOLICITANTE, que operam em uma unidade
        // especifica. Demais roles deixam null e o JWT simplesmente nao carrega o claim unit_id.
        UUID unitId

) {
    public NewUserDTO(String username, String email, String password, UserRole role) {
        this(username, email, password, role, null);
    }
}

