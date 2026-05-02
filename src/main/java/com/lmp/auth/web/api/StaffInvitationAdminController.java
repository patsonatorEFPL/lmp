package com.lmp.auth.web.api;

import com.lmp.auth.domain.StaffInvitation;
import com.lmp.auth.domain.StaffInvitationStatus;
import com.lmp.auth.domain.User;
import com.lmp.auth.dto.StaffInvitationCreateRequest;
import com.lmp.auth.dto.StaffInvitationResponse;
import com.lmp.auth.repository.StaffInvitationRepository;
import com.lmp.auth.service.StaffInvitationService;
import com.lmp.auth.service.UserService;
import com.lmp.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints admin pour gérer les invitations de collaborateurs (STAFF).
 */
@RestController
@RequestMapping("/api/v1/admin/staff-invitations")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Staff Invitations", description = "Inviter, lister et révoquer les collaborateurs")
public class StaffInvitationAdminController {

    private static final Logger logger = LoggerFactory.getLogger(StaffInvitationAdminController.class);

    private final StaffInvitationService invitationService;
    private final StaffInvitationRepository invitationRepository;
    private final UserService userService;

    public StaffInvitationAdminController(StaffInvitationService invitationService,
                                          StaffInvitationRepository invitationRepository,
                                          UserService userService) {
        this.invitationService = invitationService;
        this.invitationRepository = invitationRepository;
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Inviter un collaborateur par email")
    public ResponseEntity<ApiResponse<StaffInvitationResponse>> invite(
            @RequestBody StaffInvitationCreateRequest request,
            Authentication authentication) {
        try {
            UUID actorId = currentUserId(authentication);
            StaffInvitation inv = invitationService.createInvitation(request, actorId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Invitation envoyée à " + inv.getEmail(),
                            StaffInvitationResponse.from(inv)));
        } catch (IllegalArgumentException e) {
            logger.warn("[STAFF-INVITE] Validation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Lister les invitations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StaffInvitation> pageResult;
        if (status != null && !status.isBlank()) {
            try {
                StaffInvitationStatus parsed = StaffInvitationStatus.valueOf(status.trim().toUpperCase());
                pageResult = invitationRepository.findByStatus(parsed, pageable);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Statut invalide : " + status));
            }
        } else {
            pageResult = invitationRepository.findAll(pageable);
        }
        List<StaffInvitationResponse> items = pageResult.getContent().stream()
                .map(StaffInvitationResponse::from)
                .toList();
        Map<String, Object> body = Map.of(
                "items", items,
                "page", pageResult.getNumber(),
                "size", pageResult.getSize(),
                "totalElements", pageResult.getTotalElements(),
                "totalPages", pageResult.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @PostMapping("/{id}/resend")
    @Operation(summary = "Renvoyer l'email d'invitation (régénère le token)")
    public ResponseEntity<ApiResponse<Void>> resend(@PathVariable UUID id, Authentication authentication) {
        try {
            invitationService.resendInvitation(id, currentUserId(authentication));
            return ResponseEntity.ok(ApiResponse.<Void>ok("Invitation renvoyée", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Révoquer une invitation en attente")
    public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable UUID id, Authentication authentication) {
        try {
            invitationService.revokeInvitation(id, currentUserId(authentication));
            return ResponseEntity.ok(ApiResponse.<Void>ok("Invitation révoquée", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private UUID currentUserId(Authentication authentication) {
        User actor = userService.findByLogin(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Session administrateur invalide"));
        return actor.getId();
    }
}
