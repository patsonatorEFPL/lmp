-- V43 declared support_consent_record.consent_text_hash as CHAR(64). Hibernate
-- entity @Column(length = 64) maps to String → VARCHAR(64). Schema validation
-- fails: "wrong column type — found [bpchar], expecting [varchar(64)]".
-- ALTER to VARCHAR(64) to align with entity. SHA-256 hex digest always 64 chars
-- so no semantic change; CHAR padding never kicked in anyway.

ALTER TABLE support_consent_record
    ALTER COLUMN consent_text_hash TYPE VARCHAR(64);
