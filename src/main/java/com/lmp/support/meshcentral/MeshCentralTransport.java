package com.lmp.support.meshcentral;

import java.util.function.Consumer;

/**
 * Thin abstraction over a single bidirectional text WebSocket connection.
 * Production impl: {@link JdkHttpClientTransport} using {@code java.net.http.HttpClient.WebSocket}.
 * Tests inject a fake transport, so {@link MeshCentralWebSocketClient} can be exercised
 * without spinning up a real WebSocket server.
 */
public interface MeshCentralTransport extends AutoCloseable {

    /** Open the connection. Blocks until handshake completes or throws. */
    void connect() throws Exception;

    /** Send a UTF-8 text frame. */
    void send(String text) throws Exception;

    /** Register a handler invoked once per inbound text frame. */
    void onMessage(Consumer<String> handler);

    /** Register a handler invoked when the peer closes the connection. */
    void onClose(Runnable handler);

    boolean isOpen();

    @Override
    void close();
}
