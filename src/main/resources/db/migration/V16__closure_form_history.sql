ALTER TABLE hse_schema.travaux_closure_form
    DROP CONSTRAINT travaux_closure_form_travaux_id_key;

ALTER TABLE hse_schema.travaux_closure_form
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    ADD COLUMN rejection_reason TEXT;

-- Every existing row predates this history model (rejections used to be deleted
-- outright), so the only forms still on file are ones that were actually validated.
UPDATE hse_schema.travaux_closure_form cf
SET status = 'ACCEPTED'
FROM hse_schema.travaux t
WHERE cf.travaux_id = t.travaux_id AND t.status = 'CLOSED';
