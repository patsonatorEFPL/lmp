package com.lmp.notification.web;

import com.lmp.notification.mail.queue.EmailQueueEvent;
import com.lmp.notification.mail.queue.EmailQueueEventDto;
import com.lmp.notification.mail.queue.EmailQueueRepository;
import com.lmp.notification.mail.queue.EmailQueueSpecification;
import com.lmp.notification.mail.queue.EmailQueueStatus;
import com.lmp.shared.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/email-queue")
@PreAuthorize("hasRole('ADMIN')")
public class EmailQueueRestController {

    private static final int MAX_BULK_SIZE = 500;
    private static final String MANUAL_RETRY_TAG_FMT = "\n[manual-retry] reset at %s";
    private static final String BULK_RETRY_TAG_FMT = "\n[bulk-retry] reset at %s";

    private final EmailQueueRepository emailQueueRepository;

    public EmailQueueRestController(EmailQueueRepository emailQueueRepository) {
        this.emailQueueRepository = emailQueueRepository;
    }

    @GetMapping
    public ResponseEntity<Page<EmailQueueEventDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) EmailQueueStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        Page<EmailQueueEvent> result = emailQueueRepository.findAll(
                EmailQueueSpecification.withFilters(status, search, from, to), pageable);
        return ResponseEntity.ok(result.map(EmailQueueEventDto::fromEntity));
    }

    @PostMapping("/{id}/retry")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> retry(@PathVariable UUID id) {
        int updated = emailQueueRepository.markForRetry(id, MANUAL_RETRY_TAG_FMT.formatted(Instant.now()));
        if (updated == 0) {
            return ResponseEntity.status(404).body(
                    ApiResponse.error("Email introuvable ou en cours d'envoi (SENDING)"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Email remis en file d'attente", Map.of("retried", updated)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable UUID id) {
        if (!emailQueueRepository.existsById(id)) {
            return ResponseEntity.status(404).body(ApiResponse.error("Email introuvable"));
        }
        emailQueueRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Email supprimé", Map.of("deleted", 1)));
    }

    @PostMapping("/bulk-retry")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkRetry(@RequestBody List<UUID> ids) {
        ResponseEntity<ApiResponse<Map<String, Object>>> guard = validateBulk(ids);
        if (guard != null) return guard;

        int retried = emailQueueRepository.bulkMarkForRetry(ids, BULK_RETRY_TAG_FMT.formatted(Instant.now()));
        return ResponseEntity.ok(ApiResponse.ok(retried + " emails relancés", Map.of("retried", retried)));
    }

    @PostMapping("/bulk-delete")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkDelete(@RequestBody List<UUID> ids) {
        ResponseEntity<ApiResponse<Map<String, Object>>> guard = validateBulk(ids);
        if (guard != null) return guard;

        int deleted = emailQueueRepository.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.ok(deleted + " emails supprimés", Map.of("deleted", deleted)));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        EnumMap<EmailQueueStatus, Long> byStatus = new EnumMap<>(EmailQueueStatus.class);
        for (EmailQueueStatus s : EmailQueueStatus.values()) {
            byStatus.put(s, 0L);
        }
        long total = 0;
        for (Object[] row : emailQueueRepository.statsGroupByStatus()) {
            EmailQueueStatus s = (EmailQueueStatus) row[0];
            long c = ((Number) row[1]).longValue();
            byStatus.put(s, c);
            total += c;
        }
        return ResponseEntity.ok(Map.of(
                "total", total,
                "notSent", byStatus.get(EmailQueueStatus.NOT_SENT),
                "sent", byStatus.get(EmailQueueStatus.SENT),
                "error", byStatus.get(EmailQueueStatus.ERROR),
                "sending", byStatus.get(EmailQueueStatus.SENDING)
        ));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> validateBulk(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Liste d'ids vide"));
        }
        if (ids.size() > MAX_BULK_SIZE) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Taille de lot dépassée (max " + MAX_BULK_SIZE + ")"));
        }
        return null;
    }
}
