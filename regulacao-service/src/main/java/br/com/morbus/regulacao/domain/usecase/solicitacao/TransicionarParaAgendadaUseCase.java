package br.com.morbus.regulacao.domain.usecase.solicitacao;

import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.ITransicionarParaAgendadaUseCase;
import br.com.morbus.regulacao.ports.in.dto.AppointmentCreatedCommand;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class TransicionarParaAgendadaUseCase implements ITransicionarParaAgendadaUseCase {

    ISolicitacaoRepository solicitacaoRepository;

    public TransicionarParaAgendadaUseCase(ISolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    @Override
    @Transactional
    public void execute(AppointmentCreatedCommand appointmentCreatedCommand) {
        Solicitacao solicitacao;

        try{
            solicitacao = solicitacaoRepository.findById(appointmentCreatedCommand.solicitacaoId());
        } catch (SolicitacaoNaoEncontradaException e) {
            log.warn("appointment.created descartado: solicitacao {} nao encontrada (idempotencia)",
                    appointmentCreatedCommand.solicitacaoId());
            return;
        }
        if(!EStatusSolicitacao.APROVADA.equals(solicitacao.getStatus())) {
            log.warn("appointment.created descartado: solicitacao {} esta em status {} (esperado APROVADA)",
                    appointmentCreatedCommand.solicitacaoId(), solicitacao.getStatus());
            return;
        }

        solicitacao.agendar(appointmentCreatedCommand.appointmentId());
        solicitacaoRepository.save(solicitacao);
    }
}
