package br.com.morbus.regulacao.domain.usecase.solicitacao;

import br.com.morbus.regulacao.domain.enums.EDecisaoRegulador;
import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.CampoObrigatorioException;
import br.com.morbus.regulacao.domain.exception.CotaExcedidaException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.model.Parecer;
import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.IAvaliarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.dto.AvaliarSolicitacaoCommand;
import br.com.morbus.regulacao.ports.in.dto.AvaliarSolicitacaoResult;
import br.com.morbus.regulacao.ports.out.IParecerRepository;
import br.com.morbus.regulacao.ports.out.IQuotaRepository;
import br.com.morbus.regulacao.ports.out.IRegulacaoEventPublisher;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Set;

@Slf4j
public class AvaliarSolicitacaoUseCase implements IAvaliarSolicitacaoUseCase {

    private static final Set<EStatusSolicitacao> STATUS_PERMITIDO_AUTORIZAR_OU_FILA_ESPERA =
            Set.of(EStatusSolicitacao.AGUARDANDO, EStatusSolicitacao.PENDENTE);

    private final ISolicitacaoRepository solicitacaoRepository;
    private final IParecerRepository parecerRepository;
    private final IRegulacaoEventPublisher eventPublisher;
    private final IQuotaRepository quotaRepository;

    public AvaliarSolicitacaoUseCase(ISolicitacaoRepository solicitacaoRepository,
                                     IParecerRepository parecerRepository,
                                     IRegulacaoEventPublisher eventPublisher,
                                     IQuotaRepository quotaRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.parecerRepository = parecerRepository;
        this.eventPublisher = eventPublisher;
        this.quotaRepository = quotaRepository;
    }

    @Override
    public AvaliarSolicitacaoResult execute(AvaliarSolicitacaoCommand command) {
        Solicitacao solicitacao = solicitacaoRepository.findById(command.solicitacaoId());

        validarStatusPermitido(solicitacao.getStatus(), command.decisao());
        validarCamposObrigatorios(command);

        aplicarDecisao(solicitacao, command);

        Solicitacao solicitacaoSalva = solicitacaoRepository.save(solicitacao);
        Parecer parecer = parecerRepository.save(
                new Parecer(solicitacaoSalva.getId(), command.reguladorId(), command.decisao(), command.justificativa()));

        publicarEvento(solicitacaoSalva, command.decisao());

        return new AvaliarSolicitacaoResult(parecer, solicitacaoSalva.getStatus());
    }

    private void aplicarDecisao(Solicitacao solicitacao, AvaliarSolicitacaoCommand command) {
        switch (command.decisao()) {
            case AUTORIZAR -> solicitacao.aprovar(command.riskColorDefinido(), command.unidadeExecutanteId());
            case FILA_ESPERA -> {
                enforceQuotaSeNecessario(solicitacao);
                solicitacao.aprovarParaFilaEspera(command.unidadeExecutanteId());
            }
            case NEGAR -> solicitacao.negar(command.justificativa());
            case DEVOLVER -> solicitacao.devolver(command.justificativa());
            case PENDENTE -> solicitacao.marcarPendente();
        }
    }

    /**
     * Solicitacoes criadas com destino=FILA_ESPERA ja tiveram a cota verificada/consumida
     * em CriarSolicitacaoUseCase. Aqui so precisamos aplicar o enforcement quando o regulador
     * esta redirecionando uma solicitacao originalmente FILA_REGULADA para FILA_ESPERA — caminho
     * que hoje contorna a cota da UBS por completo.
     */
    private void enforceQuotaSeNecessario(Solicitacao solicitacao) {
        if (solicitacao.getDestino() == EDestino.FILA_ESPERA) {
            return;
        }

        LocalDate periodStart = LocalDate.now().withDayOfMonth(1);
        Quota quota = quotaRepository.findOrCreate(
                solicitacao.getUnidadeSolicitanteId(), solicitacao.getProcedureId(), periodStart);

        if (!quotaRepository.incrementarSeDisponivel(quota.getId())) {
            throw new CotaExcedidaException("Cota esgotada para unitId=%s, procedureId=%s no periodo %s"
                    .formatted(solicitacao.getUnidadeSolicitanteId(), solicitacao.getProcedureId(), periodStart));
        }
    }

    private void publicarEvento(Solicitacao solicitacao, EDecisaoRegulador decisao) {
        switch (decisao) {
            case AUTORIZAR, FILA_ESPERA -> eventPublisher.publishSolicitacaoAprovada(solicitacao);
            case DEVOLVER -> eventPublisher.publishSolicitacaoDevolvida(solicitacao);
            case NEGAR -> publicarNegacaoSemFalhar(solicitacao);
            case PENDENTE -> { /* sem evento: aguarda abertura de cota */ }
        }
    }

    private void publicarNegacaoSemFalhar(Solicitacao solicitacao) {
        try {
            eventPublisher.publishSolicitacaoNegada(solicitacao);
        } catch (Exception e) {
            log.error("Falha ao publicar solicitation.denied para a solicitacao {}", solicitacao.getId(), e);
        }
    }

    private void validarStatusPermitido(EStatusSolicitacao status, EDecisaoRegulador decisao) {
        boolean permitido = switch (decisao) {
            case AUTORIZAR, FILA_ESPERA -> STATUS_PERMITIDO_AUTORIZAR_OU_FILA_ESPERA.contains(status);
            case NEGAR, DEVOLVER, PENDENTE -> status == EStatusSolicitacao.AGUARDANDO;
        };

        if (!permitido) {
            throw new SolicitacaoNaoPendenteException(
                    "A solicitacao informada nao esta em um status que permite a decisao " + decisao + ".");
        }
    }

    private void validarCamposObrigatorios(AvaliarSolicitacaoCommand command) {
        boolean exigeRiskColor = command.decisao() == EDecisaoRegulador.AUTORIZAR
                || command.decisao() == EDecisaoRegulador.FILA_ESPERA;

        if (exigeRiskColor && command.riskColorDefinido() == null) {
            throw new CampoObrigatorioException(
                    "riskColorDefinido e obrigatorio quando decisao = " + command.decisao() + ".");
        }

        if (exigeRiskColor && command.unidadeExecutanteId() == null) {
            throw new CampoObrigatorioException(
                    "unidadeExecutanteId e obrigatorio quando decisao = " + command.decisao() + ".");
        }

        boolean exigeJustificativa = command.decisao() == EDecisaoRegulador.NEGAR
                || command.decisao() == EDecisaoRegulador.DEVOLVER;

        if (exigeJustificativa && (command.justificativa() == null || command.justificativa().isBlank())) {
            throw new CampoObrigatorioException(
                    "justificativa e obrigatoria quando decisao = " + command.decisao() + ".");
        }
    }
}
