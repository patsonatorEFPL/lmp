-- ============================================================================
-- V43 — HelpDesk Phase 2 foundation: session, consent, audit log
-- ============================================================================
-- Adds the durable schema for the remote support workflow:
--   * support_session : per-tech-customer remote session (state machine)
--   * support_consent_record : GDPR/Loi 25 consent capture, signed text hash
--   * support_audit_log : append-only event log, partitioned by month,
--     SHA-256 hash chain enforced application-side, append-only enforced
--     at DB-level via BEFORE UPDATE OR DELETE trigger (role-agnostic)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Sessions
-- ---------------------------------------------------------------------------

CREATE TABLE support_session (
    id                        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id                 UUID         NOT NULL REFERENCES tickets(id),
    tech_user_id              UUID         NOT NULL REFERENCES users(id),
    client_user_id            UUID         NOT NULL REFERENCES users(id),
    status                    VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    mesh_central_group_id     VARCHAR(128),
    mesh_central_node_id      VARCHAR(128),
    runner_token_jti          UUID         NOT NULL UNIQUE,
    created_at                TIMESTAMP    NOT NULL DEFAULT NOW(),
    invited_at                TIMESTAMP,
    consent_at                TIMESTAMP,
    started_at                TIMESTAMP,
    ended_at                  TIMESTAMP,
    archived_at               TIMESTAMP,
    end_reason                VARCHAR(64),
    metadata                  JSONB        NOT NULL DEFAULT '{}'::jsonb,
    version                   BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT support_session_status_check CHECK (
        status IN ('DRAFT','INVITED','CONSENT_WAIT','ACTIVE','ENDING','MUXING','ARCHIVED','ABORTED')
    )
);

CREATE INDEX idx_support_session_status_active
    ON support_session (status)
    WHERE status IN ('ACTIVE','CONSENT_WAIT');

CREATE INDEX idx_support_session_tech
    ON support_session (tech_user_id, created_at DESC);

CREATE INDEX idx_support_session_ticket
    ON support_session (ticket_id);

CREATE INDEX idx_support_session_mesh_group
    ON support_session (mesh_central_group_id)
    WHERE mesh_central_group_id IS NOT NULL;

COMMENT ON TABLE support_session IS 'Remote helpdesk session — state machine from DRAFT to ARCHIVED/ABORTED.';
COMMENT ON COLUMN support_session.runner_token_jti IS 'Single-use JWT id consumed via Redis SETNX when runner connects.';

-- ---------------------------------------------------------------------------
-- Consent records
-- ---------------------------------------------------------------------------

CREATE TABLE support_consent_record (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id           UUID         NOT NULL REFERENCES support_session(id) ON DELETE CASCADE,
    accepted             BOOLEAN      NOT NULL,
    items_consented      JSONB        NOT NULL,
    client_ip            INET,
    client_user_agent    TEXT,
    consent_text_hash    CHAR(64)     NOT NULL,
    occurred_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consent_session
    ON support_consent_record (session_id);

COMMENT ON COLUMN support_consent_record.consent_text_hash IS
    'SHA-256 of the exact consent UI text shown to the client (versioned, juriste-reviewed).';

-- ---------------------------------------------------------------------------
-- Audit log (partitioned by month)
-- ---------------------------------------------------------------------------

CREATE TABLE support_audit_log (
    id                   BIGSERIAL,
    session_id           UUID         NOT NULL,
    event_type           VARCHAR(64)  NOT NULL,
    actor_type           VARCHAR(16)  NOT NULL,
    actor_id             UUID,
    payload              JSONB        NOT NULL,
    occurred_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    occurred_at_micros   BIGINT       NOT NULL,
    prev_hash            BYTEA,
    entry_hash           BYTEA        NOT NULL,
    PRIMARY KEY (id, occurred_at),

    CONSTRAINT support_audit_log_actor_check CHECK (
        actor_type IN ('TECH','CLIENT','SYSTEM','AI')
    )
) PARTITION BY RANGE (occurred_at);

-- Initial partitions: current month + next 2 months.
-- A Spring @Scheduled job (added in a later plan) creates rolling future partitions.
CREATE TABLE support_audit_log_2026_05 PARTITION OF support_audit_log
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE support_audit_log_2026_06 PARTITION OF support_audit_log
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE support_audit_log_2026_07 PARTITION OF support_audit_log
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

CREATE INDEX idx_audit_session_time
    ON support_audit_log (session_id, occurred_at);

CREATE INDEX idx_audit_event_type
    ON support_audit_log (event_type, occurred_at);

-- ---------------------------------------------------------------------------
-- Append-only enforcement at DB level (role-agnostic).
-- A BEFORE UPDATE OR DELETE trigger raises an exception so that even a
-- compromised application connection cannot rewrite history. Tamper-proof
-- in conjunction with the SHA-256 hash chain computed application-side
-- AND with R2 Object Lock on derived recordings.
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION support_audit_log_block_modifications()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'support_audit_log is append-only. % attempted by role=%, txid=%',
        TG_OP, current_user, txid_current()
        USING ERRCODE = 'insufficient_privilege';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER support_audit_log_block_update_2026_05
    BEFORE UPDATE OR DELETE ON support_audit_log_2026_05
    FOR EACH ROW EXECUTE FUNCTION support_audit_log_block_modifications();
CREATE TRIGGER support_audit_log_block_update_2026_06
    BEFORE UPDATE OR DELETE ON support_audit_log_2026_06
    FOR EACH ROW EXECUTE FUNCTION support_audit_log_block_modifications();
CREATE TRIGGER support_audit_log_block_update_2026_07
    BEFORE UPDATE OR DELETE ON support_audit_log_2026_07
    FOR EACH ROW EXECUTE FUNCTION support_audit_log_block_modifications();

COMMENT ON FUNCTION support_audit_log_block_modifications IS
    'Enforces append-only on support_audit_log. Disable per partition only via superuser before TRUNCATE.';
