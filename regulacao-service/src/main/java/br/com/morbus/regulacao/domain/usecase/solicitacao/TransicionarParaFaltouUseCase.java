package br.com.morbus.regulacao.domain.usecase.solicitacao;

import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.ITransicionarParaFaltouUseCase;
import br.com.morbus.regulacao.ports.in.dto.AppointmentNoShowCommand;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransicionarParaFaltouUseCase implements ITransicionarParaFaltouUseCase {
    private final ISolicitacaoRepository solicitacaoRepository;

    public TransicionarParaFaltouUseCase(ISolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    @Override
    public void execute(AppointmentNoShowCommand appointmentNoShowCommand) {
        Solicitacao solicitacao;
        try {
            solicitacao = solicitacaoRepository.findById(appointmentNoShowCommand.solicitacaoId());
        } catch (SolicitacaoNaoEncontradaException e) {
            log.warn("appointment.no_show descartado: solicitacao {} nao encontrada (idempotencia)", appointmentNoShowCommand.solicitacaoId());
            return;
        }

        if (EStatusSolicitacao.FALTOU.equals(solicitacao.getStatus())) {
            log.info("appointment.no_show ignorado: solicitacao {} ja esta FALTOU (idempotencia)", appointmentNoShowCommand.solicitacaoId());
            return;
        }

        if(!EStatusSolicitacao.AGENDADA.equals(solicitacao.getStatus())) {
            log.warn("appointment.no_show descartado: solicitacao {} esta em status {} (esperado AGENDADA)", appointmentNoShowCommand.solicitacaoId(), solicitacao.getStatus());
            return;
        }

        solicitacao.registrarFalta();
        solicitacaoRepository.save(solicitacao);
    }
}
