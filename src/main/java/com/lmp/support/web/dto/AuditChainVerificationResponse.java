package com.lmp.support.web.dto;

import java.util.UUID;

public record AuditChainVerificationResponse(
    UUID sessionId,
    boolean valid,
    int entryCount
) {}
