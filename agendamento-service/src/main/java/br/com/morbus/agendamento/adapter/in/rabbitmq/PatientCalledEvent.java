package br.com.morbus.agendamento.adapter.in.rabbitmq;

import java.time.Instant;
import java.util.UUID;

public record PatientCalledEvent(
        String eventType,
        UUID queueEntryId,
        UUID pacienteId,
        UUID slotId,
        String patientName,
        String patientContact,
        String procedureName,
        UUID procedureId,
        UUID preferredUnitId,
        String riskColor,
        String tipoFila,
        Instant timestamp
) {
}
