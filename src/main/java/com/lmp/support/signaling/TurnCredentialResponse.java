package com.lmp.support.signaling;

import java.util.List;

/**
 * Time-limited TURN credentials following the coturn {@code --use-auth-secret} pattern
 * (REST API for TURN, RFC draft "TURN-Servers-Easy"). Tech and runner pass these to
 * their {@code RTCPeerConnection} ICE servers list.
 */
public record TurnCredentialResponse(
    String username,
    String credential,
    long expiryEpochSeconds,
    List<String> uris
) {}
