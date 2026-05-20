package com.lmp.support.meshcentral;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class MeshCentralResponse {

    private String action;
    private String responseid;
    private String result;
    private final Map<String, Object> extra = new HashMap<>();

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResponseid() { return responseid; }
    public void setResponseid(String responseid) { this.responseid = responseid; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    @JsonAnyGetter
    public Map<String, Object> getExtra() { return extra; }

    @JsonAnySetter
    public void setExtra(String key, Object value) { extra.put(key, value); }

    /** MeshCentral signals success via {@code result == null} or {@code "ok"}. */
    public boolean isSuccess() {
        return result == null || "ok".equalsIgnoreCase(result);
    }
}
