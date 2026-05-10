-- ============================================================================
-- V41 — Email Queue (durable async mail send, multi-replica safe)
-- ============================================================================
-- Inspired by external CRM Email Queue but optimized:
--   * single-row claim via UPDATE...RETURNING (vs external CRM SELECT FOR UPDATE per row)
--   * 5s poll tick (vs external CRM 4-min cron tick)
--   * multi-replica safe by design (external CRM = single worker)
-- ============================================================================

CREATE TABLE email_queue (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender             VARCHAR(255) NOT NULL,
    sender_name        VARCHAR(255),
    reply_to           VARCHAR(255),
    recipient          VARCHAR(255) NOT NULL,
    cc                 VARCHAR(1000),
    bcc                VARCHAR(1000),
    subject            VARCHAR(500) NOT NULL,
    body_html          TEXT,
    body_text          TEXT,
    status             VARCHAR(20)  NOT NULL DEFAULT 'NOT_SENT',
    priority           INT          NOT NULL DEFAULT 5,
    retry_count        INT          NOT NULL DEFAULT 0,
    max_retries        INT          NOT NULL DEFAULT 3,
    send_after         TIMESTAMP    NOT NULL DEFAULT NOW(),
    status_changed_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    sent_at            TIMESTAMP,
    last_error         TEXT,
    correlation_id     UUID,

    CONSTRAINT email_queue_status_check CHECK (
        status IN ('NOT_SENT','SENDING','SENT','ERROR','EXPIRED')
    ),
    CONSTRAINT email_queue_priority_check CHECK (priority BETWEEN 1 AND 9),
    CONSTRAINT email_queue_retry_check    CHECK (retry_count >= 0 AND retry_count <= max_retries)
);

-- Pickup index : NOT_SENT + send_after due, sorted by priority desc + retry asc + created_at asc
CREATE INDEX idx_email_queue_claim
    ON email_queue (status, priority DESC, retry_count ASC, created_at ASC)
    WHERE status = 'NOT_SENT';

-- Orphan recovery index : SENDING rows whose status_changed_at is stale
CREATE INDEX idx_email_queue_recovery
    ON email_queue (status, status_changed_at)
    WHERE status = 'SENDING';

-- Correlation lookup (for thread tracking equivalent of external CRM Communication link)
CREATE INDEX idx_email_queue_correlation
    ON email_queue (correlation_id)
    WHERE correlation_id IS NOT NULL;

COMMENT ON TABLE  email_queue IS 'Durable async mail queue. Claim atomique via UPDATE...RETURNING.';
COMMENT ON COLUMN email_queue.status IS 'NOT_SENT (queued) -> SENDING (claimed) -> SENT|ERROR|EXPIRED.';
COMMENT ON COLUMN email_queue.priority IS '1..9, 9 = highest. Picked first by claim worker.';
COMMENT ON COLUMN email_queue.send_after IS 'Earliest delivery time. Workers skip rows where send_after > NOW().';
COMMENT ON COLUMN email_queue.status_changed_at IS 'Timestamp of last status transition. Used by orphan recovery (SENDING > 5min => reset NOT_SENT).';
