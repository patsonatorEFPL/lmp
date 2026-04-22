package com.lmp.integration.sync.mapper;

import com.lmp.support.domain.Ticket;
import com.lmp.support.domain.TicketPriority;
import com.lmp.support.domain.TicketStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapper bidirectionnel Ticket (Issue) ↔ payload externe.
 */
@Component
public class TicketSyncMapper {

    /**
     * Crée un nouveau Ticket LMP à partir d'un payload externe (Issue).
     */
    public Ticket toNewTicket(Map<String, Object> data) {
        Ticket ticket = new Ticket();
        ticket.setSubject(getString(data, "subject", "Untitled Issue"));
        ticket.setDescription(getString(data, "description", null));
        ticket.setStatus(mapStatusFromExternal(getString(data, "status", "Open")));
        ticket.setPriority(mapPriorityFromExternal(getString(data, "priority", "Medium")));
        ticket.setTicketType(getString(data, "issue_type", null));
        ticket.setResolutionDetails(getString(data, "resolution_details", null));
        ticket.setExternalIssueId(getString(data, "name", null));
        return ticket;
    }

    /**
     * Met à jour un Ticket existant depuis un payload externe.
     */
    public void updateTicketFromPayload(Ticket ticket, Map<String, Object> data) {
        if (data.containsKey("subject")) {
            ticket.setSubject(getString(data, "subject", ticket.getSubject()));
        }
        if (data.containsKey("description")) {
            ticket.setDescription(getString(data, "description", ticket.getDescription()));
        }
        if (data.containsKey("status")) {
            ticket.setStatus(mapStatusFromExternal(getString(data, "status", "Open")));
        }
        if (data.containsKey("priority")) {
            ticket.setPriority(mapPriorityFromExternal(getString(data, "priority", "Medium")));
        }
        if (data.containsKey("issue_type")) {
            ticket.setTicketType(getString(data, "issue_type", ticket.getTicketType()));
        }
        if (data.containsKey("resolution_details")) {
            ticket.setResolutionDetails(getString(data, "resolution_details", null));
        }
    }

    /**
     * Convertit un Ticket LMP en payload pour le système externe.
     */
    public Map<String, Object> toOutboundPayload(Ticket ticket, String externalCustomerId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("subject", ticket.getSubject());
        payload.put("description", ticket.getDescription());
        payload.put("status", mapStatusToExternal(ticket.getStatus()));
        payload.put("priority", mapPriorityToExternal(ticket.getPriority()));
        if (ticket.getTicketType() != null) {
            payload.put("issue_type", ticket.getTicketType());
        }
        if (externalCustomerId != null) {
            payload.put("customer", externalCustomerId);
        }
        if (ticket.getResolutionDetails() != null) {
            payload.put("resolution_details", ticket.getResolutionDetails());
        }
        return payload;
    }

    private TicketStatus mapStatusFromExternal(String externalStatus) {
        if (externalStatus == null) return TicketStatus.OPEN;
        return switch (externalStatus.toLowerCase()) {
            case "open" -> TicketStatus.OPEN;
            case "replied" -> TicketStatus.REPLIED;
            case "resolved" -> TicketStatus.RESOLVED;
            case "closed" -> TicketStatus.CLOSED;
            case "on hold", "hold" -> TicketStatus.ON_HOLD;
            default -> TicketStatus.OPEN;
        };
    }

    private String mapStatusToExternal(TicketStatus status) {
        return switch (status) {
            case OPEN -> "Open";
            case REPLIED -> "Replied";
            case RESOLVED -> "Resolved";
            case CLOSED -> "Closed";
            case ON_HOLD -> "On Hold";
        };
    }

    private TicketPriority mapPriorityFromExternal(String priority) {
        if (priority == null) return TicketPriority.MEDIUM;
        return switch (priority.toLowerCase()) {
            case "low" -> TicketPriority.LOW;
            case "medium" -> TicketPriority.MEDIUM;
            case "high" -> TicketPriority.HIGH;
            case "urgent", "critical" -> TicketPriority.URGENT;
            default -> TicketPriority.MEDIUM;
        };
    }

    private String mapPriorityToExternal(TicketPriority priority) {
        return switch (priority) {
            case LOW -> "Low";
            case MEDIUM -> "Medium";
            case HIGH -> "High";
            case URGENT -> "Urgent";
        };
    }

    private String getString(Map<String, Object> data, String key, String defaultValue) {
        Object val = data.get(key);
        return val != null ? val.toString() : defaultValue;
    }
}
