package br.com.morbus.agendamento.adapter.in.graphql.dto;

import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.model.Provider;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.port.in.IConsultarGradeUseCase.GradeItem;

import java.util.UUID;

public record ScheduleResponseDTO(
        UUID id,
        String dayOfWeek,
        String startTime,
        String endTime,
        int slotDurationMin,
        int capacity,
        boolean active,
        HealthUnitDTO unit,
        ProviderDTO provider
) {

    public record HealthUnitDTO(UUID id, String cnes, String nome, String address, String phone) {}

    public record ProviderDTO(UUID id, String nome, String crm, String especialidade) {}

    public static ScheduleResponseDTO fromGradeItem(GradeItem item) {
        return from(item.schedule(), item.unit(), item.provider());
    }

    public static ScheduleResponseDTO from(Schedule sc, HealthUnit hu, Provider p) {
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

        return new ScheduleResponseDTO(
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
}
