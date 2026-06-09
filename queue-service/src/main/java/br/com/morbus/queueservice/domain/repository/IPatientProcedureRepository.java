package br.com.morbus.queueservice.domain.repository;

import java.util.UUID;

public interface IPatientProcedureRepository {
    void save(UUID procedureId, UUID patientId);
    boolean existsByPatientAndProcedure(UUID patientId, UUID procedureId);
}
