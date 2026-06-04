package br.com.morbus.queueservice.domain.entity;

import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;

import java.time.LocalDate;
import java.util.UUID;

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
