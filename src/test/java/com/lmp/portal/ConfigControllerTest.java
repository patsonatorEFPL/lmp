package com.lmp.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.shared.config.site.SiteConfigManager;
import com.lmp.shared.web.AuthHostResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigControllerTest {

    @Mock
    private SiteConfigManager siteConfigManager;

    @Mock
    private AuthHostResolver authHostResolver;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ConfigController configController;

    @BeforeEach
    void setUp() {
        configController = new ConfigController(siteConfigManager, authHostResolver, objectMapper);
        when(siteConfigManager.getSiteName()).thenReturn("LMP Digital Services");
        when(siteConfigManager.getSupportEmail()).thenReturn("support@lmp-services.ca");
        when(siteConfigManager.getContactEmail()).thenReturn("info@lmp-services.ca");
        when(siteConfigManager.getNoreplyEmail()).thenReturn("noreply@lmp-services.ca");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getConfig_withLmpeoHost_returnsLmpeoUrls() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("lmpeo.com");
        request.setServerPort(443);
        request.setContextPath("");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ResponseEntity<byte[]> response = configController.getConfig(request);

        assertNotNull(response.getBody());
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().get("Vary").contains("Host, Accept-Encoding"));

        @SuppressWarnings("unchecked")
        Map<String, String> config = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("https://lmpeo.com", config.get("baseUrl"));
        assertEquals("https://lmpeo.com", config.get("frontendUrl"));
        assertEquals("https://lmpeo.com", config.get("authBaseUrl"));
        assertEquals("https://lmpeo.com/login", config.get("loginUrl"));
        assertEquals("https://lmpeo.com/register", config.get("registerUrl"));
    }

    @Test
    void getConfig_withLmpServicesHost_returnsLmpServicesUrls() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("lmp-services.ca");
        request.setServerPort(443);
        request.setContextPath("");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ResponseEntity<byte[]> response = configController.getConfig(request);

        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, String> config = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("https://lmp-services.ca", config.get("baseUrl"));
        assertEquals("https://lmp-services.ca/login", config.get("loginUrl"));
    }

    @Test
    void getConfig_cachesPerDomainWithoutCrossBleed() throws IOException {
        // First call with lmpeo.com
        MockHttpServletRequest requestLmpeo = new MockHttpServletRequest();
        requestLmpeo.setScheme("https");
        requestLmpeo.setServerName("lmpeo.com");
        requestLmpeo.setServerPort(443);
        requestLmpeo.setContextPath("");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestLmpeo));

        ResponseEntity<byte[]> respLmpeo = configController.getConfig(requestLmpeo);
        @SuppressWarnings("unchecked")
        Map<String, String> configLmpeo = objectMapper.readValue(respLmpeo.getBody(), Map.class);
        assertEquals("https://lmpeo.com", configLmpeo.get("baseUrl"));

        // Second call with lmp-services.ca must NOT return cached lmpeo.com
        MockHttpServletRequest requestLmp = new MockHttpServletRequest();
        requestLmp.setScheme("https");
        requestLmp.setServerName("lmp-services.ca");
        requestLmp.setServerPort(443);
        requestLmp.setContextPath("");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestLmp));

        ResponseEntity<byte[]> respLmp = configController.getConfig(requestLmp);
        @SuppressWarnings("unchecked")
        Map<String, String> configLmp = objectMapper.readValue(respLmp.getBody(), Map.class);
        assertEquals("https://lmp-services.ca", configLmp.get("baseUrl"));
    }
}
