CREATE TABLE IF NOT EXISTS agendamento.slots (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id UUID         NOT NULL REFERENCES agendamento.schedules(id),
    date_time   TIMESTAMP    NOT NULL,
    capacity    INTEGER      NOT NULL,
    booked      INTEGER      NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE'
                    CHECK (status IN ('AVAILABLE', 'BLOCKED', 'FULL')),
    UNIQUE (schedule_id, date_time)
);

CREATE INDEX idx_slots_availability
    ON agendamento.slots (schedule_id, date_time, status)
    WHERE status = 'AVAILABLE';
