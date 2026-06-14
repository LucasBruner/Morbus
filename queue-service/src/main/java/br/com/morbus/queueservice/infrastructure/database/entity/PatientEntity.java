package br.com.morbus.queueservice.infrastructure.database.entity;

import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patients")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatientEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "cpf", nullable = false, unique = true, length = 14)
    private String cpf;

    @Column(name = "cns", unique = true, length = 15)
    private String cns;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "sobrenome", nullable = false, length = 100)
    private String sobrenome;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", length = 10)
    private EGender sexo;

    @Column(name = "contato", length = 255)
    private String contato;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "grupo_legal", nullable = false)
    private EPriorityGroup grupoLegal;

    @Column(name = "ativo")
    private Boolean ativo;
}