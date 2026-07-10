package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterQueueRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddToQueue")
class AddToQueueTest {

    @Mock
    private RegisterPatientInQueue registerPatientInQueue;

    private AddToQueue useCase;

    private UUID patientId;
    private UUID procedureId;

    @BeforeEach
    void setUp() {
        useCase = AddToQueue.create(registerPatientInQueue);
        patientId = UUID.randomUUID();
        procedureId = UUID.randomUUID();
    }

    private QueueEntry buildQueueEntry() {
        return QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(Patient.builder().id(patientId).build())
                .procedure(Procedure.builder().id(procedureId).build())
                .queueStatus(EQueueStatus.AGUARDANDO)
                .build();
    }

    @Test
    @DisplayName("FILA_REGULADA repassa a cor de risco do evento")
    void filaRegulada_repassaRiskColor() {
        QueueEntry expected = buildQueueEntry();
        when(registerPatientInQueue.execute(any(RegisterQueueRequestDTO.class))).thenReturn(expected);

        QueueEntry result = useCase.execute(patientId, procedureId, EDestino.FILA_REGULADA, ERiskColor.VERMELHO, null, null);

        ArgumentCaptor<RegisterQueueRequestDTO> captor = ArgumentCaptor.forClass(RegisterQueueRequestDTO.class);
        verify(registerPatientInQueue).execute(captor.capture());
        assertThat(captor.getValue().patientId()).isEqualTo(patientId);
        assertThat(captor.getValue().procedureId()).isEqualTo(procedureId);
        assertThat(captor.getValue().riskColor()).isEqualTo(ERiskColor.VERMELHO);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("FILA_ESPERA força AZUL e propaga tipoFila=FILA_ESPERA para o repositório")
    void filaEspera_forcaAzul_ePropagatTipoFila() {
        QueueEntry expected = buildQueueEntry();
        when(registerPatientInQueue.execute(any(RegisterQueueRequestDTO.class))).thenReturn(expected);

        useCase.execute(patientId, procedureId, EDestino.FILA_ESPERA, ERiskColor.VERMELHO, null, null);

        ArgumentCaptor<RegisterQueueRequestDTO> captor = ArgumentCaptor.forClass(RegisterQueueRequestDTO.class);
        verify(registerPatientInQueue).execute(captor.capture());
        assertThat(captor.getValue().riskColor()).isEqualTo(ERiskColor.AZUL);
        assertThat(captor.getValue().tipoFila()).isEqualTo(EDestino.FILA_ESPERA);
    }
}
