package com.ibm.cdc.chcclp.rest;

/**
 * Request body for {@code POST /sessions}.
 *
 * <ul>
 *   <li>{@code accessServerHost} – hostname or IP of the Access Server (optional; falls back to
 *       the {@code CONNECT_HOSTNAME} environment variable)</li>
 *   <li>{@code accessServerPort} – TCP port of the Access Server (optional; falls back to
 *       the {@code CONNECT_PORT} environment variable)</li>
 *   <li>{@code accessServerUser} – Access Server username (<strong>mandatory</strong>)</li>
 *   <li>{@code accessServerPassword} – Access Server password (<strong>mandatory</strong>)</li>
 * </ul>
 */
public class ConnectRequest {

    private String accessServerHost;
    private int accessServerPort;
    private String accessServerUser;
    private String accessServerPassword;

    public String getAccessServerHost() { return accessServerHost; }
    public void setAccessServerHost(String accessServerHost) { this.accessServerHost = accessServerHost; }

    public int getAccessServerPort() { return accessServerPort; }
    public void setAccessServerPort(int accessServerPort) { this.accessServerPort = accessServerPort; }

    public String getAccessServerUser() { return accessServerUser; }
    public void setAccessServerUser(String accessServerUser) { this.accessServerUser = accessServerUser; }

    public String getAccessServerPassword() { return accessServerPassword; }
    public void setAccessServerPassword(String accessServerPassword) { this.accessServerPassword = accessServerPassword; }
}
