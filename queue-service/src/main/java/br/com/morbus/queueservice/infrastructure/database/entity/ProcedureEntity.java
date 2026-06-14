package br.com.morbus.queueservice.infrastructure.database.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "procedures")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcedureEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "co_procedimento", nullable = false, unique = true)
    private String coProcedimento;

    @Column(name = "no_procedimento", nullable = false)
    private String noProcedimento;

    @Column(name = "idade_minima")
    private Integer idadeMinima;

    @Column(name = "idade_maxima")
    private Integer idadeMaxima;

    private String grupo;
}
