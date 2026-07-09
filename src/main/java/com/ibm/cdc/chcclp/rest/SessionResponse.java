package com.ibm.cdc.chcclp.rest;

/**
 * Response body for {@code GET /sessions} and {@code GET /sessions/{id}}.
 */
public class SessionResponse {

    private String sessionId;
    private String createdAt;

    public SessionResponse(String sessionId, String createdAt) {
        this.sessionId = sessionId;
        this.createdAt = createdAt;
    }

    public String getSessionId() { return sessionId; }
    public String getCreatedAt() { return createdAt; }
}
