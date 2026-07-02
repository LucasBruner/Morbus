package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.adapters.out.jpa.unidade.UnidadeSolicitanteEntity;
import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UnidadeSolicitanteEntity")
class UnidadeSolicitanteEntityTest {

    private UnidadeSolicitante buildDomain() {
        return new UnidadeSolicitante(UUID.randomUUID(), "1234567", "UBS Central",
                "Rua das Flores, 100", "(11) 4444-5555");
    }

    @Nested
    @DisplayName("fromDomain")
    class FromDomain {

        @Test
        @DisplayName("deve mapear todos os campos do dominio para a entidade")
        void deveMapearTodosOsCampos() {
            UnidadeSolicitante unidade = buildDomain();

            UnidadeSolicitanteEntity entity = UnidadeSolicitanteEntity.fromDomain(unidade);

            assertThat(entity.getId()).isEqualTo(unidade.getId());
            assertThat(entity.getCnes()).isEqualTo(unidade.getCnes());
            assertThat(entity.getNome()).isEqualTo(unidade.getNome());
            assertThat(entity.getEndereco()).isEqualTo(unidade.getEndereco());
            assertThat(entity.getTelefone()).isEqualTo(unidade.getTelefone());
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("roundtrip fromDomain -> toDomain deve preservar todos os dados")
        void roundtripDevePreservarDados() {
            UnidadeSolicitante original = buildDomain();

            UnidadeSolicitante roundtrip = UnidadeSolicitanteEntity.fromDomain(original).toDomain();

            assertThat(roundtrip.getId()).isEqualTo(original.getId());
            assertThat(roundtrip.getCnes()).isEqualTo(original.getCnes());
            assertThat(roundtrip.getNome()).isEqualTo(original.getNome());
            assertThat(roundtrip.getEndereco()).isEqualTo(original.getEndereco());
            assertThat(roundtrip.getTelefone()).isEqualTo(original.getTelefone());
        }
    }
}