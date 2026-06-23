package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ISolicitacaoJpaRepository extends JpaRepository<SolicitacaoEntity, UUID> {

    boolean existsByPacienteIdAndProcedureIdAndStatusIn(UUID pacienteId,
                                                        UUID procedureId,
                                                        List<EStatusSolicitacao> statuses);
}
