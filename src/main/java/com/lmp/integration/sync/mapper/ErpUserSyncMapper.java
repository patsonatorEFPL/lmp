package com.lmp.integration.sync.mapper;

import com.lmp.auth.domain.User;
import com.lmp.integration.sync.SyncProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit le payload ERPNext "User" (DocType login) à partir d'un collaborateur LMP.
 * <p>
 * À utiliser quand le User LMP a le rôle STAFF (ou ADMIN) — sinon passer par
 * {@link CustomerSyncMapper}. La clé primaire ERPNext d'un User est l'email.
 * <p>
 * Référence pattern : {@code python/crm/crm/demo/users.py} (frappe.get_doc User).
 */
@Component
public class ErpUserSyncMapper {

    private final SyncProperties syncProperties;

    public ErpUserSyncMapper(SyncProperties syncProperties) {
        this.syncProperties = syncProperties;
    }

    public Map<String, Object> toCreatePayload(User user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        SyncProperties.ErpUser cfg = syncProperties.getExternal().getErpUser();

        payload.put("email", user.getEmail());
        payload.put("first_name", safe(user.getFirstName()));
        if (user.getLastName() != null && !user.getLastName().isBlank()) {
            payload.put("last_name", user.getLastName());
        }
        if (user.getPhone() != null) payload.put("mobile_no", user.getPhone());

        payload.put("enabled", 1);
        payload.put("send_welcome_email", cfg.isSendWelcomeEmail() ? 1 : 0);
        payload.put("user_type", cfg.getUserType());

        List<Map<String, Object>> rolesPayload = new ArrayList<>();
        for (String role : cfg.getRoles()) {
            if (role == null || role.isBlank()) continue;
            rolesPayload.add(Map.of("role", role));
        }
        if (!rolesPayload.isEmpty()) {
            payload.put("roles", rolesPayload);
        }

        return payload;
    }

    public Map<String, Object> toUpdatePayload(User user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("first_name", safe(user.getFirstName()));
        if (user.getLastName() != null) payload.put("last_name", user.getLastName());
        if (user.getPhone() != null) payload.put("mobile_no", user.getPhone());
        payload.put("enabled", isUserEnabled(user) ? 1 : 0);
        return payload;
    }

    private boolean isUserEnabled(User user) {
        if (user.getStatus() == null) return true;
        return user.getStatus().name().equals("ACTIVE")
                && !Boolean.TRUE.equals(user.getAccountLocked());
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
