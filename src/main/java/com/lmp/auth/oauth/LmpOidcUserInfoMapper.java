package com.lmp.auth.oauth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;

import com.lmp.auth.domain.User;
import com.lmp.auth.repository.UserRepository;

/**
 * Mapper personnalisé pour l'endpoint OIDC UserInfo (/userinfo).
 * Enrichit la réponse avec les claims utilisateur nécessaires à l'intégration
 * externalCrm/externalErp (Social Login Key).
 */
public class LmpOidcUserInfoMapper implements Function<OidcUserInfoAuthenticationContext, OidcUserInfo> {

    private final UserRepository userRepository;

    public LmpOidcUserInfoMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUserInfo apply(OidcUserInfoAuthenticationContext context) {
        String principalName = context.getAuthorization().getPrincipalName();

        Optional<User> userOpt = userRepository.findByEmailWithRoles(principalName);
        if (userOpt.isEmpty()) {
            return OidcUserInfo.builder()
                    .subject(principalName)
                    .build();
        }

        User user = userOpt.get();
        Map<String, Object> claims = new HashMap<>();

        claims.put("sub", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("email_verified", Boolean.TRUE.equals(user.getEmailVerified()));
        claims.put("name", user.getDisplayName());
        claims.put("given_name", user.getFirstName());
        claims.put("family_name", user.getLastName());
        if (user.getGender() != null && !user.getGender().isBlank()) {
            claims.put("gender", user.getGender());
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            claims.put("phone_number", user.getPhone());
        }
        claims.put("roles", new ArrayList<>(user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList())));

        if (user.getExternalCustomerId() != null && !user.getExternalCustomerId().isBlank()) {
            claims.put("lmp_external_customer_id", user.getExternalCustomerId());
        }

        return new OidcUserInfo(claims);
    }
}
