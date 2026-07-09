-- Ticket number is a permanent identifier, never reused — same guarantee as any
-- other primary key, just enforced as a unique constraint on top of the surrogate
-- UUID primary key rather than replacing it (see notes on why).
CREATE UNIQUE INDEX travaux_ticket_no_unique
    ON hse_schema.travaux (LOWER(ticket_no));
