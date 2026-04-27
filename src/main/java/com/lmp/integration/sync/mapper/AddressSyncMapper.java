package com.lmp.integration.sync.mapper;

import com.lmp.auth.domain.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit les payloads Address à envoyer au système externe.
 * <p>
 * L'Address est liée au Customer via une Dynamic Link (child table {@code links[]}).
 * Référence NextApp : {@code get_contact()} utilise {@code tabDynamic Link}.
 */
@Component
public class AddressSyncMapper {

    /**
     * Construit le payload de création d'une Address liée à un Customer.
     *
     * @param user             le User LMP source
     * @param customerExternalId l'ID externe du Customer external ERP auquel lier l'Address
     */
    public Map<String, Object> toCreatePayload(User user, String customerExternalId) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("address_title", buildAddressTitle(user));
        payload.put("address_type", "Billing");

        if (user.getAddress() != null && !user.getAddress().isBlank()) {
            payload.put("address_line1", user.getAddress());
        } else {
            payload.put("address_line1", "N/A");
        }

        if (user.getCity() != null) payload.put("city", user.getCity());
        if (user.getPostalCode() != null) payload.put("pincode", user.getPostalCode());
        if (user.getCountry() != null) payload.put("country", user.getCountry());
        if (user.getEmail() != null) payload.put("email_id", user.getEmail());
        if (user.getPhone() != null) payload.put("phone", user.getPhone());

        // Dynamic Link vers le Customer
        if (customerExternalId != null && !customerExternalId.isBlank()) {
            List<Map<String, Object>> links = new ArrayList<>();
            links.add(Map.of(
                    "doctype", "Dynamic Link",
                    "link_doctype", "Customer",
                    "link_name", customerExternalId
            ));
            payload.put("links", links);
        }

        return payload;
    }

    /**
     * Construit le payload de mise à jour d'une Address existante.
     * Les Dynamic Links ne sont pas modifiées (external ERP les gère).
     */
    public Map<String, Object> toUpdatePayload(User user) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("address_title", buildAddressTitle(user));
        payload.put("address_line1", user.getAddress() != null && !user.getAddress().isBlank()
                ? user.getAddress() : "N/A");
        if (user.getCity() != null) payload.put("city", user.getCity());
        if (user.getPostalCode() != null) payload.put("pincode", user.getPostalCode());
        if (user.getCountry() != null) payload.put("country", user.getCountry());
        if (user.getEmail() != null) payload.put("email_id", user.getEmail());
        if (user.getPhone() != null) payload.put("phone", user.getPhone());

        return payload;
    }

    /**
     * Vérifie si l'utilisateur a au moins un champ d'adresse renseigné.
     */
    public boolean hasAddressData(User user) {
        return (user.getAddress() != null && !user.getAddress().isBlank())
                || (user.getCity() != null && !user.getCity().isBlank())
                || (user.getPostalCode() != null && !user.getPostalCode().isBlank())
                || (user.getCountry() != null && !user.getCountry().isBlank());
    }

    private String buildAddressTitle(User user) {
        StringBuilder title = new StringBuilder();
        if (user.getFirstName() != null) title.append(user.getFirstName());
        if (user.getLastName() != null) {
            if (title.length() > 0) title.append(" ");
            title.append(user.getLastName());
        }
        if (title.length() == 0) {
            title.append(user.getEmail() != null ? user.getEmail() : "Adresse client");
        }
        return title.toString();
    }
}
