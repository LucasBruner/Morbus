package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.domain.usecase.quota.GerenciarCotaUseCase;
import br.com.morbus.regulacao.ports.in.dto.GerenciarCotaCommand;
import br.com.morbus.regulacao.ports.out.IQuotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GerenciarCotaUseCase")
class GerenciarCotaUseCaseTest {

    @Mock
    private IQuotaRepository quotaRepository;

    private GerenciarCotaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GerenciarCotaUseCase(quotaRepository);
    }

    private GerenciarCotaCommand buildCommand(UUID unitId, UUID procedureId, int maxPerPeriod, LocalDate periodStart) {
        return new GerenciarCotaCommand(unitId, procedureId, maxPerPeriod, periodStart);
    }

    @Nested
    @DisplayName("quando nao existe cota para a chave (unitId, procedureId, periodStart)")
    class QuandoNaoExisteCota {

        @Test
        @DisplayName("deve criar uma nova cota com o maxPerPeriod informado")
        void deveCriarNovaCota() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);
            GerenciarCotaCommand cmd = buildCommand(unitId, procedureId, 10, periodStart);
            when(quotaRepository.buscarPorChave(unitId, procedureId, periodStart)).thenReturn(Optional.empty());
            when(quotaRepository.salvar(any(Quota.class))).thenAnswer(inv -> inv.getArgument(0));

            Quota result = useCase.execute(cmd);

            assertThat(result.getUnitId()).isEqualTo(unitId);
            assertThat(result.getProcedureId()).isEqualTo(procedureId);
            assertThat(result.getMaxPerPeriod()).isEqualTo(10);
            assertThat(result.getPeriodStart()).isEqualTo(periodStart);
        }

        @Test
        @DisplayName("a cota criada deve iniciar com currentCount igual a zero")
        void deveIniciarComCurrentCountZero() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);
            GerenciarCotaCommand cmd = buildCommand(unitId, procedureId, 10, periodStart);
            when(quotaRepository.buscarPorChave(unitId, procedureId, periodStart)).thenReturn(Optional.empty());
            when(quotaRepository.salvar(any(Quota.class))).thenAnswer(inv -> inv.getArgument(0));

            Quota result = useCase.execute(cmd);

            assertThat(result.getCurrentCount()).isZero();
        }

        @Test
        @DisplayName("deve persistir a nova cota via salvar")
        void devePersistirNovaCota() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);
            GerenciarCotaCommand cmd = buildCommand(unitId, procedureId, 10, periodStart);
            when(quotaRepository.buscarPorChave(unitId, procedureId, periodStart)).thenReturn(Optional.empty());
            when(quotaRepository.salvar(any(Quota.class))).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(cmd);

            ArgumentCaptor<Quota> captor = ArgumentCaptor.forClass(Quota.class);
            verify(quotaRepository).salvar(captor.capture());
            assertThat(captor.getValue().getMaxPerPeriod()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("quando ja existe cota para a chave (unitId, procedureId, periodStart)")
    class QuandoExisteCota {

        @Test
        @DisplayName("deve atualizar o maxPerPeriod da cota existente")
        void deveAtualizarMaxPerPeriod() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);
            Quota existente = new Quota(UUID.randomUUID(), unitId, procedureId, 5, 3, periodStart);
            GerenciarCotaCommand cmd = buildCommand(unitId, procedureId, 20, periodStart);
            when(quotaRepository.buscarPorChave(unitId, procedureId, periodStart)).thenReturn(Optional.of(existente));
            when(quotaRepository.salvar(any(Quota.class))).thenAnswer(inv -> inv.getArgument(0));

            Quota result = useCase.execute(cmd);

            assertThat(result.getMaxPerPeriod()).isEqualTo(20);
        }

        @Test
        @DisplayName("deve preservar o currentCount da cota existente")
        void devePreservarCurrentCount() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);
            Quota existente = new Quota(UUID.randomUUID(), unitId, procedureId, 5, 3, periodStart);
            GerenciarCotaCommand cmd = buildCommand(unitId, procedureId, 20, periodStart);
            when(quotaRepository.buscarPorChave(unitId, procedureId, periodStart)).thenReturn(Optional.of(existente));
            when(quotaRepository.salvar(any(Quota.class))).thenAnswer(inv -> inv.getArgument(0));

            Quota result = useCase.execute(cmd);

            assertThat(result.getCurrentCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("deve manter o mesmo id da cota existente")
        void deveManterMesmoId() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);
            Quota existente = new Quota(UUID.randomUUID(), unitId, procedureId, 5, 3, periodStart);
            GerenciarCotaCommand cmd = buildCommand(unitId, procedureId, 20, periodStart);
            when(quotaRepository.buscarPorChave(unitId, procedureId, periodStart)).thenReturn(Optional.of(existente));
            when(quotaRepository.salvar(any(Quota.class))).thenAnswer(inv -> inv.getArgument(0));

            Quota result = useCase.execute(cmd);

            assertThat(result.getId()).isEqualTo(existente.getId());
        }

        @Test
        @DisplayName("nao deve criar uma nova cota quando ja existe uma para a chave")
        void naoDeveCriarNovaCota() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);
            Quota existente = new Quota(UUID.randomUUID(), unitId, procedureId, 5, 3, periodStart);
            GerenciarCotaCommand cmd = buildCommand(unitId, procedureId, 20, periodStart);
            when(quotaRepository.buscarPorChave(unitId, procedureId, periodStart)).thenReturn(Optional.of(existente));
            when(quotaRepository.salvar(any(Quota.class))).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(cmd);

            verify(quotaRepository, times(1)).salvar(any());
        }
    }
}