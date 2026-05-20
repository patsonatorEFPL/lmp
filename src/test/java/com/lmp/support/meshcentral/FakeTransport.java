package com.lmp.support.meshcentral;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * In-memory {@link MeshCentralTransport} for tests. Captures sent frames,
 * optionally auto-replies via {@link #setEchoResponder(Function)}, and exposes
 * {@link #pushInbound(String)} so tests can simulate unsolicited frames.
 */
final class FakeTransport implements MeshCentralTransport {

    private final List<String> sent = new ArrayList<>();
    private final AtomicBoolean open = new AtomicBoolean(false);

    private Consumer<String> messageHandler = m -> {};
    private Runnable closeHandler = () -> {};
    private Function<String, String> echoResponder;

    @Override
    public void connect() {
        open.set(true);
    }

    @Override
    public void send(String text) {
        sent.add(text);
        if (echoResponder != null) {
            String reply = echoResponder.apply(text);
            if (reply != null) {
                messageHandler.accept(reply);
            }
        }
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
        return open.get();
    }

    @Override
    public void close() {
        boolean was = open.getAndSet(false);
        if (was && closeHandler != null) {
            closeHandler.run();
        }
    }

    /** Simulate peer close. */
    void simulatePeerClose() {
        open.set(false);
        closeHandler.run();
    }

    void pushInbound(String text) {
        messageHandler.accept(text);
    }

    List<String> sentFrames() { return sent; }

    void setEchoResponder(Function<String, String> responder) { this.echoResponder = responder; }
}
