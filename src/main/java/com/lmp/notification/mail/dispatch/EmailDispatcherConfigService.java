package com.lmp.notification.mail.dispatch;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runtime configuration of the active email dispatcher strategy.
 *
 * <p>In-memory only — pod restart reverts to {@code lmp.mail.dispatcher} property.
 * Single-replica deployment today (cf prod-monolithic-merged-2026-05-22); persist
 * to admin_settings table when multi-replica.</p>
 */
@Service
public class EmailDispatcherConfigService {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcherConfigService.class);

    public static final String STRATEGY_SMTP = "smtp";
    public static final String STRATEGY_ERPNEXT = "erpnext";
    private static final String LEGACY_ALIAS = "external-crm";
    private static final Set<String> KNOWN = Set.of(STRATEGY_SMTP, STRATEGY_ERPNEXT);

    private final AtomicReference<String> activeStrategy = new AtomicReference<>();

    @Value("${lmp.mail.dispatcher:smtp}")
    private String defaultStrategy;

    @PostConstruct
    void init() {
        String normalized = normalize(defaultStrategy);
        activeStrategy.set(normalized);
        log.info("📧 [DISPATCHER] Default strategy loaded: {}", normalized);
    }

    public String getActiveStrategy() {
        return activeStrategy.get();
    }

    public void setActiveStrategy(String strategy) {
        String normalized = normalize(strategy);
        String previous = activeStrategy.getAndSet(normalized);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth != null ? auth.getName() : "anonymous";
        log.info("📧 [DISPATCHER] Strategy switched {} -> {} by user={}", previous, normalized, actor);
    }

    private String normalize(String strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Dispatcher strategy required (smtp|erpnext)");
        }
        String s = strategy.trim().toLowerCase(java.util.Locale.ROOT);
        if (LEGACY_ALIAS.equals(s)) {
            log.warn("Deprecated dispatcher alias 'external-crm' — use 'erpnext'");
            return STRATEGY_ERPNEXT;
        }
        if (!KNOWN.contains(s)) {
            throw new IllegalArgumentException("Unknown dispatcher strategy: " + strategy + " (expected smtp|erpnext)");
        }
        return s;
    }
}
