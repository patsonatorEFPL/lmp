package com.lmp.support.signaling;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * One frame on the WebRTC signaling channel between tech browser and client runner.
 * Relayed verbatim by {@link SignalingController}; the payload (SDP / ICE candidate)
 * is opaque to the broker.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SignalingMessage(
    UUID sessionId,
    String type,        // "offer" | "answer" | "ice-candidate" | "bye"
    String direction,   // "tech-to-client" | "client-to-tech"
    Object payload
) {}
