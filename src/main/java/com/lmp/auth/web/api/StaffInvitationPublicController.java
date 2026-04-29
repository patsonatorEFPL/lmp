package com.lmp.auth.web.api;

import com.lmp.auth.domain.StaffInvitation;
import com.lmp.auth.domain.User;
import com.lmp.auth.dto.StaffInvitationAcceptRequest;
import com.lmp.auth.dto.UserResponse;
import com.lmp.auth.service.StaffInvitationService;
import com.lmp.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints publics pour qu'un destinataire d'invitation finalise son inscription.
 */
@RestController
@RequestMapping("/api/v1/auth/staff-invitations")
@Tag(name = "Staff Invitation (public)", description = "Acceptation d'invitation collaborateur")
public class StaffInvitationPublicController {

    private static final Logger logger = LoggerFactory.getLogger(StaffInvitationPublicController.class);

    private final StaffInvitationService invitationService;

    public StaffInvitationPublicController(StaffInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/preview")
    @Operation(summary = "Vérifier la validité d'un token et pré-remplir le formulaire")
    public ResponseEntity<ApiResponse<Map<String, Object>>> preview(@RequestParam("token") String token) {
        try {
            StaffInvitation inv = invitationService.findValidByToken(token);
            Map<String, Object> body = Map.of(
                    "email", inv.getEmail(),
                    "firstName", inv.getFirstName() != null ? inv.getFirstName() : "",
                    "lastName", inv.getLastName() != null ? inv.getLastName() : "",
                    "expiresAt", inv.getExpiresAt()
            );
            return ResponseEntity.ok(ApiResponse.ok(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/accept")
    @Operation(summary = "Finaliser l'inscription via token et créer le compte STAFF")
    public ResponseEntity<ApiResponse<UserResponse>> accept(@RequestBody StaffInvitationAcceptRequest request) {
        try {
            User user = invitationService.acceptInvitation(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Compte créé — vous pouvez vous connecter", UserResponse.from(user)));
        } catch (IllegalArgumentException e) {
            logger.warn("[STAFF-INVITE] Acceptation refusée: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("[STAFF-INVITE] Erreur acceptation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Impossible de finaliser l'inscription"));
        }
    }
}
