package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.adapters.out.jpa.parecer.ParecerEntity;
import br.com.morbus.regulacao.domain.enums.EDecisaoRegulador;
import br.com.morbus.regulacao.domain.model.Parecer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ParecerEntity")
class ParecerEntityTest {

    private Parecer buildDomain() {
        return new Parecer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                EDecisaoRegulador.AUTORIZAR, "justificativa", LocalDateTime.now().minusHours(1));
    }

    @Nested
    @DisplayName("fromDomain")
    class FromDomain {

        @Test
        @DisplayName("deve mapear todos os campos do dominio para a entidade")
        void deveMapeiarTodosOsCampos() {
            Parecer p = buildDomain();
            ParecerEntity entity = ParecerEntity.fromDomain(p);

            assertThat(entity.getId()).isEqualTo(p.getId());
            assertThat(entity.getSolicitacaoId()).isEqualTo(p.getSolicitacaoId());
            assertThat(entity.getReguladorId()).isEqualTo(p.getReguladorId());
            assertThat(entity.getDecisao()).isEqualTo(p.getDecisao());
            assertThat(entity.getJustificativa()).isEqualTo(p.getJustificativa());
            assertThat(entity.getEmitidoEm()).isEqualTo(p.getEmitidoEm());
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("roundtrip fromDomain -> toDomain deve preservar todos os dados")
        void roundtripDevePreservarDados() {
            Parecer original = buildDomain();
            Parecer roundtrip = ParecerEntity.fromDomain(original).toDomain();

            assertThat(roundtrip.getId()).isEqualTo(original.getId());
            assertThat(roundtrip.getSolicitacaoId()).isEqualTo(original.getSolicitacaoId());
            assertThat(roundtrip.getReguladorId()).isEqualTo(original.getReguladorId());
            assertThat(roundtrip.getDecisao()).isEqualTo(original.getDecisao());
            assertThat(roundtrip.getJustificativa()).isEqualTo(original.getJustificativa());
            assertThat(roundtrip.getEmitidoEm()).isEqualTo(original.getEmitidoEm());
        }
    }
}
