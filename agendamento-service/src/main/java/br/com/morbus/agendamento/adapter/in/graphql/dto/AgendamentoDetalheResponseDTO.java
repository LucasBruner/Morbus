package br.com.morbus.agendamento.adapter.in.graphql.dto;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.AgendamentoComDetalhes;
import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.model.Provider;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoDetalheResponseDTO(
        UUID id,
        EStatusAgendamento status,
        String expiresAt,
        String confirmedAt,
        String attendedAt,
        String noShowAt,
        String cancellationReason,
        String createdAt,
        SlotDTO slot
) {

    public record SlotDTO(
            UUID id,
            String dataHora,
            int capacity,
            int booked,
            int remainingCapacity,
            EStatusSlots status,
            ScheduleDTO schedule
    ) {}

    public record ScheduleDTO(
            UUID id,
            String dayOfWeek,
            String startTime,
            String endTime,
            int slotDurationMin,
            int capacity,
            boolean active,
            HealthUnitDTO unit,
            ProviderDTO provider
    ) {}

    public record HealthUnitDTO(
            UUID id,
            String cnes,
            String nome,
            String address,
            String phone
    ) {}

    public record ProviderDTO(
            UUID id,
            String nome,
            String crm,
            String especialidade
    ) {}

    public static AgendamentoDetalheResponseDTO fromDetalhe(AgendamentoComDetalhes detalhe) {
        Agendamento a = detalhe.agendamento();
        Slot s = detalhe.slot();
        ScheduleDTO scheduleDTO = getScheduleDTO(detalhe);

        SlotDTO slotDTO = new SlotDTO(
                s.getId(),
                s.getDataHora().toString(),
                s.getCapacidade(),
                s.getReservados(),
                s.getCapacidade() - s.getReservados(),
                s.getStatus(),
                scheduleDTO
        );

        return new AgendamentoDetalheResponseDTO(
                a.getId(),
                a.getStatus(),
                formatDateTime(a.getExpiresAt()),
                formatDateTime(a.getConfirmedAt()),
                formatDateTime(a.getAttendedAt()),
                formatDateTime(a.getNoShowAt()),
                a.getCancellationReason(),
                formatDateTime(a.getCreatedAt()),
                slotDTO
        );
    }

    private static ScheduleDTO getScheduleDTO(AgendamentoComDetalhes detalhe) {
        Schedule sc = detalhe.schedule();
        HealthUnit hu = detalhe.unit();
        Provider p = detalhe.provider();

        HealthUnitDTO unitDTO = new HealthUnitDTO(
                hu.getId(),
                hu.getCnes(),
                hu.getNome(),
                hu.getMunicipio() + " - " + hu.getUf(),
                hu.getTelefone()
        );

        ProviderDTO providerDTO = p != null
                ? new ProviderDTO(p.getId(), p.getNome(), p.getCrm(), p.getEspecialidade())
                : null;

        return new ScheduleDTO(
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
    }

    private static String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.toString() : null;
    }
}
