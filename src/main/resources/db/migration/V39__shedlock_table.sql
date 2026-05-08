-- ShedLock table — coordonne les @Scheduled critiques entre N répliques swarm.
-- Utilisée par net.javacrumbs.shedlock pour leader-only execution
-- (purge, daily report, reminders, refresh externes).
--
-- Schema requis par shedlock-provider-jdbc-template (PostgreSQL).
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
