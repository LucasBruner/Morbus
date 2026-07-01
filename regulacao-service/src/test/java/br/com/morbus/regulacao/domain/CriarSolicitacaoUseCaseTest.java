package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.CotaExcedidaException;
import br.com.morbus.regulacao.domain.exception.DuplicateSolicitacaoException;
import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.domain.usecase.solicitacao.CriarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.dto.CriarSolicitacaoCommand;
import br.com.morbus.regulacao.ports.out.IQuotaRepository;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CriarSolicitacaoUseCase")
class CriarSolicitacaoUseCaseTest {

    @Mock
    private ISolicitacaoRepository repository;

    @Mock
    private IQuotaRepository quotaRepository;

    private CriarSolicitacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CriarSolicitacaoUseCase(repository, quotaRepository);
    }

    private CriarSolicitacaoCommand buildCommand(EDestino destino) {
        return new CriarSolicitacaoCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "I10",
                "Paciente com hipertensao grave",
                "Dr. Silva",
                "CRM/SP 12345",
                destino,
                UUID.randomUUID()
        );
    }

    private CriarSolicitacaoCommand buildCommand() {
        return buildCommand(EDestino.FILA_REGULADA);
    }

    private Solicitacao buildSolicitacaoSalva(CriarSolicitacaoCommand cmd) {
        return new Solicitacao(
                cmd.patientId(),
                cmd.procedureId(),
                cmd.unidadeSolicitanteId(),
                cmd.cid(),
                cmd.justificativaClinica(),
                cmd.profissionalSolicitante(),
                cmd.crmProfissional(),
                cmd.destino(),
                cmd.solicitadoPor()
        );
    }

    private Quota buildQuota(UUID unitId, UUID procedureId, LocalDate periodStart, int maxPerPeriod, int currentCount) {
        return new Quota(UUID.randomUUID(), unitId, procedureId, maxPerPeriod, currentCount, periodStart);
    }

    @Nested
    @DisplayName("quando nao existe solicitacao ativa")
    class QuandoNaoExisteDuplicata {

        @Test
        @DisplayName("deve retornar a solicitacao com status AGUARDANDO")
        void deveRetornarComStatusAguardando() {
            CriarSolicitacaoCommand cmd = buildCommand();
            Solicitacao salva = buildSolicitacaoSalva(cmd);
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(salva);

            Solicitacao result = useCase.execute(cmd);

            assertThat(result.getStatus()).isEqualTo(EStatusSolicitacao.AGUARDANDO);
        }

        @Test
        @DisplayName("deve persistir a solicitacao no repositorio")
        void devePersistir() {
            CriarSolicitacaoCommand cmd = buildCommand();
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(buildSolicitacaoSalva(cmd));

            useCase.execute(cmd);

            verify(repository).save(any(Solicitacao.class));
        }

        @Test
        @DisplayName("deve retornar a entidade devolvida pelo repositorio")
        void deveRetornarEntidadeDoRepositorio() {
            CriarSolicitacaoCommand cmd = buildCommand();
            Solicitacao salva = buildSolicitacaoSalva(cmd);
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(salva);

            Solicitacao result = useCase.execute(cmd);

            assertThat(result).isSameAs(salva);
        }

        @Test
        @DisplayName("deve verificar duplicata antes de salvar")
        void deveVerificarDuplicataAntesDeSalvar() {
            CriarSolicitacaoCommand cmd = buildCommand();
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(buildSolicitacaoSalva(cmd));

            useCase.execute(cmd);

            var order = inOrder(repository);
            order.verify(repository).existsAtiva(cmd.patientId(), cmd.procedureId());
            order.verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("quando ja existe solicitacao AGUARDANDO ou APROVADA")
    class QuandoExisteDuplicata {

        @Test
        @DisplayName("deve lancar DuplicateSolicitacaoException")
        void deveLancarDuplicateSolicitacaoException() {
            CriarSolicitacaoCommand cmd = buildCommand();
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(true);

            assertThatThrownBy(() -> useCase.execute(cmd))
                    .isInstanceOf(DuplicateSolicitacaoException.class);
        }

        @Test
        @DisplayName("nao deve persistir quando ha duplicata")
        void naoDevePersistir() {
            CriarSolicitacaoCommand cmd = buildCommand();
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(true);

            assertThatThrownBy(() -> useCase.execute(cmd))
                    .isInstanceOf(DuplicateSolicitacaoException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("nao deve verificar cota quando ha duplicata")
        void naoDeveVerificarCotaQuandoHaDuplicata() {
            CriarSolicitacaoCommand cmd = buildCommand(EDestino.FILA_ESPERA);
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(true);

            assertThatThrownBy(() -> useCase.execute(cmd))
                    .isInstanceOf(DuplicateSolicitacaoException.class);

            verifyNoInteractions(quotaRepository);
        }
    }

    @Nested
    @DisplayName("quando destino e FILA_REGULADA")
    class QuandoDestinoFilaRegulada {

        @Test
        @DisplayName("nao deve consultar nem incrementar cota")
        void naoDeveConsultarCota() {
            CriarSolicitacaoCommand cmd = buildCommand(EDestino.FILA_REGULADA);
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(buildSolicitacaoSalva(cmd));

            useCase.execute(cmd);

            verifyNoInteractions(quotaRepository);
        }

        @Test
        @DisplayName("deve criar a solicitacao normalmente")
        void deveCriarNormalmente() {
            CriarSolicitacaoCommand cmd = buildCommand(EDestino.FILA_REGULADA);
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(buildSolicitacaoSalva(cmd));

            Solicitacao result = useCase.execute(cmd);

            assertThat(result.getDestino()).isEqualTo(EDestino.FILA_REGULADA);
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("quando destino e FILA_ESPERA")
    class QuandoDestinoFilaEspera {

        @Test
        @DisplayName("deve buscar/criar cota do periodo corrente para a unidade e procedimento")
        void deveBuscarCotaDoPeriodoCorrente() {
            CriarSolicitacaoCommand cmd = buildCommand(EDestino.FILA_ESPERA);
            LocalDate periodoEsperado = LocalDate.now().withDayOfMonth(1);
            Quota quota = buildQuota(cmd.unidadeSolicitanteId(), cmd.procedureId(), periodoEsperado, 5, 2);
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(quotaRepository.findOrCreate(cmd.unidadeSolicitanteId(), cmd.procedureId(), periodoEsperado))
                    .thenReturn(quota);
            when(quotaRepository.incrementarSeDisponivel(quota.getId())).thenReturn(true);
            when(repository.save(any())).thenReturn(buildSolicitacaoSalva(cmd));

            useCase.execute(cmd);

            verify(quotaRepository).findOrCreate(cmd.unidadeSolicitanteId(), cmd.procedureId(), periodoEsperado);
        }

        @Test
        @DisplayName("deve criar a solicitacao quando ha cota disponivel")
        void deveCriarQuandoCotaDisponivel() {
            CriarSolicitacaoCommand cmd = buildCommand(EDestino.FILA_ESPERA);
            Quota quota = buildQuota(cmd.unidadeSolicitanteId(), cmd.procedureId(),
                    LocalDate.now().withDayOfMonth(1), 5, 2);
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(quotaRepository.findOrCreate(any(), any(), any())).thenReturn(quota);
            when(quotaRepository.incrementarSeDisponivel(quota.getId())).thenReturn(true);
            when(repository.save(any())).thenReturn(buildSolicitacaoSalva(cmd));

            Solicitacao result = useCase.execute(cmd);

            assertThat(result.getStatus()).isEqualTo(EStatusSolicitacao.AGUARDANDO);
            verify(repository).save(any());
        }

        @Test
        @DisplayName("deve lancar CotaExcedidaException quando cota esgotada")
        void deveLancarCotaExcedidaQuandoEsgotada() {
            CriarSolicitacaoCommand cmd = buildCommand(EDestino.FILA_ESPERA);
            Quota quota = buildQuota(cmd.unidadeSolicitanteId(), cmd.procedureId(),
                    LocalDate.now().withDayOfMonth(1), 5, 5);
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(quotaRepository.findOrCreate(any(), any(), any())).thenReturn(quota);
            when(quotaRepository.incrementarSeDisponivel(quota.getId())).thenReturn(false);

            assertThatThrownBy(() -> useCase.execute(cmd))
                    .isInstanceOf(CotaExcedidaException.class);
        }

        @Test
        @DisplayName("nao deve persistir a solicitacao quando cota esgotada")
        void naoDevePersistirQuandoCotaEsgotada() {
            CriarSolicitacaoCommand cmd = buildCommand(EDestino.FILA_ESPERA);
            Quota quota = buildQuota(cmd.unidadeSolicitanteId(), cmd.procedureId(),
                    LocalDate.now().withDayOfMonth(1), 5, 5);
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(quotaRepository.findOrCreate(any(), any(), any())).thenReturn(quota);
            when(quotaRepository.incrementarSeDisponivel(quota.getId())).thenReturn(false);

            assertThatThrownBy(() -> useCase.execute(cmd))
                    .isInstanceOf(CotaExcedidaException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar CotaExcedidaException quando nao existe registro de cota (bloqueada com max=0)")
        void deveLancarCotaExcedidaQuandoCotaInexistente() {
            CriarSolicitacaoCommand cmd = buildCommand(EDestino.FILA_ESPERA);
            Quota quotaBloqueada = Quota.bloqueada(cmd.unidadeSolicitanteId(), cmd.procedureId(),
                    LocalDate.now().withDayOfMonth(1));
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(quotaRepository.findOrCreate(any(), any(), any())).thenReturn(quotaBloqueada);
            when(quotaRepository.incrementarSeDisponivel(quotaBloqueada.getId())).thenReturn(false);

            assertThatThrownBy(() -> useCase.execute(cmd))
                    .isInstanceOf(CotaExcedidaException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("deve verificar cota antes de persistir a solicitacao")
        void deveVerificarCotaAntesDePersistir() {
            CriarSolicitacaoCommand cmd = buildCommand(EDestino.FILA_ESPERA);
            Quota quota = buildQuota(cmd.unidadeSolicitanteId(), cmd.procedureId(),
                    LocalDate.now().withDayOfMonth(1), 5, 2);
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(quotaRepository.findOrCreate(any(), any(), any())).thenReturn(quota);
            when(quotaRepository.incrementarSeDisponivel(quota.getId())).thenReturn(true);
            when(repository.save(any())).thenReturn(buildSolicitacaoSalva(cmd));

            useCase.execute(cmd);

            var order = inOrder(quotaRepository, repository);
            order.verify(quotaRepository).findOrCreate(any(), any(), any());
            order.verify(quotaRepository).incrementarSeDisponivel(quota.getId());
            order.verify(repository).save(any());
        }

        @Test
        @DisplayName("deve usar o primeiro dia do mes corrente como periodStart")
        void deveUsarPrimeiroDiaDoMesCorrente() {
            CriarSolicitacaoCommand cmd = buildCommand(EDestino.FILA_ESPERA);
            Quota quota = buildQuota(cmd.unidadeSolicitanteId(), cmd.procedureId(),
                    LocalDate.now().withDayOfMonth(1), 5, 2);
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(quotaRepository.findOrCreate(any(), any(), any())).thenReturn(quota);
            when(quotaRepository.incrementarSeDisponivel(quota.getId())).thenReturn(true);
            when(repository.save(any())).thenReturn(buildSolicitacaoSalva(cmd));

            useCase.execute(cmd);

            ArgumentCaptor<LocalDate> periodoCaptor = ArgumentCaptor.forClass(LocalDate.class);
            verify(quotaRepository).findOrCreate(any(), any(), periodoCaptor.capture());
            assertThat(periodoCaptor.getValue()).isEqualTo(LocalDate.now().withDayOfMonth(1));
        }
    }
}