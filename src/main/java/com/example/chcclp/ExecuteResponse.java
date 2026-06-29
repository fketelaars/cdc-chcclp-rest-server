package com.example.chcclp;

/**
 * Response body for {@code POST /sessions/{id}/execute}.
 */
public class ExecuteResponse {

    private String result;
    private String executedAt;

    public ExecuteResponse(String result, String executedAt) {
        this.result = result;
        this.executedAt = executedAt;
    }

    public String getResult() { return result; }
    public String getExecutedAt() { return executedAt; }
}
