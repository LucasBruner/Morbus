package br.com.morbus.queueservice.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Procedure {
    private UUID id;
    private String coProcedimento;
    private String noProcedimento;
    private int idadeMinima;
    private int idadeMaxima;
    private String grupo;
}
