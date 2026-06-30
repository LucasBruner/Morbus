package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.adapters.out.jpa.parecer.IParecerJpaRepository;
import br.com.morbus.regulacao.adapters.out.jpa.parecer.ParecerEntity;
import br.com.morbus.regulacao.adapters.out.jpa.parecer.ParecerJpaAdapter;
import br.com.morbus.regulacao.domain.enums.EDecisaoRegulador;
import br.com.morbus.regulacao.domain.model.Parecer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParecerJpaAdapter")
class ParecerJpaAdapterTest {

    @Mock
    private IParecerJpaRepository jpaRepository;

    private ParecerJpaAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ParecerJpaAdapter(jpaRepository);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve persistir e retornar a entidade convertida para dominio")
        void devePersistirERetornar() {
            Parecer domain = new Parecer(UUID.randomUUID(), UUID.randomUUID(), EDecisaoRegulador.NEGAR, "sem indicacao");
            ParecerEntity entity = ParecerEntity.fromDomain(domain);
            when(jpaRepository.save(any())).thenReturn(entity);

            Parecer result = adapter.save(domain);

            assertThat(result.getId()).isEqualTo(domain.getId());
            assertThat(result.getDecisao()).isEqualTo(EDecisaoRegulador.NEGAR);
            verify(jpaRepository).save(any(ParecerEntity.class));
        }
    }
}
