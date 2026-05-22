package com.lmp.support.meshcentral;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeshCentralWebSocketClientTest {

    private MeshCentralProperties props;
    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        props = new MeshCentralProperties(
            URI.create("http://localhost"),
            URI.create("ws://localhost"),
            "admin", "secret",
            Duration.ofSeconds(2),
            Duration.ofMillis(50),
            Duration.ofMillis(500),
            10,
            false
        );
        mapper = new ObjectMapper();
    }

    @Test
    void sendAndAwaitCorrelatesResponseByResponseId() throws Exception {
        FakeTransport fake = new FakeTransport();
        fake.setEchoResponder(req -> {
            try {
                JsonNode node = mapper.readTree(req);
                String responseid = node.get("responseid").asText();
                return String.format(
                    "{\"action\":\"createmesh\",\"responseid\":\"%s\",\"result\":\"ok\",\"meshid\":\"mesh//abc\"}",
                    responseid);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        MeshCentralWebSocketClient client = new MeshCentralWebSocketClient(
            props, mapper, null, p -> fake);
        client.connectAndAuthenticate();

        MeshCentralCommand cmd = new MeshCentralCommand("createmesh", "req-123")
            .with("meshname", "test-mesh");
        CompletableFuture<MeshCentralResponse> future = client.sendAndAwait(cmd);
        MeshCentralResponse resp = future.get(1, TimeUnit.SECONDS);

        assertThat(resp.getResponseid()).isEqualTo("req-123");
        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getExtra()).containsEntry("meshid", "mesh//abc");

        // Verify the outbound frame carried action + responseid + custom params
        assertThat(fake.sentFrames()).hasSize(1);
        JsonNode sent = mapper.readTree(fake.sentFrames().get(0));
        assertThat(sent.get("action").asText()).isEqualTo("createmesh");
        assertThat(sent.get("responseid").asText()).isEqualTo("req-123");
        assertThat(sent.get("meshname").asText()).isEqualTo("test-mesh");

        client.close();
    }

    @Test
    void unsolicitedFramePublishedAsSpringEvent() {
        AtomicReference<MeshCentralResponse> captured = new AtomicReference<>();
        FakeTransport fake = new FakeTransport();
        MeshCentralWebSocketClient client = new MeshCentralWebSocketClient(
            props, mapper,
            event -> {
                if (event instanceof MeshCentralWebSocketClient.RawMeshCentralEvent raw) {
                    captured.set(raw.response());
                }
            },
            p -> fake
        );
        client.connectAndAuthenticate();

        fake.pushInbound("{\"action\":\"nodeconnect\",\"meshid\":\"mesh//xyz\",\"nodeid\":\"node//1\"}");

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getAction()).isEqualTo("nodeconnect");
        assertThat(captured.get().getExtra()).containsEntry("meshid", "mesh//xyz");

        client.close();
    }

    @Test
    void sendOnClosedTransportFailsFuture() {
        FakeTransport fake = new FakeTransport();
        MeshCentralWebSocketClient client = new MeshCentralWebSocketClient(
            props, mapper, null, p -> fake);
        // intentionally do NOT connect

        MeshCentralCommand cmd = new MeshCentralCommand("createmesh", "x");
        CompletableFuture<MeshCentralResponse> future = client.sendAndAwait(cmd);

        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(MeshCentralException.class);

        client.close();
    }

    @Test
    void timeoutWhenNoResponseArrives() {
        FakeTransport fake = new FakeTransport();
        // no responder -> server never replies
        MeshCentralWebSocketClient client = new MeshCentralWebSocketClient(
            props, mapper, null, p -> fake);
        client.connectAndAuthenticate();

        MeshCentralCommand cmd = new MeshCentralCommand("createmesh", "no-reply");
        CompletableFuture<MeshCentralResponse> future = client.sendAndAwait(cmd);

        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(TimeoutException.class);

        client.close();
    }

    @Test
    void peerClosesTriggersReconnectAttempt() throws Exception {
        FakeTransport first = new FakeTransport();
        FakeTransport second = new FakeTransport();
        FakeTransport[] sequence = {first, second};
        int[] idx = {0};
        MeshCentralWebSocketClient client = new MeshCentralWebSocketClient(
            props, mapper, null, p -> sequence[Math.min(idx[0]++, sequence.length - 1)]);
        client.connectAndAuthenticate();
        assertThat(client.isRunning()).isTrue();

        first.simulatePeerClose();
        assertThat(client.isRunning()).isFalse();

        // Wait for reconnect (initial backoff 50ms in test props)
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline && !client.isRunning()) {
            Thread.sleep(20);
        }
        assertThat(client.isRunning()).isTrue();

        client.close();
    }
}
