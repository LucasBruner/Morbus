package br.com.morbus.queueservice.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Procedure")
class ProcedureTest {

    private Procedure buildProcedure(int idadeMinima, int idadeMaxima) {
        return Procedure.builder()
                .id(UUID.randomUUID())
                .coProcedimento("0301010072")
                .noProcedimento("CONSULTA MÉDICA EM ATENÇÃO BÁSICA")
                .idadeMinima(idadeMinima)
                .idadeMaxima(idadeMaxima)
                .grupo("01")
                .build();
    }

    @Nested
    @DisplayName("isAgeEligible")
    class IsAgeEligible {

        @Test
        @DisplayName("deve retornar true quando idade está dentro do intervalo permitido")
        void deveRetornarTrueQuandoIdadeDentroDoIntervalo() {
            Procedure procedure = buildProcedure(18, 60);
            LocalDate dataNascimento = LocalDate.now().minusYears(30);

            assertThat(procedure.isAgeEligible(dataNascimento)).isTrue();
        }

        @Test
        @DisplayName("deve retornar true quando idade está exatamente no limite mínimo")
        void deveRetornarTrueQuandoIdadeExatamenteNoMinimo() {
            Procedure procedure = buildProcedure(18, 60);
            LocalDate dataNascimento = LocalDate.now().minusYears(18);

            assertThat(procedure.isAgeEligible(dataNascimento)).isTrue();
        }

        @Test
        @DisplayName("deve retornar true quando idade está exatamente no limite máximo")
        void deveRetornarTrueQuandoIdadeExatamenteNoMaximo() {
            Procedure procedure = buildProcedure(18, 60);
            LocalDate dataNascimento = LocalDate.now().minusYears(60);

            assertThat(procedure.isAgeEligible(dataNascimento)).isTrue();
        }

        @Test
        @DisplayName("deve retornar false quando idade é menor que o mínimo permitido")
        void deveRetornarFalseQuandoIdadeMenorQueMinimo() {
            Procedure procedure = buildProcedure(18, 60);
            LocalDate dataNascimento = LocalDate.now().minusYears(17);

            assertThat(procedure.isAgeEligible(dataNascimento)).isFalse();
        }

        @Test
        @DisplayName("deve retornar false quando idade é maior que o máximo permitido")
        void deveRetornarFalseQuandoIdadeMaiorQueMaximo() {
            Procedure procedure = buildProcedure(18, 60);
            LocalDate dataNascimento = LocalDate.now().minusYears(61);

            assertThat(procedure.isAgeEligible(dataNascimento)).isFalse();
        }

        @Test
        @DisplayName("deve considerar procedimento pediátrico inelegível para adulto")
        void deveConsiderarProcedimentoPediatricoInelegivelParaAdulto() {
            Procedure procedimentoPediatrico = buildProcedure(0, 12);
            LocalDate dataNascimento = LocalDate.now().minusYears(30);

            assertThat(procedimentoPediatrico.isAgeEligible(dataNascimento)).isFalse();
        }

        @Test
        @DisplayName("deve considerar procedimento geral elegível para qualquer idade no intervalo")
        void deveConsiderarProcedimentoGeralElegivelParaQualquerIdade() {
            Procedure procedimentoGeral = buildProcedure(0, 120);
            LocalDate recemNascido = LocalDate.now().minusMonths(1);
            LocalDate idoso = LocalDate.now().minusYears(90);

            assertThat(procedimentoGeral.isAgeEligible(recemNascido)).isTrue();
            assertThat(procedimentoGeral.isAgeEligible(idoso)).isTrue();
        }
    }
}
