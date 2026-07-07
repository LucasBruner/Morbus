package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentosPacienteUseCaseTest {

    @Mock
    private IAgendamentoRepository agendamentoRepository;

    private AgendamentosPacienteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AgendamentosPacienteUseCase(agendamentoRepository);
    }

    @Test
    void deveBuscarAgendamentosPorPacienteStatusEPeriodo() {
        UUID patientId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        EStatusAgendamento status = EStatusAgendamento.CONFIRMADO;
        List<Agendamento> esperado = List.of();

        LocalDateTime dateFrom = LocalDateTime.of(2026, Month.JULY, 1, 0, 0);
        LocalDateTime dateTo = LocalDateTime.of(2026, Month.JULY, 31, 23, 59, 59, 999999999);

        when(agendamentoRepository.findByPatientAndStatusAndDate(
                patientId,
                unitId,
                status,
                dateFrom,
                dateTo
        )).thenReturn(esperado);

        List<Agendamento> resultado = useCase.execute(
                patientId,
                unitId,
                status,
                "2026-07-01",
                "2026-07-31"
        );

        assertEquals(esperado, resultado);
        verify(agendamentoRepository).findByPatientAndStatusAndDate(
                patientId,
                unitId,
                status,
                dateFrom,
                dateTo
        );
    }

    @Test
    void deveFalharQuandoDateFromForMaiorQueDateTo() {
        UUID patientId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(patientId, null, null, "2026-07-10", "2026-07-01")
        );

        assertEquals("dateFrom deve ser anterior ou igual a dateTo", exception.getMessage());
    }

    @Test
    void deveFalharQuandoFormatoDaDataForInvalido() {
        UUID patientId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(patientId, null, null, "01/07/2026", "2026-07-31")
        );

        assertEquals("Formato de data invalido. Use ISO-8601 em dateFrom/dateTo.", exception.getMessage());
    }
}