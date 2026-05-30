package org.tpkprav.api;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RequestContext {

    private String requestId;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
