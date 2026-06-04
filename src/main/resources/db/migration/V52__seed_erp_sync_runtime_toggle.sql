-- Seed du toggle runtime ERP sync (spec 2026-05-29-erp-sync-toggle-design.md).
-- Sans cette ligne, la clé est absente partout (env/file/DB) et
-- SiteConfigManager.getString refait un SELECT par enqueue() sous charge
-- (ConcurrentHashMap.computeIfAbsent ne cache pas null).
-- NE PAS seeder via application.properties : la hiérarchie env > file > DB
-- rendrait le PUT admin inopérant (env masquerait la valeur DB).
INSERT INTO site_config (config_key, config_value, config_description)
VALUES ('lmp.sync.runtime-enabled', 'true',
        'Toggle runtime sync ERP (fail-open). false = enqueue() ignore tous les événements sortants.')
ON CONFLICT (config_key) DO NOTHING;
