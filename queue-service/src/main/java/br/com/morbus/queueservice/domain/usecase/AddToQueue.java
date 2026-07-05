package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterQueueRequestDTO;

import java.util.UUID;

public class AddToQueue {

    private final RegisterPatientInQueue registerPatientInQueue;

    private AddToQueue(RegisterPatientInQueue registerPatientInQueue) {
        this.registerPatientInQueue = registerPatientInQueue;
    }

    public static AddToQueue create(RegisterPatientInQueue registerPatientInQueue) {
        return new AddToQueue(registerPatientInQueue);
    }

    public QueueEntry execute(UUID patientId, UUID procedureId, EDestino tipoFila, ERiskColor riskColor) {
        ERiskColor effectiveRiskColor = tipoFila == EDestino.FILA_ESPERA ? ERiskColor.AZUL : riskColor;
        return registerPatientInQueue.execute(
                new RegisterQueueRequestDTO(patientId, procedureId, effectiveRiskColor));
    }
}
