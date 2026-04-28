-- Phase 5 : Mapper versioning sur sync_event_log
ALTER TABLE sync_event_log ADD COLUMN mapper_version VARCHAR(20) NOT NULL DEFAULT '1';

-- Index pour filtrer/trouver par version de mapper si besoin de re-jouer/retry ciblés
CREATE INDEX idx_sync_mapper_version ON sync_event_log (mapper_version);
