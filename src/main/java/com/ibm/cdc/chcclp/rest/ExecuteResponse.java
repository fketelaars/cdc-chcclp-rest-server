package com.ibm.cdc.chcclp.rest;

/**
 * Response body for {@code POST /execute}.
 *
 * <p>{@code result} is a JSON-native value: a list of objects for TABLE results,
 * a list of strings for LIST results, a map for KEY_VALUES results, a plain
 * string for VALUE results, or {@code null} when the command produces no output.
 */
public class ExecuteResponse {

    private Object result;
    private String executedAt;

    public ExecuteResponse(Object result, String executedAt) {
        this.result = result;
        this.executedAt = executedAt;
    }

    public Object getResult() { return result; }
    public String getExecutedAt() { return executedAt; }
}
