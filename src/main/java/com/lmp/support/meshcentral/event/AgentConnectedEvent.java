package com.lmp.support.meshcentral.event;

import java.util.UUID;

public record AgentConnectedEvent(UUID sessionId, String meshId, String nodeId) {}
