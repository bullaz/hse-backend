CREATE TABLE work_permit (
    permit_id      VARCHAR(20)  PRIMARY KEY,
    induction_id   UUID         NOT NULL REFERENCES hse_induction(induction_id),
    site_id        INTEGER      NOT NULL REFERENCES site(site_id),
    description    TEXT,
    start_datetime TIMESTAMP    NOT NULL,
    end_datetime   TIMESTAMP    NOT NULL,
    status         VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
                                CONSTRAINT work_permit_status_check CHECK (status IN ('ACTIVE','EXPIRED','REVOKED')),
    created_at     TIMESTAMP    NOT NULL
);
