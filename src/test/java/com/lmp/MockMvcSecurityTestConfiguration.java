package com.lmp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

/**
 * SB 4 a retiré MockMvcSecurityConfiguration. Sans cette config, le MockMvc
 * @AutoConfigureMockMvc construit ne plug plus springSecurity() — donc
 * @WithMockUser ne propage plus l'authentication au filter chain et tous
 * les tests sécurisés retournent 401.
 *
 * Cette TestConfiguration restaure le comportement SB 3.x en injectant un
 * MockMvcBuilderCustomizer qui appelle SecurityMockMvcConfigurers.springSecurity().
 *
 * Importer via @Import(MockMvcSecurityTestConfiguration.class) sur les tests
 * qui utilisent @WithMockUser.
 */
@TestConfiguration
public class MockMvcSecurityTestConfiguration {

    @Bean
    public MockMvcBuilderCustomizer springSecurityMockMvcCustomizer() {
        return builder -> builder.apply(SecurityMockMvcConfigurers.springSecurity());
    }
}
