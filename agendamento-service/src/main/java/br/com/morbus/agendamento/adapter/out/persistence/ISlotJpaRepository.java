package br.com.morbus.agendamento.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ISlotJpaRepository extends JpaRepository<SlotEntity, UUID> {
}
