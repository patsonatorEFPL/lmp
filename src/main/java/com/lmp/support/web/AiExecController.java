package com.lmp.support.web;

import com.lmp.support.ai.AiSuggestionOutcome;
import com.lmp.support.ai.AiSuggestionService;
import com.lmp.support.domain.SessionStatus;
import com.lmp.support.domain.SupportSession;
import com.lmp.support.service.SupportSessionService;
import com.lmp.support.web.dto.AiSuggestRequest;
import com.lmp.support.web.dto.AiSuggestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * AI-assisted command suggestion endpoint. Approval + actual execution will be
 * added once the MeshCentral shell channel binding and ExecutedCommand entity
 * land (deferred — P7 DB tasks).
 */
@RestController
@RequestMapping("/api/v1/support/sessions")
public class AiExecController {

    private final AiSuggestionService suggestionService;
    private final SupportSessionService sessionService;

    public AiExecController(AiSuggestionService suggestionService,
                            SupportSessionService sessionService) {
        this.suggestionService = suggestionService;
        this.sessionService = sessionService;
    }

    @PostMapping("/{id}/ai/suggest")
    @PreAuthorize("hasAnyRole('TECH','SUPPORT','ADMIN')")
    public AiSuggestResponse suggest(@PathVariable UUID id,
                                     @RequestBody AiSuggestRequest req) {
        if (req == null || req.prompt() == null || req.prompt().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt required");
        }
        SupportSession s = sessionService.findById(id);
        if (s.getStatus() != SessionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "AI suggest requires ACTIVE session; current=" + s.getStatus());
        }
        String ctx = "Session " + s.getId() + " status=" + s.getStatus();
        AiSuggestionOutcome outcome = suggestionService.suggest(s.getId(), req.prompt(), ctx);
        return AiSuggestResponse.from(outcome);
    }
}
