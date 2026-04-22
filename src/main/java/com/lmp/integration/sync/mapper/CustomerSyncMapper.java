package com.lmp.integration.sync.mapper;

import com.lmp.auth.domain.User;
import com.lmp.integration.sync.SyncProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Construit le payload Customer à envoyer au système externe à partir d'un User LMP.
 * <p>
 * Les clés du payload correspondent aux champs attendus par le système externe.
 * Ce mapping est le point unique de traduction User → Customer.
 */
@Component
public class CustomerSyncMapper {

    private final SyncProperties syncProperties;

    public CustomerSyncMapper(SyncProperties syncProperties) {
        this.syncProperties = syncProperties;
    }

    /**
     * Construit le payload de création pour un nouveau Customer.
     */
    public Map<String, Object> toCreatePayload(User user) {
        Map<String, Object> payload = new LinkedHashMap<>();

        // Identification
        payload.put("customer_name", buildCustomerName(user));
        payload.put("customer_type", resolveCustomerType(user));

        // Contact info
        if (user.getEmail() != null) payload.put("email_id", user.getEmail());
        if (user.getPhone() != null) payload.put("mobile_no", user.getPhone());

        // Entreprise / TVA
        if (user.getVatNumber() != null) payload.put("tax_id", user.getVatNumber());
        if (Boolean.TRUE.equals(user.getVatReverseCharge())) {
            payload.put("tax_category", "Autoliquidation UE");
        }

        // Territoire / Langue / Devise
        if (user.getCountry() != null) payload.put("territory", user.getCountry());
        payload.put("customer_group", resolveCustomerGroup(user));
        payload.put("default_currency", syncProperties.getExternal().getCurrency());

        return payload;
    }

    /**
     * Construit le payload de mise à jour pour un Customer existant.
     */
    public Map<String, Object> toUpdatePayload(User user) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("customer_name", buildCustomerName(user));
        payload.put("customer_type", resolveCustomerType(user));

        if (user.getPhone() != null) payload.put("mobile_no", user.getPhone());
        if (user.getVatNumber() != null) payload.put("tax_id", user.getVatNumber());

        if (Boolean.TRUE.equals(user.getVatReverseCharge())) {
            payload.put("tax_category", "Autoliquidation UE");
        } else {
            payload.put("tax_category", "");
        }

        return payload;
    }

    /**
     * Construit le payload Contact associé au Customer.
     */
    public Map<String, Object> toContactPayload(User user, String customerExternalId) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("first_name", user.getFirstName() != null ? user.getFirstName() : "");
        payload.put("last_name", user.getLastName() != null ? user.getLastName() : "");
        payload.put("email_id", user.getEmail());
        if (user.getPhone() != null) payload.put("mobile_no", user.getPhone());
        if (user.getGender() != null) payload.put("gender", capitalizeGender(user.getGender()));

        // Lien vers le Customer
        if (customerExternalId != null) {
            payload.put("links", java.util.List.of(
                    Map.of("link_doctype", "Customer", "link_name", customerExternalId)
            ));
        }

        return payload;
    }

    private String buildCustomerName(User user) {
        if (user.getCompanyName() != null && !user.getCompanyName().isBlank()) {
            return user.getCompanyName();
        }
        StringBuilder name = new StringBuilder();
        if (user.getFirstName() != null) name.append(user.getFirstName());
        if (user.getLastName() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(user.getLastName());
        }
        return name.length() > 0 ? name.toString() : user.getEmail();
    }

    private String resolveCustomerType(User user) {
        return (user.getCompanyName() != null && !user.getCompanyName().isBlank())
                ? "Company" : "Individual";
    }

    private String resolveCustomerGroup(User user) {
        return (user.getCompanyName() != null && !user.getCompanyName().isBlank())
                ? "Commercial" : "Individual";
    }

    private String capitalizeGender(String gender) {
        if (gender == null || gender.isBlank()) return "";
        return gender.substring(0, 1).toUpperCase() + gender.substring(1).toLowerCase();
    }
}
