package br.com.morbus.queueservice.domain.repository;

import java.util.UUID;

public interface IPatientProcedureRepository {
    void save(UUID patientId, UUID procedureId);
    boolean existsByPatientAndProcedure(UUID patientId, UUID procedureId);
    void deleteByPatientAndProcedure(UUID patientId, UUID procedureId);
}
