package com.lmp.support.meshcentral;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MeshCentralCommand {

    private final String action;
    private final String responseid;
    private final Map<String, Object> params;

    public MeshCentralCommand(String action, String responseid) {
        this.action = action;
        this.responseid = responseid;
        this.params = new HashMap<>();
    }

    public MeshCentralCommand with(String key, Object value) {
        this.params.put(key, value);
        return this;
    }

    public String getAction() { return action; }
    public String getResponseid() { return responseid; }
    public Map<String, Object> getParams() { return params; }
}
