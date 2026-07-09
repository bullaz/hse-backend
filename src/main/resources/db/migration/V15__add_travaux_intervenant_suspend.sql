CREATE TABLE hse_schema.travaux_intervenant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    travaux_id UUID NOT NULL REFERENCES hse_schema.travaux(travaux_id),
    induction_id UUID NOT NULL REFERENCES hse_schema.hse_induction(induction_id),
    suspended BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (travaux_id, induction_id)
);

INSERT INTO hse_schema.travaux_intervenant (travaux_id, induction_id, suspended)
SELECT travaux_id, induction_id, FALSE FROM hse_schema.travaux_intervenants;

DROP TABLE hse_schema.travaux_intervenants;
