package br.com.morbus.agendamento.adapter.in.graphql.dto;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.AgendamentoComDetalhes;
import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.model.Provider;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentosPacienteResponseDTO(UUID id,
                                              EStatusAgendamento status,
                                              String expiresAt,
                                              String cancellationReason,
                                              String createdAt,
                                              AgendamentoDetalheResponseDTO.SlotDTO slot) {

    public static AgendamentosPacienteResponseDTO fromDetalhe(AgendamentoComDetalhes detalhe) {
        Agendamento a = detalhe.agendamento();
        Slot s = detalhe.slot();
        Schedule sc = detalhe.schedule();
        HealthUnit hu = detalhe.unit();
        Provider p = detalhe.provider();

        AgendamentoDetalheResponseDTO.HealthUnitDTO unitDTO = new AgendamentoDetalheResponseDTO.HealthUnitDTO(
                hu.getId(),
                hu.getCnes(),
                hu.getNome(),
                hu.getMunicipio() + " - " + hu.getUf(),
                hu.getTelefone()
        );

        AgendamentoDetalheResponseDTO.ProviderDTO providerDTO = p != null
                ? new AgendamentoDetalheResponseDTO.ProviderDTO(p.getId(), p.getNome(), p.getCrm(), p.getEspecialidade())
                : null;

        AgendamentoDetalheResponseDTO.ScheduleDTO scheduleDTO = new AgendamentoDetalheResponseDTO.ScheduleDTO(
                sc.getId(),
                sc.getDiaDaSemana().name(),
                sc.getHorarioInicio().toString(),
                sc.getHorarioFim().toString(),
                sc.getSlotDuracaoMinutos(),
                sc.getCapacidade(),
                sc.isAtivo(),
                unitDTO,
                providerDTO
        );

        AgendamentoDetalheResponseDTO.SlotDTO slotDTO = new AgendamentoDetalheResponseDTO.SlotDTO(
                s.getId(),
                s.getDataHora().toString(),
                s.getCapacidade(),
                s.getReservados(),
                s.getCapacidade() - s.getReservados(),
                s.getStatus(),
                scheduleDTO
        );

        return new AgendamentosPacienteResponseDTO(
                a.getId(),
                a.getStatus(),
                formatDateTime(a.getExpiresAt()),
                a.getCancellationReason(),
                formatDateTime(a.getCreatedAt()),
                slotDTO
        );
    }

    private static String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.toString() : null;
    }
}
