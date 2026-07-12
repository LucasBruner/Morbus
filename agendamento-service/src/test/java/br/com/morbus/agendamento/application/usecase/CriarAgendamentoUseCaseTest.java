package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.application.command.CriarAgendamentoCommand;
import br.com.morbus.agendamento.domain.exception.DuplicateAgendamentoException;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriarAgendamentoUseCaseTest {

    @Mock
    private IAgendamentoRepository agendamentoRepository;

    private CriarAgendamentoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CriarAgendamentoUseCase(agendamentoRepository);
    }

    @Test
    void deveCriarAgendamentoQuandoNaoHouverDuplicidade() {
        UUID queueEntryId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);
        CriarAgendamentoCommand command = new CriarAgendamentoCommand(queueEntryId, slotId, pacienteId, expiresAt);

        when(agendamentoRepository.existsByPacienteIdAndSlotId(pacienteId, slotId)).thenReturn(false);
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Agendamento result = useCase.execute(command);

        ArgumentCaptor<Agendamento> captor = ArgumentCaptor.forClass(Agendamento.class);
        verify(agendamentoRepository).save(captor.capture());
        Agendamento saved = captor.getValue();

        assertEquals(queueEntryId, saved.getQueueEntryId());
        assertEquals(slotId, saved.getSlotId());
        assertEquals(pacienteId, saved.getPacienteId());
        assertEquals(expiresAt, saved.getExpiresAt());
        assertEquals(saved, result);
    }

    @Test
    void deveLancarExcecaoQuandoPacienteJaPossuiAgendamentoParaOSlot() {
        UUID queueEntryId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        CriarAgendamentoCommand command = new CriarAgendamentoCommand(queueEntryId, slotId, pacienteId, LocalDateTime.now());

        when(agendamentoRepository.existsByPacienteIdAndSlotId(pacienteId, slotId)).thenReturn(true);

        assertThrows(DuplicateAgendamentoException.class, () -> useCase.execute(command));

        verify(agendamentoRepository, never()).save(any());
    }
}
