package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.dto.ListQueueByPriorityDTO;

import java.util.List;

public class ListQueueByPriority {

    private final IQueueEntryRepository queueEntryRepository;
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final EQueueStatus DEFAULT_STATUS = EQueueStatus.AGUARDANDO;

    private ListQueueByPriority(IQueueEntryRepository queueEntryRepository) {
        this.queueEntryRepository = queueEntryRepository;
    }

    public static ListQueueByPriority create(IQueueEntryRepository queueEntryRepository) {
        return new ListQueueByPriority(queueEntryRepository);
    }

    public List<QueueEntry> run(ListQueueByPriorityDTO dto) {
        EQueueStatus statusFilter = dto.status() != null ? dto.status() : DEFAULT_STATUS;

        // Filtra por status
        // Filtra por riskColor (opcional, retorna todas as cores se não for informado no dto)
        // Retorna lista por: riskColor ASC, priorityGroup ASC, registeredAt ASC
        List<QueueEntry> entries = queueEntryRepository.findByProcedureIdAndFilters(
                dto.procedureId(),
                statusFilter,
                dto.riskColor()
        );

        // Aplica paginação
        int page = dto.page() != null ? dto.page() : DEFAULT_PAGE;
        int size = dto.size() != null ? dto.size() : DEFAULT_SIZE;

        int start = page * size;
        int end = Math.min(start + size, entries.size());

        // Retorna lista vazia caso não tenha filas
        if (start >= entries.size()) {
            return List.of();
        }

        return entries.subList(start, end);
    }
}
