package com.lmp.auth.oauth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import com.lmp.auth.domain.User;
import com.lmp.auth.repository.UserRepository;

/**
 * Customise les JWT access tokens et ID tokens émis par le Spring Authorization Server
 * pour y injecter les claims spécifiques à l’intégration externalCrm/externalErp.
 */
public class LmpOAuth2TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final UserRepository userRepository;

    public LmpOAuth2TokenCustomizer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void customize(JwtEncodingContext context) {
        if (!context.getPrincipal().isAuthenticated()) {
            return;
        }

        String tokenType = context.getTokenType().getValue();
        boolean isAccessToken = OAuth2TokenType.ACCESS_TOKEN.getValue().equals(tokenType);
        boolean isIdToken = OidcParameterNames.ID_TOKEN.equals(tokenType);

        if (!isAccessToken && !isIdToken) {
            return;
        }

        String email = context.getPrincipal().getName();
        Optional<User> userOpt = userRepository.findByEmailWithRoles(email);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        var claims = context.getClaims();

        claims.claim("sub", user.getId().toString());
        claims.claim("email", user.getEmail());
        claims.claim("email_verified", user.getEmailVerified());
        claims.claim("name", user.getDisplayName());
        claims.claim("given_name", user.getFirstName());
        claims.claim("family_name", user.getLastName());
        // picture claim omitted — externalCrm does not require it
        claims.claim("roles", new ArrayList<>(user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList())));
        claims.claim("lmp_user_id", user.getId().toString());

        if (user.getExternalCustomerId() != null && !user.getExternalCustomerId().isBlank()) {
            claims.claim("lmp_external_customer_id", user.getExternalCustomerId());
        }
    }
}
