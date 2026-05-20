package com.lmp.support.meshcentral;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * {@link MeshCentralTransport} backed by JDK 11+ {@link HttpClient.WebSocket}.
 * No third-party WS dependency required.
 */
public class JdkHttpClientTransport implements MeshCentralTransport {

    private final URI uri;
    private final String basicAuthHeader;
    private final boolean acceptSelfSigned;
    private final Duration handshakeTimeout;

    private final StringBuilder partial = new StringBuilder();
    private volatile Consumer<String> messageHandler = msg -> {};
    private volatile Runnable closeHandler = () -> {};
    private volatile WebSocket socket;
    private volatile boolean open;

    public JdkHttpClientTransport(URI uri, String username, String password,
                                  boolean acceptSelfSigned, Duration handshakeTimeout) {
        this.uri = uri;
        if (username != null && !username.isEmpty()) {
            String creds = username + ":" + (password == null ? "" : password);
            this.basicAuthHeader = "Basic " + java.util.Base64.getEncoder()
                .encodeToString(creds.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } else {
            this.basicAuthHeader = null;
        }
        this.acceptSelfSigned = acceptSelfSigned;
        this.handshakeTimeout = handshakeTimeout == null ? Duration.ofSeconds(15) : handshakeTimeout;
    }

    @Override
    public void connect() throws Exception {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        if (acceptSelfSigned) {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{trustAll()}, new SecureRandom());
            clientBuilder.sslContext(ctx);
        }
        HttpClient client = clientBuilder.build();

        WebSocket.Builder b = client.newWebSocketBuilder()
            .connectTimeout(handshakeTimeout);
        if (basicAuthHeader != null) {
            b.header("Authorization", basicAuthHeader);
        }

        CompletableFuture<WebSocket> future = b.buildAsync(uri, new Listener());
        socket = future.get(handshakeTimeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        open = true;
    }

    @Override
    public void send(String text) throws Exception {
        if (socket == null) throw new IllegalStateException("Transport not connected");
        socket.sendText(text, true).get(5, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public void onMessage(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    @Override
    public void onClose(Runnable handler) {
        this.closeHandler = handler;
    }

    @Override
    public boolean isOpen() {
        return open && socket != null && !socket.isInputClosed() && !socket.isOutputClosed();
    }

    @Override
    public void close() {
        open = false;
        if (socket != null) {
            try {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "client close").orTimeout(
                    2, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {
                socket.abort();
            }
        }
    }

    private static TrustManager trustAll() {
        return new X509TrustManager() {
            @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        };
    }

    private final class Listener implements WebSocket.Listener {
        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                String msg = partial.toString();
                partial.setLength(0);
                try {
                    messageHandler.accept(msg);
                } catch (RuntimeException ignored) {
                    // never break the read loop on handler failures
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            open = false;
            try {
                closeHandler.run();
            } catch (RuntimeException ignored) {}
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            open = false;
            try {
                closeHandler.run();
            } catch (RuntimeException ignored) {}
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            return null;
        }
    }
}
