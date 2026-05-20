package com.lmp.support.meshcentral.event;

import java.util.UUID;

public record AgentDisconnectedEvent(UUID sessionId, String meshId, String nodeId) {}
