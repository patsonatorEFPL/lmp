package com.lmp.support.web.dto;

import java.util.List;

/**
 * Client/runner-side consent submission. {@code itemsConsented} lists the
 * granular permissions the user accepted (e.g. "screen", "voice",
 * "system_audio", "commands", "recording"). {@code consentTextHash} is the
 * SHA-256 of the legal copy the user actually saw — lets the audit log
 * reconstruct which consent version was accepted.
 */
public record ConsentRequest(
    boolean accepted,
    List<String> itemsConsented,
    String consentTextHash
) {}
