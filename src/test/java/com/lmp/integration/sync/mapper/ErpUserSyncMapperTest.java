package com.lmp.integration.sync.mapper;

import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.integration.sync.SyncProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ErpUserSyncMapper} — maps an LMP {@link User} to the
 * external ERP "User" DocType payload used to provision staff logins.
 */
class ErpUserSyncMapperTest {

    private ErpUserSyncMapper mapper;

    @BeforeEach
    void setUp() {
        // Defaults: userType="System User", roles=["Sales User"], sendWelcomeEmail=false
        mapper = new ErpUserSyncMapper(new SyncProperties());
    }

    @Test
    void toCreatePayload_writesEmailFirstNameAndConfigDefaults() {
        User user = staffUser();

        Map<String, Object> payload = mapper.toCreatePayload(user);

        assertThat(payload)
                .containsEntry("email", "alice@lmp.example")
                .containsEntry("first_name", "Alice")
                .containsEntry("last_name", "Martin")
                .containsEntry("mobile_no", "+32 470 00 00 00")
                .containsEntry("enabled", 1)
                .containsEntry("send_welcome_email", 0)
                .containsEntry("user_type", "System User");
    }

    @Test
    void toCreatePayload_rolesAreEmittedAsLinkRows() {
        Map<String, Object> payload = mapper.toCreatePayload(staffUser());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> roles = (List<Map<String, Object>>) payload.get("roles");
        assertThat(roles).singleElement()
                .satisfies(r -> assertThat(r).containsEntry("role", "Sales User"));
    }

    @Test
    void toCreatePayload_blankFirstNameDefaultsToEmptyString() {
        User user = staffUser();
        user.setFirstName(null);

        Map<String, Object> payload = mapper.toCreatePayload(user);

        assertThat(payload).containsEntry("first_name", "");
    }

    @Test
    void toCreatePayload_blankLastNameIsOmitted() {
        User user = staffUser();
        user.setLastName("");

        Map<String, Object> payload = mapper.toCreatePayload(user);

        // last_name is only emitted when non-blank — otherwise external ERP
        // would clear an existing value with the empty string.
        assertThat(payload).doesNotContainKey("last_name");
    }

    @Test
    void toUpdatePayload_lockedAccount_emitsEnabledZero() {
        User user = staffUser();
        user.setStatus(UserStatus.ACTIVE);
        user.setAccountLocked(true);

        Map<String, Object> payload = mapper.toUpdatePayload(user);

        assertThat(payload).containsEntry("enabled", 0);
    }

    @Test
    void toUpdatePayload_activeNonLocked_emitsEnabledOne() {
        User user = staffUser();
        user.setStatus(UserStatus.ACTIVE);
        user.setAccountLocked(false);

        Map<String, Object> payload = mapper.toUpdatePayload(user);

        assertThat(payload).containsEntry("enabled", 1);
    }

    private static User staffUser() {
        User u = new User();
        u.setFirstName("Alice");
        u.setLastName("Martin");
        u.setEmail("alice@lmp.example");
        u.setPhone("+32 470 00 00 00");
        return u;
    }
}
