package com.lmp.notification.mail.dispatch;

import com.lmp.shared.config.site.SiteConfigEntry;
import com.lmp.shared.config.site.SiteConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stateless runtime configuration of the active email dispatcher strategy.
 *
 * <ul>
 *   <li><b>Source of truth</b> : {@code site_config} row {@value #DB_KEY}. Survives
 *       restart. Read on boot, refreshed on broadcast.</li>
 *   <li><b>Per-pod hot path</b> : {@link AtomicReference} cache so {@link #getActiveStrategy()}
 *       stays lock-free on the email-send path.</li>
 *   <li><b>Cross-pod sync</b> : Redis pub/sub channel {@value #CHANNEL}. Any pod
 *       updating the value publishes the new value; all pods (including the
 *       publisher, loopback) refresh their cache → eventual consistency in ms.</li>
 * </ul>
 */
@Service
public class EmailDispatcherConfigService {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcherConfigService.class);

    public static final String STRATEGY_SMTP = "smtp";
    public static final String STRATEGY_ERPNEXT = "erpnext";
    public static final String DB_KEY = "lmp.mail.dispatcher";
    public static final String CHANNEL = "lmp:dispatcher:changed";

    private static final String LEGACY_ALIAS = "external-crm";
    private static final Set<String> KNOWN = Set.of(STRATEGY_SMTP, STRATEGY_ERPNEXT);

    private final SiteConfigRepository repository;
    private final StringRedisTemplate redis;
    private final RedisMessageListenerContainer listenerContainer;
    private final AtomicReference<String> activeStrategy = new AtomicReference<>();

    @Value("${lmp.mail.dispatcher:smtp}")
    private String envDefaultStrategy;

    public EmailDispatcherConfigService(SiteConfigRepository repository,
                                        StringRedisTemplate redis,
                                        RedisMessageListenerContainer listenerContainer) {
        this.repository = repository;
        this.redis = redis;
        this.listenerContainer = listenerContainer;
    }

    @PostConstruct
    void init() {
        loadFromSource();
        MessageListener listener = (message, pattern) -> {
            String payload = new String(message.getBody());
            try {
                activeStrategy.set(normalize(payload));
                log.info("[DISPATCHER] Cache refreshed from pub/sub : {}", payload);
            } catch (IllegalArgumentException e) {
                log.warn("[DISPATCHER] Ignored invalid pub/sub payload '{}'", payload);
            }
        };
        listenerContainer.addMessageListener(listener, new ChannelTopic(CHANNEL));
        log.info("[DISPATCHER] Subscribed to {} for cross-pod sync", CHANNEL);
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

        // Local cache + broadcast so other replicas refresh.
        activeStrategy.set(normalized);
        redis.convertAndSend(CHANNEL, normalized);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth != null ? auth.getName() : "anonymous";
        log.info("[DISPATCHER] Strategy switched {} -> {} by user={} (persisted + broadcast)",
                previous, normalized, actor);
    }

    private void loadFromSource() {
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
        log.info("[DISPATCHER] Strategy loaded from {} : {}",
                dbValue != null ? "DB" : "env", normalized);
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
