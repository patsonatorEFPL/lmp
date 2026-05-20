package com.lmp.support.meshcentral;

public class MeshCentralException extends RuntimeException {

    public MeshCentralException(String message) {
        super(message);
    }

    public MeshCentralException(String message, Throwable cause) {
        super(message, cause);
    }
}
