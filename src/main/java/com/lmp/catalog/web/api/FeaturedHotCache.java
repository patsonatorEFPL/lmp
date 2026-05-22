package com.lmp.catalog.web.api;

import com.lmp.shared.web.PrecompressedResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared cache des réponses pré-compressées de {@code /api/v1/services/featured}
 * (key = country code ISO). Partagé entre {@link ServiceRestController} (writer)
 * et {@link FeaturedFastFilter} (reader, HIGHEST_PRECEDENCE).
 *
 * <p>Filter hit = bypass Spring MVC + filter chain complet. Controller fallback
 * pour cache miss → build + populate.</p>
 */
@Component
public class FeaturedHotCache {

    private final Map<String, PrecompressedResponse> cache = new ConcurrentHashMap<>();

    public PrecompressedResponse get(String country) {
        return cache.get(country);
    }

    public void put(String country, PrecompressedResponse r) {
        cache.put(country, r);
    }

    public Map<String, PrecompressedResponse> asMap() {
        return cache;
    }
}
