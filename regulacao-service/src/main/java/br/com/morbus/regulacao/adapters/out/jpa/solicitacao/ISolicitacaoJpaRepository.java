package br.com.morbus.regulacao.adapters.out.jpa.solicitacao;

import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ISolicitacaoJpaRepository extends JpaRepository<SolicitacaoEntity, UUID>, JpaSpecificationExecutor<SolicitacaoEntity> {

    boolean existsByPatientIdAndProcedureIdAndStatusIn(UUID patientId,
                                                       UUID procedureId,
                                                       List<EStatusSolicitacao> statuses);
}
