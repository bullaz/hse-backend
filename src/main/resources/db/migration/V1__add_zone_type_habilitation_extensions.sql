-- ZoneType lookup table
CREATE TABLE zone_type (
    zone_type_id SERIAL PRIMARY KEY,
    label        VARCHAR(100) NOT NULL UNIQUE
);

-- Habilitation lookup table
CREATE TABLE habilitation (
    habilitation_id SERIAL PRIMARY KEY,
    code            VARCHAR(20)  NOT NULL UNIQUE,
    label           VARCHAR(100) NOT NULL,
    description     TEXT
);

-- Site: add zone type FK and habilitation join table
ALTER TABLE site
    ADD COLUMN zone_type_id INTEGER REFERENCES zone_type(zone_type_id);

CREATE TABLE site_habilitation (
    site_id         INTEGER NOT NULL REFERENCES site(site_id) ON DELETE CASCADE,
    habilitation_id INTEGER NOT NULL REFERENCES habilitation(habilitation_id) ON DELETE CASCADE,
    PRIMARY KEY (site_id, habilitation_id)
);

-- HseInduction: add contact/professional fields and habilitation join table
ALTER TABLE hse_induction
    ADD COLUMN phone VARCHAR(20),
    ADD COLUMN email VARCHAR(100),
    ADD COLUMN work  VARCHAR(100);

CREATE TABLE induction_habilitation (
    induction_id    UUID    NOT NULL REFERENCES hse_induction(induction_id) ON DELETE CASCADE,
    habilitation_id INTEGER NOT NULL REFERENCES habilitation(habilitation_id) ON DELETE CASCADE,
    PRIMARY KEY (induction_id, habilitation_id)
);

-- PpeVerificationLog: add rejection causes and worker certification timestamp
ALTER TABLE ppe_verification_log
    ADD COLUMN rejection_causes TEXT,
    ADD COLUMN certified_at     TIMESTAMP;
