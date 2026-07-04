package br.com.morbus.regulacao.domain.usecase.solicitacao;

import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.ITransicionarParaAtendidaUseCase;
import br.com.morbus.regulacao.ports.in.dto.AppointmentAttendedCommand;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class TransicionarParaAtendidaUseCase implements ITransicionarParaAtendidaUseCase {
    private final ISolicitacaoRepository solicitacaoRepository;

    public TransicionarParaAtendidaUseCase(ISolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }


    @Override
    @Transactional
    public void execute(AppointmentAttendedCommand appointmentAttendedCommand) {
        Solicitacao solicitacao;
        try {
            solicitacao = solicitacaoRepository.findById(appointmentAttendedCommand.solicitacaoId());
        } catch (SolicitacaoNaoEncontradaException e) {
            log.warn("appointment.attended descartado: solicitacao {} nao encontrada (idempotencia)", appointmentAttendedCommand.solicitacaoId());
            return;
        }

        if (EStatusSolicitacao.ATENDIDA.equals(solicitacao.getStatus())) {
            log.info("appointment.attended ignorado: solicitacao {} ja esta ATENDIDA (idempotencia)", appointmentAttendedCommand.solicitacaoId());
            return;
        }

        if(!EStatusSolicitacao.AGENDADA.equals(solicitacao.getStatus())) {
            log.warn("appointment.attended descartado: solicitacao {} esta em status {} (esperado AGENDADA)", appointmentAttendedCommand.solicitacaoId(), solicitacao.getStatus());
            return;
        }

        solicitacao.atender();
        solicitacaoRepository.save(solicitacao);
    }
}
