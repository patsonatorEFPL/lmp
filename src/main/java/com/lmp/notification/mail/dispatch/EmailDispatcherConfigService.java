package com.lmp.notification.mail.dispatch;

import com.lmp.shared.config.site.SiteConfigEntry;
import com.lmp.shared.config.site.SiteConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runtime configuration of the active email dispatcher strategy.
 *
 * <p>Persisted in {@code site_config_entry} under key {@value #DB_KEY} so the
 * admin choice survives pod restarts. In-memory {@link AtomicReference} caches
 * the value to avoid a DB hit on every send. Single-replica today; multi-replica
 * needs a sync event (cf [[project_email_dispatch_stateless_debt]]).</p>
 */
@Service
public class EmailDispatcherConfigService {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcherConfigService.class);

    public static final String STRATEGY_SMTP = "smtp";
    public static final String STRATEGY_ERPNEXT = "erpnext";
    public static final String DB_KEY = "lmp.mail.dispatcher";
    private static final String LEGACY_ALIAS = "external-crm";
    private static final Set<String> KNOWN = Set.of(STRATEGY_SMTP, STRATEGY_ERPNEXT);

    private final SiteConfigRepository repository;
    private final AtomicReference<String> activeStrategy = new AtomicReference<>();

    @Value("${lmp.mail.dispatcher:smtp}")
    private String envDefaultStrategy;

    public EmailDispatcherConfigService(SiteConfigRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void init() {
        String dbValue = repository.findByKey(DB_KEY)
                .map(SiteConfigEntry::getValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(null);

        String initial = dbValue != null ? dbValue : envDefaultStrategy;
        String normalized;
        try {
            normalized = normalize(initial);
        } catch (IllegalArgumentException e) {
            log.warn("Stored dispatcher value '{}' invalid — falling back to '{}'", initial, STRATEGY_SMTP);
            normalized = STRATEGY_SMTP;
        }
        activeStrategy.set(normalized);
        log.info("📧 [DISPATCHER] Strategy loaded from {} : {}",
                dbValue != null ? "DB" : "env", normalized);
    }

    public String getActiveStrategy() {
        return activeStrategy.get();
    }

    @Transactional
    public void setActiveStrategy(String strategy) {
        String normalized = normalize(strategy);
        String previous = activeStrategy.get();

        SiteConfigEntry entry = repository.findByKey(DB_KEY)
                .orElseGet(() -> new SiteConfigEntry(DB_KEY, normalized,
                        "Active email dispatcher strategy (smtp|erpnext)"));
        entry.setValue(normalized);
        repository.save(entry);

        activeStrategy.set(normalized);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth != null ? auth.getName() : "anonymous";
        log.info("📧 [DISPATCHER] Strategy switched {} -> {} by user={} (persisted)",
                previous, normalized, actor);
    }

    /** Test seam : reload from DB without restart (e.g. external mutation). */
    public void reloadFromDb() {
        Optional<String> dbValue = repository.findByKey(DB_KEY)
                .map(SiteConfigEntry::getValue)
                .filter(v -> v != null && !v.isBlank());
        if (dbValue.isPresent()) {
            try {
                activeStrategy.set(normalize(dbValue.get()));
            } catch (IllegalArgumentException ignored) {
                // keep current value
            }
        }
    }

    private String normalize(String strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Dispatcher strategy required (smtp|erpnext)");
        }
        String s = strategy.trim().toLowerCase(Locale.ROOT);
        if (LEGACY_ALIAS.equals(s)) {
            log.warn("Deprecated dispatcher alias 'external-crm' — use 'erpnext'");
            return STRATEGY_ERPNEXT;
        }
        if (!KNOWN.contains(s)) {
            throw new IllegalArgumentException(
                    "Unknown dispatcher strategy: " + strategy + " (expected smtp|erpnext)");
        }
        return s;
    }
}
