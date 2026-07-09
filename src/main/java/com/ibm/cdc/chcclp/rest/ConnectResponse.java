package com.ibm.cdc.chcclp.rest;

/**
 * Response body for {@code POST /connect}.
 */
public class ConnectResponse {

    private String token;
    private String createdAt;

    public ConnectResponse(String token, String createdAt) {
        this.token = token;
        this.createdAt = createdAt;
    }

    public String getToken() { return token; }
    public String getCreatedAt() { return createdAt; }
}
