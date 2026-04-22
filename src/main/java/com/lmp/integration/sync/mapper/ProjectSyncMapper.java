package com.lmp.integration.sync.mapper;

import com.lmp.project.domain.Project;
import com.lmp.project.domain.ProjectStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper bidirectionnel Project ↔ payload externe.
 * Aucune référence à un ERP spécifique — noms de champs génériques.
 */
@Component
public class ProjectSyncMapper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Crée un nouveau Project LMP à partir d'un payload externe.
     */
    public Project toNewProject(Map<String, Object> data) {
        Project project = new Project();
        project.setTitle(getString(data, "project_name", "Untitled Project"));
        project.setDescription(getString(data, "notes", null));
        project.setStatus(mapStatusFromExternal(getString(data, "status", "Open")));
        project.setPercentComplete(getInt(data, "percent_complete", 0));
        project.setExpectedStartDate(getDate(data, "expected_start_date"));
        project.setExpectedEndDate(getDate(data, "expected_end_date"));
        project.setActualStartDate(getDate(data, "actual_start_date"));
        project.setActualEndDate(getDate(data, "actual_end_date"));
        project.setExternalProjectId(getString(data, "name", null));
        return project;
    }

    /**
     * Met à jour un Project existant depuis un payload externe.
     */
    public void updateProjectFromPayload(Project project, Map<String, Object> data) {
        if (data.containsKey("project_name")) {
            project.setTitle(getString(data, "project_name", project.getTitle()));
        }
        if (data.containsKey("notes")) {
            project.setDescription(getString(data, "notes", project.getDescription()));
        }
        if (data.containsKey("status")) {
            project.setStatus(mapStatusFromExternal(getString(data, "status", "Open")));
        }
        if (data.containsKey("percent_complete")) {
            project.setPercentComplete(getInt(data, "percent_complete", project.getPercentComplete()));
        }
        if (data.containsKey("expected_start_date")) {
            project.setExpectedStartDate(getDate(data, "expected_start_date"));
        }
        if (data.containsKey("expected_end_date")) {
            project.setExpectedEndDate(getDate(data, "expected_end_date"));
        }
        if (data.containsKey("actual_start_date")) {
            project.setActualStartDate(getDate(data, "actual_start_date"));
        }
        if (data.containsKey("actual_end_date")) {
            project.setActualEndDate(getDate(data, "actual_end_date"));
        }
    }

    /**
     * Convertit un Project LMP en payload pour le système externe.
     */
    public Map<String, Object> toOutboundPayload(Project project) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("project_name", project.getTitle());
        payload.put("notes", project.getDescription());
        payload.put("status", mapStatusToExternal(project.getStatus()));
        payload.put("percent_complete", project.getPercentComplete());
        if (project.getExpectedStartDate() != null) {
            payload.put("expected_start_date", project.getExpectedStartDate().format(DATE_FMT));
        }
        if (project.getExpectedEndDate() != null) {
            payload.put("expected_end_date", project.getExpectedEndDate().format(DATE_FMT));
        }
        return payload;
    }

    private ProjectStatus mapStatusFromExternal(String externalStatus) {
        if (externalStatus == null) return ProjectStatus.OPEN;
        return switch (externalStatus.toLowerCase()) {
            case "open" -> ProjectStatus.OPEN;
            case "working", "in progress" -> ProjectStatus.IN_PROGRESS;
            case "completed" -> ProjectStatus.COMPLETED;
            case "cancelled" -> ProjectStatus.CANCELLED;
            case "on hold" -> ProjectStatus.ON_HOLD;
            default -> ProjectStatus.OPEN;
        };
    }

    private String mapStatusToExternal(ProjectStatus status) {
        return switch (status) {
            case OPEN -> "Open";
            case IN_PROGRESS -> "Working";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
            case ON_HOLD -> "On Hold";
        };
    }

    private String getString(Map<String, Object> data, String key, String defaultValue) {
        Object val = data.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> data, String key, int defaultValue) {
        Object val = data.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); }
            catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    private LocalDate getDate(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val == null || val.toString().isBlank()) return null;
        try { return LocalDate.parse(val.toString(), DATE_FMT); }
        catch (Exception e) { return null; }
    }
}
