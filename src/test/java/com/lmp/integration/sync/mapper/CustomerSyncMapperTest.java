package com.lmp.integration.sync.mapper;

import com.lmp.auth.domain.User;
import com.lmp.integration.sync.SyncProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CustomerSyncMapper} — pure mapping function from LMP
 * {@link User} domain to the external CRM Customer payload schema.
 *
 * <p>Tests target the public mapping contract (output keys + values for given
 * inputs), not the internal helper methods. Renaming the private helpers must
 * not break these tests.</p>
 */
class CustomerSyncMapperTest {

    private CustomerSyncMapper mapper;

    @BeforeEach
    void setUp() {
        SyncProperties props = new SyncProperties();
        // External.currency defaults to "EUR" per SyncProperties.External
        mapper = new CustomerSyncMapper(props);
    }

    @Test
    void toCreatePayload_individualUser_includesIdentityContactAndDefaults() {
        User user = individualUser();

        Map<String, Object> payload = mapper.toCreatePayload(user);

        assertThat(payload)
                .containsEntry("customer_name", "Jane Doe")
                .containsEntry("customer_type", "Individual")
                .containsEntry("email_id", "jane@example.com")
                .containsEntry("mobile_no", "+32 470 12 34 56")
                .containsEntry("territory", "BE")
                .containsEntry("customer_group", "Individual")
                .containsEntry("default_currency", "EUR");
    }

    @Test
    void toCreatePayload_companyUser_switchesTypeAndGroupToCommercial() {
        User user = individualUser();
        user.setCompanyName("Acme Corp");

        Map<String, Object> payload = mapper.toCreatePayload(user);

        // Company name overrides the personal name used for customer_name
        assertThat(payload)
                .containsEntry("customer_name", "Acme Corp")
                .containsEntry("customer_type", "Company")
                .containsEntry("customer_group", "Commercial");
    }

    @Test
    void toCreatePayload_vatReverseCharge_addsTaxCategoryAutoliquidation() {
        User user = individualUser();
        user.setVatNumber("BE0123456789");
        user.setVatReverseCharge(true);

        Map<String, Object> payload = mapper.toCreatePayload(user);

        assertThat(payload)
                .containsEntry("tax_id", "BE0123456789")
                .containsEntry("tax_category", "Autoliquidation UE");
    }

    @Test
    void toUpdatePayload_reverseChargeOff_clearsTaxCategory() {
        User user = individualUser();
        user.setVatReverseCharge(false);

        Map<String, Object> payload = mapper.toUpdatePayload(user);

        // Explicit empty string (not omitted) so the external side clears any
        // previously-set tax category. Asserting on null would let an
        // implementation that just omits the key sneak past this contract.
        assertThat(payload).containsEntry("tax_category", "");
    }

    @Test
    void toContactPayload_linksToCustomerWhenExternalIdProvided() {
        User user = individualUser();

        Map<String, Object> payload = mapper.toContactPayload(user, "CUST-0001");

        assertThat(payload)
                .containsEntry("first_name", "Jane")
                .containsEntry("last_name", "Doe")
                .containsEntry("email_id", "jane@example.com");

        @SuppressWarnings("unchecked")
        var links = (java.util.List<Map<String, Object>>) payload.get("links");
        assertThat(links).singleElement()
                .satisfies(link -> {
                    assertThat(link).containsEntry("link_doctype", "Customer");
                    assertThat(link).containsEntry("link_name", "CUST-0001");
                });
    }

    private static User individualUser() {
        User u = new User();
        u.setFirstName("Jane");
        u.setLastName("Doe");
        u.setEmail("jane@example.com");
        u.setPhone("+32 470 12 34 56");
        u.setCountry("BE");
        return u;
    }
}
