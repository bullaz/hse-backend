-- Company lookup table
CREATE TABLE company (
    company_id SERIAL PRIMARY KEY,
    name       VARCHAR(150) NOT NULL UNIQUE
);

-- Induction role lookup table
CREATE TABLE induction_role (
    role_id SERIAL PRIMARY KEY,
    name    VARCHAR(150) NOT NULL UNIQUE
);

-- Add company and role FKs to hse_induction
ALTER TABLE hse_induction
    ADD COLUMN company_id INTEGER REFERENCES company(company_id),
    ADD COLUMN role_id    INTEGER REFERENCES induction_role(role_id);

-- Permit type lookup table
CREATE TABLE permit_type (
    permit_type_id SERIAL PRIMARY KEY,
    code           VARCHAR(50)  NOT NULL UNIQUE,
    label          VARCHAR(255) NOT NULL,
    description    TEXT
);

-- Permit template: one official blank template file per permit type
CREATE TABLE permit_template (
    template_id    SERIAL       PRIMARY KEY,
    permit_type_id INTEGER      NOT NULL UNIQUE REFERENCES permit_type(permit_type_id) ON DELETE CASCADE,
    file_name      VARCHAR(255) NOT NULL,
    content_type   VARCHAR(100),
    file_data      BYTEA        NOT NULL,
    uploaded_at    TIMESTAMP    NOT NULL
);

-- Add permit type FK and filled permit file storage to work_permit
ALTER TABLE work_permit
    ADD COLUMN permit_type_id           INTEGER     REFERENCES permit_type(permit_type_id),
    ADD COLUMN permit_file_name         VARCHAR(255),
    ADD COLUMN permit_file_data         BYTEA,
    ADD COLUMN permit_file_content_type VARCHAR(100);

-- Link verification log to its associated work permit (auto-resolved for WORK intent)
ALTER TABLE ppe_verification_log
    ADD COLUMN permit_id VARCHAR(20) REFERENCES work_permit(permit_id);
