-- Side-by-side composite images for ALL verifications (original | annotated with bboxes)
-- Stored 48 hours then purged by the cleanup scheduler.
CREATE TABLE IF NOT EXISTS hse_schema.verification_composite_image (
    log_id      UUID        NOT NULL PRIMARY KEY
                            REFERENCES hse_schema.ppe_verification_log(log_id) ON DELETE CASCADE,
    image_data  BYTEA       NOT NULL,
    expires_at  TIMESTAMP   NOT NULL
);
