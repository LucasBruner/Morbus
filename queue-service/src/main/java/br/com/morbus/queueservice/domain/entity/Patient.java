package br.com.morbus.queueservice.domain.entity;

import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    private UUID id;
    private String cpf;
    private String cns;
    private String nome;
    private String sobrenome;
    private LocalDate dataNascimento;
    private EGender gender;
    private String contato;
    private EPriorityGroup grupoLegal;
}
