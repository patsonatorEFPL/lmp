package com.lmp.support.web;

import com.lmp.support.repository.AuditLogRepository;
import com.lmp.support.service.AuditLogService;
import com.lmp.support.service.SupportSessionService;
import com.lmp.support.web.dto.AuditChainVerificationResponse;
import com.lmp.support.web.dto.AuditEntryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Read-only access to a session's audit log + hash chain verification. Scoped
 * to SUPPORT/ADMIN since the chain exposes per-event details (consent ip,
 * command payloads, etc.). The session existence check propagates the
 * 404 from {@code SupportSessionService.findById} when the id is unknown.
 */
@RestController
@RequestMapping("/api/v1/support/sessions")
public class AuditController {

    private final AuditLogRepository auditRepo;
    private final AuditLogService auditService;
    private final SupportSessionService sessionService;

    public AuditController(AuditLogRepository auditRepo,
                           AuditLogService auditService,
                           SupportSessionService sessionService) {
        this.auditRepo = auditRepo;
        this.auditService = auditService;
        this.sessionService = sessionService;
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('SUPPORT','ADMIN')")
    public List<AuditEntryResponse> list(@PathVariable UUID id) {
        sessionService.findById(id);  // 404 propagates if unknown
        return auditRepo.findBySessionIdOrderByOccurredAtAsc(id).stream()
            .map(AuditEntryResponse::of)
            .toList();
    }

    @GetMapping("/{id}/audit/verify")
    @PreAuthorize("hasAnyRole('SUPPORT','ADMIN')")
    public AuditChainVerificationResponse verify(@PathVariable UUID id) {
        sessionService.findById(id);
        boolean valid = auditService.verifyChain(id);
        int count = auditRepo.findBySessionIdOrderByOccurredAtAsc(id).size();
        return new AuditChainVerificationResponse(id, valid, count);
    }
}
