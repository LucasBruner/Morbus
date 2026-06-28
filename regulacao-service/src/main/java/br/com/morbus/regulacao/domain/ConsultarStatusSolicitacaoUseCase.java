package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.dto.UsuarioContexto;
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
    public Solicitacao execute(UUID solicitacaoId, UsuarioContexto principal) {
        Solicitacao solicitacao = solicitacaoRepository.findById(solicitacaoId);
        if("ROLE_PACIENTE".equals(principal.role())
                && !principal.pacienteId().equals(solicitacao.getPacienteId()))
            throw new IdPacienteIncorretoException("Paciente não pode acessar essa solicitação.");

        return solicitacao;
    }
}
