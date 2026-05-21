-- Move habilitations from site to zone_type
DROP TABLE IF EXISTS site_habilitation;

CREATE TABLE zone_type_habilitation (
    zone_type_id    INTEGER NOT NULL REFERENCES zone_type(zone_type_id) ON DELETE CASCADE,
    habilitation_id INTEGER NOT NULL REFERENCES habilitation(habilitation_id) ON DELETE CASCADE,
    PRIMARY KEY (zone_type_id, habilitation_id)
);
