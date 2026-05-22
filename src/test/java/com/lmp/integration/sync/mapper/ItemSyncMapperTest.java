package com.lmp.integration.sync.mapper;

import com.lmp.catalog.domain.Service;
import com.lmp.catalog.domain.ServiceCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ItemSyncMapper} — bi-directional translation between
 * LMP {@link Service} and the external ERP "Item" DocType payload.
 */
class ItemSyncMapperTest {

    private ItemSyncMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ItemSyncMapper();
    }

    // ==================== Outbound (Service → external Item payload) ====================

    @Test
    void toItemCreatePayload_activeService_emitsExpectedKeys() {
        Service service = new Service();
        service.setTitle("Web Hosting");
        service.setDescription("Managed cPanel hosting");
        service.setActive(true);

        Map<String, Object> payload = mapper.toItemCreatePayload(service);

        assertThat(payload)
                .containsEntry("item_code", "Web Hosting")
                .containsEntry("item_name", "Web Hosting")
                .containsEntry("item_group", "Services")
                .containsEntry("stock_uom", "Nos")
                .containsEntry("is_stock_item", 0)
                .containsEntry("is_sales_item", 1)
                .containsEntry("disabled", 0)
                .containsEntry("description", "Managed cPanel hosting");
    }

    @Test
    void toItemCreatePayload_inactiveService_emitsDisabledOne() {
        Service service = new Service();
        service.setTitle("Legacy Service");
        service.setActive(false);

        Map<String, Object> payload = mapper.toItemCreatePayload(service);

        assertThat(payload).containsEntry("disabled", 1);
    }

    @Test
    void toItemCreatePayload_blankDescription_omitsDescriptionKey() {
        Service service = new Service();
        service.setTitle("Service Without Description");
        service.setActive(true);
        service.setDescription("");

        Map<String, Object> payload = mapper.toItemCreatePayload(service);

        // Empty description must NOT be sent — external ERP would overwrite a
        // localized description on the external side with an empty string.
        assertThat(payload).doesNotContainKey("description");
    }

    // ==================== Inbound (external Item payload → Service) ====================

    @Test
    void updateServiceFromPayload_appliesNameDescriptionAndActive() {
        Service service = new Service();
        service.setTitle("old title");
        service.setActive(true);

        Map<String, Object> data = new HashMap<>();
        data.put("item_name", "New Title");
        data.put("description", "<p>HTML <b>stripped</b></p>");
        data.put("disabled", 1);  // truthy → service should become inactive

        mapper.updateServiceFromPayload(service, data);

        assertThat(service.getTitle()).isEqualTo("New Title");
        assertThat(service.getDescription()).isEqualTo("HTML stripped");
        assertThat(service.getActive()).isFalse();
        assertThat(service.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateServiceFromPayload_missingKeys_areLeftUnchanged() {
        Service service = new Service();
        service.setTitle("kept");
        service.setActive(true);

        // Payload only has 'image' — title/description/active must be untouched.
        Map<String, Object> data = Map.of("image", "/files/icon.png");

        mapper.updateServiceFromPayload(service, data);

        assertThat(service.getTitle()).isEqualTo("kept");
        assertThat(service.getActive()).isTrue();
        assertThat(service.getIcon()).isEqualTo("/files/icon.png");
    }

    @Test
    void toNewService_populatesDefaultsAndExternalItemCode() {
        ServiceCategory category = new ServiceCategory();
        Map<String, Object> data = Map.of(
                "item_name", "Imported Service",
                "description", "<p>Body</p>",
                "disabled", false,
                "name", "ITEM-EXT-001"
        );

        Service service = mapper.toNewService(data, category);

        assertThat(service.getTitle()).isEqualTo("Imported Service");
        assertThat(service.getSlug()).isEqualTo("imported-service");
        assertThat(service.getDescription()).isEqualTo("Body");
        assertThat(service.getActive()).isTrue();
        assertThat(service.getCategory()).isSameAs(category);
        assertThat(service.getDisplayOrder()).isEqualTo(0);
        assertThat(service.getFeatured()).isFalse();
        assertThat(service.getExternalItemCode()).isEqualTo("ITEM-EXT-001");
    }

    // ==================== Utility ====================

    @Test
    void generateSlug_normalisesAccentsCaseAndWhitespace() {
        String slug = mapper.generateSlug("Création de Sites Web — Évoluée");

        // Em-dash gets stripped by [^a-z0-9\s-]; the surrounding spaces then
        // collapse via \s+→- + -+→- to a single hyphen.
        assertThat(slug).isEqualTo("creation-de-sites-web-evoluee");
    }

    @Test
    void generateSlug_nullInput_returnsFallback() {
        String slug = mapper.generateSlug(null);

        // Deterministic prefix lets us assert without coupling to the
        // implementation's exact timestamp value.
        assertThat(slug).startsWith("untitled-");
    }
}
