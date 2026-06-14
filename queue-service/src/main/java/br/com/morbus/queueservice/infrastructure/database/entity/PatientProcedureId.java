package br.com.morbus.queueservice.infrastructure.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PatientProcedureId implements Serializable {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "procedure_id", nullable = false)
    private UUID procedureId;
}
