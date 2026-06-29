package com.example.chcclp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Default connect-server credentials loaded from {@code .env} / environment variables.
 *
 * <pre>
 * CONNECT_HOSTNAME=my-access-server
 * CONNECT_PORT=11001
 * CONNECT_USERNAME=admin
 * CONNECT_PASSWORD=secret
 * </pre>
 *
 * Any field that is non-blank here is used as a fallback when the caller omits it
 * from the {@code POST /sessions} request body.
 */
@Component
@ConfigurationProperties(prefix = "connect")
public class ConnectDefaults {

    private String hostname = "";
    private int port = 0;
    private String username = "";
    private String password = "";

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
