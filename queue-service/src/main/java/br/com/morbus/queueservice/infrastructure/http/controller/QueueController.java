package br.com.morbus.queueservice.infrastructure.http.controller;

import br.com.morbus.queueservice.domain.usecase.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/queue")
@Tag(name = "Queue", description = "Endpoints para gerenciamento de filas")
public class QueueController {

    private final RegisterPatientInQueue registerPatientInQueue;
    private final ListQueueByPriority listQueueByPriority;
    private final GetQueuePosition getQueuePosition;
    private final CallNextPatient callNextPatient;
    private final ReclassifyPriority reclassifyPriority;
    private final CancelQueueEntry cancelQueueEntry;

    public QueueController(RegisterPatientInQueue registerPatientInQueue, ListQueueByPriority listQueueByPriority, GetQueuePosition getQueuePosition, CallNextPatient callNextPatient, ReclassifyPriority reclassifyPriority, CancelQueueEntry cancelQueueEntry) {
        this.registerPatientInQueue = registerPatientInQueue;
        this.listQueueByPriority = listQueueByPriority;
        this.getQueuePosition = getQueuePosition;
        this.callNextPatient = callNextPatient;
        this.reclassifyPriority = reclassifyPriority;
        this.cancelQueueEntry = cancelQueueEntry;
    }
}
