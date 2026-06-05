package br.com.morbus.queueservice.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
public class Procedure {
    private UUID id;
    private String coProcedimento;
    private String noProcedimento;
    private int idadeMinima;
    private int idadeMaxima;
    private String grupo;
}
