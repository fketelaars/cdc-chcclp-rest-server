package com.ibm.cdc.chcclp.rest;

/**
 * Generic error response body returned on 4xx / 5xx responses.
 */
public class ErrorResponse {

    private String error;
    private String detail;

    public ErrorResponse(String error, String detail) {
        this.error = error;
        this.detail = detail;
    }

    public String getError() { return error; }
    public String getDetail() { return detail; }
}
