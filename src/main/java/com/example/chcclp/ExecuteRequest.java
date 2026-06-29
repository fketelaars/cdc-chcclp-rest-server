package com.example.chcclp;

/**
 * Request body for {@code POST /sessions/{id}/execute}.
 */
public class ExecuteRequest {

    private String command;

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
}
