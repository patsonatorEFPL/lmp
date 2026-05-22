package com.lmp.support.meshcentral;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Long-lived authenticated WebSocket to MeshCentral. Sends commands, demultiplexes
 * responses by {@code responseid}, publishes unsolicited events as Spring
 * {@link RawMeshCentralEvent}. Auto-reconnects with exponential backoff.
 *
 * Transport is injected so unit tests can swap a mock for the real network call.
 */
@Component
public class MeshCentralWebSocketClient implements AutoCloseable {

    private final MeshCentralProperties props;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Function<MeshCentralProperties, MeshCentralTransport> transportFactory;

    private final Map<String, CompletableFuture<MeshCentralResponse>> pending = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reconnectScheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mesh-reconnect");
            t.setDaemon(true);
            return t;
        });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private volatile MeshCentralTransport transport;

    @org.springframework.beans.factory.annotation.Autowired
    public MeshCentralWebSocketClient(MeshCentralProperties props,
                                      ObjectMapper mapper,
                                      ApplicationEventPublisher eventPublisher) {
        this(props, mapper, eventPublisher, defaultFactory());
    }

    /** Test-friendly constructor: inject a custom transport factory. */
    public MeshCentralWebSocketClient(MeshCentralProperties props,
                                      ObjectMapper mapper,
                                      ApplicationEventPublisher eventPublisher,
                                      Function<MeshCentralProperties, MeshCentralTransport> transportFactory) {
        this.props = props;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher == null ? event -> {} : eventPublisher;
        this.transportFactory = transportFactory;
    }

    private static Function<MeshCentralProperties, MeshCentralTransport> defaultFactory() {
        return p -> new JdkHttpClientTransport(
            URI.create(p.wsBase() + "/control.ashx"),
            p.adminUsername(), p.adminPassword(),
            p.acceptSelfSigned(),
            p.commandTimeout()
        );
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (props.adminPassword() == null || props.adminPassword().isEmpty()) {
            return;
        }
        connectAndAuthenticate();
    }

    public synchronized void connectAndAuthenticate() {
        if (shuttingDown.get()) return;
        try {
            MeshCentralTransport t = transportFactory.apply(props);
            t.onMessage(this::onTextMessage);
            t.onClose(this::onClose);
            t.connect();
            this.transport = t;
            this.running.set(true);
        } catch (Exception e) {
            running.set(false);
            scheduleReconnect(props.reconnectInitialBackoff());
            throw new MeshCentralException("Connect failed: " + e.getMessage(), e);
        }
    }

    private void onTextMessage(String message) {
        try {
            MeshCentralResponse resp = mapper.readValue(message, MeshCentralResponse.class);
            if (resp.getResponseid() != null) {
                CompletableFuture<MeshCentralResponse> fut = pending.remove(resp.getResponseid());
                if (fut != null) {
                    fut.complete(resp);
                    return;
                }
            }
            eventPublisher.publishEvent(new RawMeshCentralEvent(resp));
        } catch (Exception ignored) {
            // unparseable frame — drop
        }
    }

    private void onClose() {
        running.set(false);
        if (!shuttingDown.get()) {
            scheduleReconnect(props.reconnectInitialBackoff());
        }
    }

    public CompletableFuture<MeshCentralResponse> sendAndAwait(MeshCentralCommand cmd) {
        CompletableFuture<MeshCentralResponse> future = new CompletableFuture<>();
        pending.put(cmd.getResponseid(), future);
        try {
            String json = mapper.writeValueAsString(merge(cmd));
            if (transport == null || !transport.isOpen()) {
                pending.remove(cmd.getResponseid());
                future.completeExceptionally(new MeshCentralException("Transport not open"));
                return future;
            }
            transport.send(json);
        } catch (Exception e) {
            pending.remove(cmd.getResponseid());
            future.completeExceptionally(e);
        }
        return future.orTimeout(props.commandTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }

    private Map<String, Object> merge(MeshCentralCommand cmd) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("action", cmd.getAction());
        map.put("responseid", cmd.getResponseid());
        map.putAll(cmd.getParams());
        return map;
    }

    private void scheduleReconnect(Duration backoff) {
        if (shuttingDown.get()) return;
        Duration next = backoff.compareTo(props.reconnectMaxBackoff()) >= 0
            ? props.reconnectMaxBackoff()
            : backoff.multipliedBy(2);
        reconnectScheduler.schedule(() -> {
            try {
                connectAndAuthenticate();
            } catch (Exception e) {
                scheduleReconnect(next);
            }
        }, backoff.toMillis(), TimeUnit.MILLISECONDS);
    }

    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void close() {
        shuttingDown.set(true);
        running.set(false);
        if (transport != null) {
            transport.close();
        }
        reconnectScheduler.shutdownNow();
    }

    /** Spring event published when an unsolicited (no {@code responseid}) frame arrives. */
    public record RawMeshCentralEvent(MeshCentralResponse response) {}
}
