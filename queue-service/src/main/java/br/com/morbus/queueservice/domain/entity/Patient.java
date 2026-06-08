package br.com.morbus.queueservice.domain.entity;

import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.usecase.DTO.UpdatePatientDTO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Getter
public class Patient {
    private UUID id;
    private String cpf;
    private String cns;
    private String nome;
    private String sobrenome;
    private LocalDate dataNascimento;
    private EGender gender;
    private String contato;
    private boolean ativo;
    private EPriorityGroup grupoLegal;

    public void updateGrupoLegal(EPriorityGroup grupoLegal) {
        this.grupoLegal = grupoLegal;
    }

    public boolean isAtivo(){
        return this.ativo;
    }

    public void update(UpdatePatientDTO updatePatientDTO){
        this.cns = updatePatientDTO.cns();
        this.nome = updatePatientDTO.nome();
        this.sobrenome = updatePatientDTO.sobrenome();
        this.dataNascimento = updatePatientDTO.dataNascimento();
        this.gender = updatePatientDTO.gender();
        this.contato = updatePatientDTO.contato();
        this.grupoLegal = updatePatientDTO.grupoLegal();
    }
}
