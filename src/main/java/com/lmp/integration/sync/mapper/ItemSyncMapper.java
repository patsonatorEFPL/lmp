package com.lmp.integration.sync.mapper;

import com.lmp.catalog.domain.Service;
import com.lmp.catalog.domain.ServiceCategory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mappe les données d'un Item externe vers un Service LMP.
 * <p>
 * Les clés du payload correspondent aux champs envoyés par le système externe
 * via webhook. Ce mapper est le point unique de traduction Item → Service.
 */
@Component
public class ItemSyncMapper {

    // ==================== Outbound (LMP → système externe) ====================

    /**
     * Construit le payload pour créer un Item dans le système externe
     * à partir d'un Service LMP.
     */
    public Map<String, Object> toItemCreatePayload(Service service) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("item_code", service.getTitle());
        payload.put("item_name", service.getTitle());
        payload.put("item_group", "Services");
        payload.put("stock_uom", "Nos");
        payload.put("is_stock_item", 0);
        payload.put("is_sales_item", 1);
        payload.put("disabled", Boolean.TRUE.equals(service.getActive()) ? 0 : 1);
        if (service.getDescription() != null && !service.getDescription().isBlank()) {
            payload.put("description", service.getDescription());
        }
        return payload;
    }

    // ==================== Inbound (système externe → LMP) ====================

    /**
     * Met à jour un Service existant avec les données de l'Item externe.
     * Ne touche pas aux champs non présents dans le payload.
     */
    public void updateServiceFromPayload(Service service, Map<String, Object> data) {
        if (data.containsKey("item_name")) {
            service.setTitle((String) data.get("item_name"));
        }
        if (data.containsKey("description")) {
            String desc = (String) data.get("description");
            if (desc != null && !desc.isBlank()) {
                // Strip HTML tags if present (external CRM stores rich text)
                service.setDescription(stripHtml(desc));
            }
        }
        if (data.containsKey("disabled")) {
            Object disabled = data.get("disabled");
            service.setActive(!isTruthy(disabled));
        }
        if (data.containsKey("image")) {
            service.setIcon((String) data.get("image"));
        }

        service.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Crée un nouveau Service à partir d'un payload Item externe.
     */
    public Service toNewService(Map<String, Object> data, ServiceCategory category) {
        Service service = new Service();

        service.setTitle(getStringOrDefault(data, "item_name", "Untitled"));
        service.setSlug(generateSlug(service.getTitle()));
        service.setDescription(stripHtml(getStringOrDefault(data, "description", "")));
        service.setActive(!isTruthy(data.get("disabled")));
        service.setCategory(category);
        service.setDisplayOrder(0);
        service.setFeatured(false);
        service.setCreatedAt(LocalDateTime.now());
        service.setUpdatedAt(LocalDateTime.now());

        if (data.containsKey("image")) {
            service.setIcon((String) data.get("image"));
        }
        if (data.containsKey("name")) {
            service.setExternalItemCode((String) data.get("name"));
        }

        return service;
    }

    /**
     * Génère un slug URL-safe à partir d'un titre.
     */
    public String generateSlug(String title) {
        if (title == null) return "untitled-" + System.currentTimeMillis();
        return title.toLowerCase()
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").trim();
    }

    private String getStringOrDefault(Map<String, Object> data, String key, String defaultVal) {
        Object val = data.get(key);
        return (val instanceof String s && !s.isBlank()) ? s : defaultVal;
    }

    private boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean b) return b;
        if (val instanceof Number n) return n.intValue() != 0;
        if (val instanceof String s) return "1".equals(s) || "true".equalsIgnoreCase(s);
        return false;
    }
}
