package com.lmp.integration.sync.mapper;

import com.lmp.project.domain.ProjectTask;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper bidirectionnel Task ↔ payload externe.
 */
@Component
public class TaskSyncMapper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Crée une nouvelle ProjectTask LMP à partir d'un payload externe.
     */
    public ProjectTask toNewTask(Map<String, Object> data) {
        ProjectTask task = new ProjectTask();
        task.setTitle(getString(data, "subject", "Untitled Task"));
        task.setDescription(getString(data, "description", null));
        task.setStatus(getString(data, "status", "Open"));
        task.setPriority(getString(data, "priority", "Medium"));
        task.setProgress(getInt(data, "progress", 0));
        task.setExpectedStartDate(getDate(data, "exp_start_date"));
        task.setExpectedEndDate(getDate(data, "exp_end_date"));
        task.setExternalTaskId(getString(data, "name", null));
        return task;
    }

    /**
     * Met à jour une ProjectTask existante depuis un payload externe.
     */
    public void updateTaskFromPayload(ProjectTask task, Map<String, Object> data) {
        if (data.containsKey("subject")) {
            task.setTitle(getString(data, "subject", task.getTitle()));
        }
        if (data.containsKey("description")) {
            task.setDescription(getString(data, "description", task.getDescription()));
        }
        if (data.containsKey("status")) {
            task.setStatus(getString(data, "status", task.getStatus()));
        }
        if (data.containsKey("priority")) {
            task.setPriority(getString(data, "priority", task.getPriority()));
        }
        if (data.containsKey("progress")) {
            task.setProgress(getInt(data, "progress", task.getProgress()));
        }
        if (data.containsKey("exp_start_date")) {
            task.setExpectedStartDate(getDate(data, "exp_start_date"));
        }
        if (data.containsKey("exp_end_date")) {
            task.setExpectedEndDate(getDate(data, "exp_end_date"));
        }
    }

    /**
     * Convertit une ProjectTask LMP en payload pour le système externe.
     */
    public Map<String, Object> toOutboundPayload(ProjectTask task, String externalProjectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("subject", task.getTitle());
        payload.put("description", task.getDescription());
        payload.put("status", task.getStatus());
        payload.put("priority", task.getPriority());
        payload.put("progress", task.getProgress());
        if (externalProjectId != null) {
            payload.put("project", externalProjectId);
        }
        if (task.getExpectedStartDate() != null) {
            payload.put("exp_start_date", task.getExpectedStartDate().format(DATE_FMT));
        }
        if (task.getExpectedEndDate() != null) {
            payload.put("exp_end_date", task.getExpectedEndDate().format(DATE_FMT));
        }
        return payload;
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
