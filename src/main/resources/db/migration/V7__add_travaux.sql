-- Idempotent: all statements use IF NOT EXISTS / conditional DO blocks
-- Safe to run whether or not create-drop already created these tables/columns

CREATE TABLE IF NOT EXISTS hse_schema.travaux (
    travaux_id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    ticket_no           VARCHAR(50)  NOT NULL,
    company_id          INTEGER      REFERENCES hse_schema.company(company_id),
    site_id             INTEGER      NOT NULL REFERENCES hse_schema.site(site_id),
    objet_visite        VARCHAR(30)  NOT NULL,
    description         TEXT         NOT NULL,
    date_debut          TIMESTAMP    NOT NULL,
    date_fin            TIMESTAMP    NOT NULL,
    superviseur_nom     VARCHAR(200) NOT NULL,
    superviseur_email   VARCHAR(255) NOT NULL,
    impact_services     BOOLEAN      NOT NULL DEFAULT FALSE,
    impact_types        TEXT,
    modop_file_name     VARCHAR(255),
    modop_file_data     BYTEA,
    modop_content_type  VARCHAR(100),
    access_type         VARCHAR(10)  NOT NULL DEFAULT 'TRAVAUX',
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    closure_token       VARCHAR(36),
    closure_token_used  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    closed_at           TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hse_schema.travaux_intervenants (
    travaux_id   UUID NOT NULL REFERENCES hse_schema.travaux(travaux_id) ON DELETE CASCADE,
    induction_id UUID NOT NULL REFERENCES hse_schema.hse_induction(induction_id),
    PRIMARY KEY (travaux_id, induction_id)
);

CREATE TABLE IF NOT EXISTS hse_schema.travaux_closure_form (
    closure_id      UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    travaux_id      UUID      NOT NULL UNIQUE REFERENCES hse_schema.travaux(travaux_id) ON DELETE CASCADE,
    chantier_propre BOOLEAN   NOT NULL,
    danger_residuel BOOLEAN   NOT NULL,
    observations    TEXT,
    signature_data  TEXT,
    submitted_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Add travaux_id FK to work_permit (skip if already exists)
ALTER TABLE hse_schema.work_permit
    ADD COLUMN IF NOT EXISTS travaux_id UUID REFERENCES hse_schema.travaux(travaux_id);

-- Make induction_id nullable (skip if already nullable)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'hse_schema'
          AND table_name   = 'work_permit'
          AND column_name  = 'induction_id'
          AND is_nullable  = 'NO'
    ) THEN
        ALTER TABLE hse_schema.work_permit ALTER COLUMN induction_id DROP NOT NULL;
    END IF;
END $$;

-- Add travaux_id FK to ppe_verification_log (skip if already exists)
ALTER TABLE hse_schema.ppe_verification_log
    ADD COLUMN IF NOT EXISTS travaux_id UUID REFERENCES hse_schema.travaux(travaux_id);
