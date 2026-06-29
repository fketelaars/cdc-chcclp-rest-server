package com.example.chcclp;

/**
 * Response body for {@code POST /sessions}.
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
