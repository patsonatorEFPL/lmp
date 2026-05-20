package com.lmp.support.meshcentral;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeshCentralServiceTest {

    private MeshCentralWebSocketClient ws;
    private MeshCentralService service;

    @BeforeEach
    void setup() {
        ws = mock(MeshCentralWebSocketClient.class);
        service = new MeshCentralService(ws);
    }

    @Test
    void createDeviceGroupReturnsMeshId() {
        MeshCentralResponse resp = new MeshCentralResponse();
        resp.setAction("createmesh");
        resp.setResult("ok");
        resp.setExtra("meshid", "mesh//abc123");
        when(ws.sendAndAwait(any(MeshCentralCommand.class)))
            .thenReturn(CompletableFuture.completedFuture(resp));

        String meshId = service.createDeviceGroup(UUID.randomUUID());

        assertThat(meshId).isEqualTo("mesh//abc123");
    }

    @Test
    void createDeviceGroupThrowsOnNonOkResult() {
        MeshCentralResponse resp = new MeshCentralResponse();
        resp.setResult("not allowed");
        when(ws.sendAndAwait(any(MeshCentralCommand.class)))
            .thenReturn(CompletableFuture.completedFuture(resp));

        assertThatThrownBy(() -> service.createDeviceGroup(UUID.randomUUID()))
            .isInstanceOf(MeshCentralException.class)
            .hasMessageContaining("not allowed");
    }

    @Test
    void createDeviceGroupThrowsWhenMeshIdMissing() {
        MeshCentralResponse resp = new MeshCentralResponse();
        resp.setResult("ok");  // success but missing field
        when(ws.sendAndAwait(any(MeshCentralCommand.class)))
            .thenReturn(CompletableFuture.completedFuture(resp));

        assertThatThrownBy(() -> service.createDeviceGroup(UUID.randomUUID()))
            .isInstanceOf(MeshCentralException.class)
            .hasMessageContaining("no meshid");
    }

    @Test
    void generateAgentInviteReturnsUrl() {
        MeshCentralResponse resp = new MeshCentralResponse();
        resp.setResult("ok");
        resp.setExtra("url", "https://mesh.lmp-services.ca/agentinvite?c=abc123");
        when(ws.sendAndAwait(any(MeshCentralCommand.class)))
            .thenReturn(CompletableFuture.completedFuture(resp));

        String url = service.generateAgentInvite("mesh//abc123", Duration.ofMinutes(15));

        assertThat(url).contains("agentinvite");
    }

    @Test
    void generateAgentInviteThrowsOnError() {
        MeshCentralResponse resp = new MeshCentralResponse();
        resp.setResult("unauthorized");
        when(ws.sendAndAwait(any(MeshCentralCommand.class)))
            .thenReturn(CompletableFuture.completedFuture(resp));

        assertThatThrownBy(() -> service.generateAgentInvite("mesh//x", Duration.ofMinutes(5)))
            .isInstanceOf(MeshCentralException.class)
            .hasMessageContaining("unauthorized");
    }

    @Test
    void removeDeviceGroupSucceedsOnOk() {
        MeshCentralResponse resp = new MeshCentralResponse();
        resp.setResult("ok");
        when(ws.sendAndAwait(any(MeshCentralCommand.class)))
            .thenReturn(CompletableFuture.completedFuture(resp));

        service.removeDeviceGroup("mesh//abc123");  // no throw = pass
    }

    @Test
    void removeDeviceGroupThrowsOnFailure() {
        MeshCentralResponse resp = new MeshCentralResponse();
        resp.setResult("not found");
        when(ws.sendAndAwait(any(MeshCentralCommand.class)))
            .thenReturn(CompletableFuture.completedFuture(resp));

        assertThatThrownBy(() -> service.removeDeviceGroup("mesh//ghost"))
            .isInstanceOf(MeshCentralException.class)
            .hasMessageContaining("not found");
    }
}
