package br.com.morbus.queueservice.application.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.event.PatientRegisteredEvent;
import br.com.morbus.queueservice.domain.exception.PatientNotEligibleForProcedureException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotFoundException;
import br.com.morbus.queueservice.domain.port.PatientRegisteredEventPublisher;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.service.PriorityCalculator;
import br.com.morbus.queueservice.domain.usecase.RegisterPatientCommand;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegisterPatientInQueueUseCaseTest {

    private InMemoryPatientRepository patientRepository;
    private InMemoryProcedureRepository procedureRepository;
    private InMemoryQueueEntryRepository queueEntryRepository;
    private TestPublisher eventPublisher;
    private RegisterPatientInQueueUseCase useCase;

    @BeforeEach
    void setUp() {
        patientRepository = new InMemoryPatientRepository();
        procedureRepository = new InMemoryProcedureRepository();
        queueEntryRepository = new InMemoryQueueEntryRepository();
        eventPublisher = new TestPublisher();
        useCase = new RegisterPatientInQueueUseCase(
                patientRepository,
                procedureRepository,
                queueEntryRepository,
                eventPublisher
        );
    }

    @Test
    void shouldSaveNewPatientAndQueueEntryWithDefaultAzul() {
        Procedure exam = new Procedure(UUID.randomUUID(), "P001", "Consulta Geral", 18, 70, "GERAL");
        procedureRepository.save(exam);

        Patient patient = new Patient();
        patient.setCpf("12345678901");
        patient.setCns("999999999999999");
        patient.setNome("João");
        patient.setDataNascimento(LocalDate.now().minusYears(30));

        QueueEntry queueEntry = useCase.execute(new RegisterPatientCommand(patient, 1));

        assertNotNull(queueEntry);
        assertEquals(ERiskColor.AZUL, queueEntry.getRiskColor());
        assertEquals(EQueueStatus.AGUARDANDO, queueEntry.getQueueStatus());
        assertNotNull(queueEntry.getPatient().getId());
        assertTrue(patientRepository.saveCallCount > 0);
        assertTrue(queueEntryRepository.saveCallCount > 0);
        assertTrue(eventPublisher.published);
    }

    @Test
    void shouldReuseExistingPatientWhenCpfAlreadyExists() {
        Patient existing = new Patient();
        existing.setId(UUID.randomUUID());
        existing.setCpf("12345678901");
        existing.setCns("999999999999999");
        existing.setNome("Maria");
        existing.setDataNascimento(LocalDate.now().minusYears(45));
        patientRepository.save(existing);

        Procedure exam = new Procedure(UUID.randomUUID(), "P002", "Exame de Rotina", 18, 70, "GERAL");
        procedureRepository.save(exam);

        Patient candidate = new Patient();
        candidate.setCpf(existing.getCpf());
        candidate.setCns(existing.getCns());
        candidate.setNome("Maria Silva");
        candidate.setDataNascimento(existing.getDataNascimento());

        QueueEntry queueEntry = useCase.execute(new RegisterPatientCommand(candidate, 1));

        assertNotNull(queueEntry);
        assertSame(existing, queueEntry.getPatient());
        assertEquals(existing.getId(), queueEntry.getPatient().getId());
        assertEquals(1, patientRepository.saveCallCount);
    }

    @Test
    void shouldAssignIdosoGroupAutomaticallyForPatientsSixtyPlus() {
        Procedure exam = new Procedure(UUID.randomUUID(), "P003", "Exame Geriátrico", 60, 90, "GERAL");
        procedureRepository.save(exam);

        Patient patient = new Patient();
        patient.setCpf("11122233344");
        patient.setDataNascimento(LocalDate.now().minusYears(62));

        QueueEntry queueEntry = useCase.execute(new RegisterPatientCommand(patient, 1));

        assertEquals(EPriorityGroup.IDOSO, queueEntry.getPatient().getGrupoLegal());
    }

    @Test
    void shouldThrowWhenProcedureDoesNotExist() {
        Patient patient = new Patient();
        patient.setCpf("44455566677");
        patient.setDataNascimento(LocalDate.now().minusYears(35));

        RegisterPatientCommand command = new RegisterPatientCommand(patient, 1);
        assertThrows(ProcedureNotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void shouldThrowWhenPatientNotEligibleByAge() {
        Procedure exam = new Procedure(UUID.randomUUID(), "P004", "Consulta Pediátrica", 0, 12, "GERAL");
        procedureRepository.save(exam);

        Patient patient = new Patient();
        patient.setCpf("77788899900");
        patient.setDataNascimento(LocalDate.now().minusYears(30));

        RegisterPatientCommand command = new RegisterPatientCommand(patient, 1);
        assertThrows(PatientNotEligibleForProcedureException.class, () -> useCase.execute(command));
    }

    private static class InMemoryPatientRepository implements IPatientRepository {
        private final List<Patient> patients = new ArrayList<>();
        private int saveCallCount = 0;

        @Override
        public void save(Patient patient) {
            saveCallCount++;
            if (patient.getId() == null) {
                patient.setId(UUID.randomUUID());
            }
            patients.removeIf(existing -> existing.getId() != null && existing.getId().equals(patient.getId()));
            patients.add(patient);
        }

        @Override
        public Optional<Patient> findById(Integer id) {
            return Optional.empty();
        }

        @Override
        public Optional<Patient> findByCpf(String cpf) {
            return patients.stream()
                    .filter(patient -> cpf.equals(patient.getCpf()))
                    .findFirst();
        }

        @Override
        public Optional<Patient> findByCns(String cns) {
            return patients.stream()
                    .filter(patient -> cns.equals(patient.getCns()))
                    .findFirst();
        }

        @Override
        public List<Patient> findAll() {
            return new ArrayList<>(patients);
        }
    }

    private static class InMemoryProcedureRepository implements IProcedureRepository {
        private final List<Procedure> procedures = new ArrayList<>();

        @Override
        public void save(Procedure procedure) {
            procedures.removeIf(existing -> existing.getId() != null && existing.getId().equals(procedure.getId()));
            procedures.add(procedure);
        }

        @Override
        public Optional<Procedure> findById(Integer id) {
            if (id == null || id <= 0 || id > procedures.size()) {
                return Optional.empty();
            }
            return Optional.of(procedures.get(id - 1));
        }

        @Override
        public Optional<Procedure> findByCoProcedimento(String coProcedimento) {
            return procedures.stream()
                    .filter(procedure -> coProcedimento.equals(procedure.getCoProcedimento()))
                    .findFirst();
        }

        @Override
        public List<Procedure> findAll() {
            return new ArrayList<>(procedures);
        }
    }

    private static class InMemoryQueueEntryRepository implements IQueueEntryRepository {
        private final List<QueueEntry> entries = new ArrayList<>();
        private int saveCallCount = 0;

        @Override
        public void save(QueueEntry queueEntry) {
            saveCallCount++;
            entries.add(queueEntry);
        }

        @Override
        public QueueEntry execute(RegisterPatientCommand command) {
            validateCommand(command);

            Procedure procedure = procedureRepository.findById(command.getProcedureId())
                    .orElseThrow(() -> new ProcedureNotFoundException(command.getProcedureId()));

            Optional<Patient> existingPatient = findExistingPatient(command.getPatient());
            Patient patient = existingPatient.orElse(command.getPatient());
            validatePatientAge(patient.getDataNascimento(), procedure);

            EPriorityGroup priorityGroup = PriorityCalculator.getPriorityGroup(patient);
            patient.setGrupoLegal(priorityGroup);

            if (existingPatient.isEmpty()) {
                patientRepository.save(patient);
            }

            QueueEntry queueEntry = buildQueueEntry(patient, procedure);
            queueEntryRepository.save(queueEntry);

            eventPublisher.publish(new PatientRegisteredEvent(patient, queueEntry));
            return queueEntry;
        }

        @Override
        public Optional<QueueEntry> findById(Integer id) {
            return Optional.empty();
        }

        @Override
        public QueueEntry findNextByPriority() {
            return null;
        }

        @Override
        public List<QueueEntry> findAllOrderedByPriority() {
            return new ArrayList<>(entries);
        }
    }

    private static class TestPublisher implements PatientRegisteredEventPublisher {
        private boolean published;

        @Override
        public void publish(PatientRegisteredEvent event) {
            published = true;
        }
    }
}
