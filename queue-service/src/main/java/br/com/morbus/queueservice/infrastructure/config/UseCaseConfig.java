package br.com.morbus.queueservice.infrastructure.config;

import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.repository.IPatientProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.AssignProcedureToPatient;
import br.com.morbus.queueservice.domain.usecase.CallNextPatient;
import br.com.morbus.queueservice.domain.usecase.CancelQueueEntry;
import br.com.morbus.queueservice.domain.usecase.GetQueuePosition;
import br.com.morbus.queueservice.domain.usecase.InactivatePatient;
import br.com.morbus.queueservice.domain.usecase.ListQueueByPriority;
import br.com.morbus.queueservice.domain.usecase.ReclassifyPriority;
import br.com.morbus.queueservice.domain.usecase.RegisterPatient;
import br.com.morbus.queueservice.domain.usecase.RegisterPatientInQueue;
import br.com.morbus.queueservice.domain.usecase.RemoveProcedureFromPatient;
import br.com.morbus.queueservice.domain.usecase.UpdatePatient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public RegisterPatient registerPatient(IPatientRepository patientRepository) {
        return RegisterPatient.create(patientRepository);
    }

    @Bean
    public UpdatePatient updatePatient(IPatientRepository patientRepository) {
        return UpdatePatient.create(patientRepository);
    }

    @Bean
    public InactivatePatient inactivatePatient(IPatientRepository patientRepository,
                                               IQueueEntryRepository queueEntryRepository) {
        return InactivatePatient.create(patientRepository, queueEntryRepository);
    }

    @Bean
    public AssignProcedureToPatient assignProcedureToPatient(IPatientProcedureRepository patientProcedureRepository,
                                                              IPatientRepository patientRepository,
                                                              IProcedureRepository procedureRepository) {
        return AssignProcedureToPatient.create(patientProcedureRepository, patientRepository, procedureRepository);
    }

    @Bean
    public RemoveProcedureFromPatient removeProcedureFromPatient(IPatientProcedureRepository patientProcedureRepository,
                                                                  IQueueEntryRepository queueEntryRepository,
                                                                  IPatientRepository patientRepository,
                                                                  IProcedureRepository procedureRepository) {
        return RemoveProcedureFromPatient.create(patientProcedureRepository, queueEntryRepository,
                patientRepository, procedureRepository);
    }

    @Bean
    public RegisterPatientInQueue registerPatientInQueue(IPatientRepository patientRepository,
                                                         IProcedureRepository procedureRepository,
                                                         IQueueEntryRepository queueEntryRepository,
                                                         IQueueEventPublisher eventPublisher) {
        return RegisterPatientInQueue.create(patientRepository, procedureRepository,
                queueEntryRepository, eventPublisher);
    }

    @Bean
    public CallNextPatient callNextPatient(IQueueEntryRepository queueEntryRepository,
                                           IQueueEventPublisher eventPublisher) {
        return CallNextPatient.create(queueEntryRepository, eventPublisher);
    }

    @Bean
    public CancelQueueEntry cancelQueueEntry(IQueueEntryRepository queueEntryRepository,
                                              IQueueEventPublisher eventPublisher) {
        return CancelQueueEntry.create(queueEntryRepository, eventPublisher);
    }

    @Bean
    public GetQueuePosition getQueuePosition(IQueueEntryRepository queueEntryRepository) {
        return GetQueuePosition.create(queueEntryRepository);
    }

    @Bean
    public ReclassifyPriority reclassifyPriority(IQueueEventPublisher eventPublisher,
                                                   IQueueEntryRepository queueEntryRepository) {
        return ReclassifyPriority.create(eventPublisher, queueEntryRepository);
    }

    @Bean
    public ListQueueByPriority listQueueByPriority(IQueueEntryRepository queueEntryRepository) {
        return ListQueueByPriority.create(queueEntryRepository);
    }
}
