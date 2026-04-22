package com.lmp.integration.sync.mapper;

import com.lmp.catalog.domain.ServiceCategory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mappe les données d'un Item Group externe vers une ServiceCategory LMP.
 * <p>
 * Les clés du payload correspondent aux champs envoyés par le système externe
 * via webhook. Ce mapper est le point unique de traduction Item Group → ServiceCategory.
 */
@Component
public class ItemGroupSyncMapper {

    /**
     * Met à jour une ServiceCategory existante avec les données de l'Item Group externe.
     */
    public void updateCategoryFromPayload(ServiceCategory category, Map<String, Object> data) {
        if (data.containsKey("item_group_name")) {
            category.setName((String) data.get("item_group_name"));
            category.setSlug(generateSlug((String) data.get("item_group_name")));
        } else if (data.containsKey("name")) {
            category.setName((String) data.get("name"));
            category.setSlug(generateSlug((String) data.get("name")));
        }

        if (data.containsKey("description")) {
            String desc = (String) data.get("description");
            category.setDescription(desc != null ? desc.replaceAll("<[^>]*>", "").trim() : null);
        }

        if (data.containsKey("image")) {
            category.setIcon((String) data.get("image"));
        }
    }

    /**
     * Crée une nouvelle ServiceCategory à partir d'un payload Item Group externe.
     */
    public ServiceCategory toNewCategory(Map<String, Object> data) {
        String name = getStringOrDefault(data, "item_group_name",
                getStringOrDefault(data, "name", "Uncategorized"));

        ServiceCategory category = new ServiceCategory();
        category.setName(name);
        category.setSlug(generateSlug(name));
        category.setDisplayOrder(0);

        if (data.containsKey("description")) {
            String desc = (String) data.get("description");
            category.setDescription(desc != null ? desc.replaceAll("<[^>]*>", "").trim() : null);
        }
        if (data.containsKey("image")) {
            category.setIcon((String) data.get("image"));
        }
        if (data.containsKey("name")) {
            category.setExternalGroupId((String) data.get("name"));
        }

        return category;
    }

    private String generateSlug(String name) {
        if (name == null) return "uncategorized-" + System.currentTimeMillis();
        return name.toLowerCase()
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

    private String getStringOrDefault(Map<String, Object> data, String key, String defaultVal) {
        Object val = data.get(key);
        return (val instanceof String s && !s.isBlank()) ? s : defaultVal;
    }
}
