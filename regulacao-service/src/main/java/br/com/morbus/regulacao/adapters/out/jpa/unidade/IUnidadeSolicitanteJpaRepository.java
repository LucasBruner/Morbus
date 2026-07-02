package br.com.morbus.regulacao.adapters.out.jpa.unidade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IUnidadeSolicitanteJpaRepository extends JpaRepository<UnidadeSolicitanteEntity, UUID> {
    boolean existsByCnes(String cnes);
}