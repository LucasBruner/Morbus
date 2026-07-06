package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
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
class ConsultarDisponibilidadeUseCaseTest {

    @Mock
    private ISlotRepository slotRepository;

    private ConsultarDisponibilidadeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarDisponibilidadeUseCase(slotRepository);
    }

    @Test
    void deveConsultarDisponibilidadeComIntervaloExpandidoParaDatas() {
        UUID procedureId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        List<Slot> esperado = List.of();

        when(slotRepository.findByProcedureAndUnitAndDate(
                procedureId,
                unitId,
            LocalDateTime.of(2026, Month.JULY, 5, 0, 0),
            LocalDateTime.of(2026, Month.JULY, 6, 23, 59, 59, 999999999)
        )).thenReturn(esperado);

        List<Slot> resultado = useCase.execute(procedureId, unitId, "2026-07-05", "2026-07-06");

        assertEquals(esperado, resultado);
        verify(slotRepository).findByProcedureAndUnitAndDate(
                procedureId,
                unitId,
            LocalDateTime.of(2026, Month.JULY, 5, 0, 0),
            LocalDateTime.of(2026, Month.JULY, 6, 23, 59, 59, 999999999)
        );
    }

    @Test
    void deveFalharQuandoIntervaloForInvalido() {
        UUID procedureId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
            () -> useCase.execute(procedureId, null, "2026-07-07", "2026-07-06")
        );

        assertEquals("dateFrom deve ser anterior ou igual a dateTo", exception.getMessage());
    }

    @Test
    void deveFalharQuandoFormatoDeDataForInvalido() {
        UUID procedureId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
            () -> useCase.execute(procedureId, null, "07/05/2026", "2026-07-06")
        );

        assertEquals("Formato de data invalido. Use ISO-8601 em dateFrom/dateTo.", exception.getMessage());
    }
}