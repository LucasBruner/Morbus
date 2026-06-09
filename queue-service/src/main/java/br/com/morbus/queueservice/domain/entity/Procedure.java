package br.com.morbus.queueservice.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.Period;
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

    public boolean isAgeEligible(LocalDate dataNascimento) {
        int age = Period.between(dataNascimento, LocalDate.now()).getYears();
        return age >= idadeMinima && age <= idadeMaxima;
    }
}
