package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class SolicitacaoJpaAdapter implements ISolicitacaoRepository {

    private final ISolicitacaoJpaRepository jpaRepository;

    public SolicitacaoJpaAdapter(ISolicitacaoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsAtiva(UUID pacienteId, UUID procedureId) {
        return jpaRepository.existsByPacienteIdAndProcedureIdAndStatusIn(
                pacienteId,
                procedureId,
                List.of(EStatusSolicitacao.PENDENTE, EStatusSolicitacao.APROVADA)
        );
    }

    @Override
    public Solicitacao save(Solicitacao solicitacao) {
        SolicitacaoEntity entity = SolicitacaoEntity.fromDomain(solicitacao);
        return jpaRepository.save(entity).toDomain();
    }
}
