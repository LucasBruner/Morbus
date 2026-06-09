CREATE TABLE patient_procedures (
                                    patient_id    UUID NOT NULL REFERENCES patients(id),
                                    procedure_id  UUID NOT NULL REFERENCES procedures(id),
                                    assigned_at   TIMESTAMP NOT NULL DEFAULT NOW(),
                                    PRIMARY KEY (patient_id, procedure_id)
);