package br.com.morbus.regulacao.adapters.out.jpa.parecer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IParecerJpaRepository extends JpaRepository<ParecerEntity, UUID> {
}
