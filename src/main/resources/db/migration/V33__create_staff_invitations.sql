-- =============================================
-- V33: Staff invitations
-- =============================================
-- Permet à un admin d'inviter un collaborateur par email.
-- L'invité reçoit un lien avec token pour fixer son mot de passe et activer
-- son compte (rôle STAFF + USER, email_verified=true par construction).

CREATE TABLE staff_invitations (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    token           VARCHAR(128) NOT NULL UNIQUE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    invited_by      UUID         NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    accepted_user   UUID                  REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP    NOT NULL,
    accepted_at     TIMESTAMP,
    revoked_at      TIMESTAMP,

    CONSTRAINT staff_invitations_status_chk
        CHECK (status IN ('PENDING','ACCEPTED','EXPIRED','REVOKED'))
);

-- Une seule invitation PENDING par email à la fois
CREATE UNIQUE INDEX idx_staff_invitations_email_pending
    ON staff_invitations (email)
    WHERE status = 'PENDING';

CREATE INDEX idx_staff_invitations_token        ON staff_invitations (token);
CREATE INDEX idx_staff_invitations_status       ON staff_invitations (status);
CREATE INDEX idx_staff_invitations_expires_at   ON staff_invitations (expires_at);
