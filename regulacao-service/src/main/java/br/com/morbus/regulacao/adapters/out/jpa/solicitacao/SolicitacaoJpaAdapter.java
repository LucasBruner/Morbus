package br.com.morbus.regulacao.adapters.out.jpa.solicitacao;

import br.com.morbus.regulacao.domain.dto.ESortDirection;
import br.com.morbus.regulacao.domain.dto.ListarSolicitacoesQuery;
import br.com.morbus.regulacao.domain.dto.PageResult;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    public Solicitacao findById(UUID solicitacaoId) {
        return jpaRepository.findById(solicitacaoId)
                .orElseThrow(() -> new SolicitacaoNaoEncontradaException("Id informado nao possui nenhuma solicitacao.")).toDomain();
    }

    @Override
    public boolean existsAtiva(UUID patientId, UUID procedureId) {
        return jpaRepository.existsByPatientIdAndProcedureIdAndStatusIn(
                patientId,
                procedureId,
                List.of(EStatusSolicitacao.AGUARDANDO, EStatusSolicitacao.APROVADA)
        );
    }

    @Override
    public Solicitacao save(Solicitacao solicitacao) {
        SolicitacaoEntity entity = SolicitacaoEntity.fromDomain(solicitacao);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public PageResult<Solicitacao> listar(ListarSolicitacoesQuery query) {
        Specification<SolicitacaoEntity> spec = (r, cq, cb) -> cb.conjunction();
        if (query.unidadeId() != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("unidadeSolicitanteId"), query.unidadeId()));
        }

        if (query.status() != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("status"), query.status()));
        }

        if (query.destino() != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("destino"), query.destino()));
        }

        if (query.procedureId() != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("procedureId"), query.procedureId()));
        }

        Sort.Direction direction = query.sortDirection() == ESortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(query.page(), query.size(), Sort.by(direction, "createdAt"));
        Page<Solicitacao> page = jpaRepository.findAll(spec, pageable).map(SolicitacaoEntity::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
