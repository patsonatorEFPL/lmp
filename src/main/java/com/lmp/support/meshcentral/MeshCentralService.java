package com.lmp.support.meshcentral;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * High-level facade over {@link MeshCentralWebSocketClient}. Hides the
 * JSON-RPC-ish wire protocol behind typed methods: createDeviceGroup,
 * generateAgentInvite, removeDeviceGroup.
 */
@Service
public class MeshCentralService {

    private final MeshCentralWebSocketClient ws;

    public MeshCentralService(MeshCentralWebSocketClient ws) {
        this.ws = ws;
    }

    public String createDeviceGroup(UUID sessionId) {
        String responseid = "create-" + UUID.randomUUID();
        MeshCentralCommand cmd = new MeshCentralCommand("createmesh", responseid)
            .with("meshname", "lmp-session-" + sessionId)
            .with("meshtype", 2)
            .with("desc", "LMP HelpDesk session " + sessionId);

        MeshCentralResponse resp = await(cmd);
        if (!resp.isSuccess()) {
            throw new MeshCentralException("createmesh failed: " + resp.getResult());
        }
        Object meshId = resp.getExtra().get("meshid");
        if (meshId == null) {
            throw new MeshCentralException("createmesh succeeded but no meshid returned");
        }
        return meshId.toString();
    }

    public String generateAgentInvite(String meshId, Duration ttl) {
        String responseid = "invite-" + UUID.randomUUID();
        MeshCentralCommand cmd = new MeshCentralCommand("createInviteLink", responseid)
            .with("meshid", meshId)
            .with("hours", Math.max(1, (int) ttl.toHours()))
            .with("flags", 8);

        MeshCentralResponse resp = await(cmd);
        if (!resp.isSuccess()) {
            throw new MeshCentralException("createInviteLink failed: " + resp.getResult());
        }
        Object url = resp.getExtra().get("url");
        if (url == null) {
            throw new MeshCentralException("createInviteLink succeeded but no url returned");
        }
        return url.toString();
    }

    public void removeDeviceGroup(String meshId) {
        String responseid = "remove-" + UUID.randomUUID();
        MeshCentralCommand cmd = new MeshCentralCommand("deletemesh", responseid)
            .with("meshid", meshId);

        MeshCentralResponse resp = await(cmd);
        if (!resp.isSuccess()) {
            throw new MeshCentralException("deletemesh failed: " + resp.getResult());
        }
    }

    private MeshCentralResponse await(MeshCentralCommand cmd) {
        try {
            return ws.sendAndAwait(cmd).get(15, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new MeshCentralException("interrupted waiting for response", ie);
        } catch (ExecutionException ee) {
            throw new MeshCentralException("execution failed: " + ee.getCause().getMessage(), ee.getCause());
        } catch (TimeoutException te) {
            throw new MeshCentralException("timeout awaiting response", te);
        }
    }
}
