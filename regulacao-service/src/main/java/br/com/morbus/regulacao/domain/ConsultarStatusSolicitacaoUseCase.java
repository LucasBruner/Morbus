package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.adapters.security.UserPrincipal;
import br.com.morbus.regulacao.domain.exception.IdPacienteIncorretoException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.IConsultarStatusSolicitacao;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;

import java.util.UUID;

public class ConsultarStatusSolicitacaoUseCase implements IConsultarStatusSolicitacao {

    private final ISolicitacaoRepository solicitacaoRepository;

    public ConsultarStatusSolicitacaoUseCase(ISolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    @Override
    public Solicitacao execute(UUID solicitacaoId, UserPrincipal principal) {
        Solicitacao solicitacao = solicitacaoRepository.findById(solicitacaoId);
        if("PACIENTE".equals(principal.role())
                && principal.userId().equals(solicitacao.getPacienteId()))
            throw new IdPacienteIncorretoException("Paciente não pode acessar essa solicitação.");

        return solicitacao;
    }
}
