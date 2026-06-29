package com.example.chcclp;

import com.ibm.replication.cdc.scripting.EmbeddedScript;
import com.ibm.replication.cdc.scripting.EmbeddedScriptException;
import com.ibm.replication.cdc.scripting.Result;
import com.ibm.replication.cdc.scripting.ResultFormatter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Represents a single client CHCCLP session backed by one {@link EmbeddedScript} instance.
 *
 * <p>Each client obtains its own session so that server-connection state is fully isolated.
 * The {@link #execute(String)} method is {@code synchronized} because {@code EmbeddedScript}
 * is not thread-safe; a single session must only be used by one thread at a time.
 */
public class ChcclpSession {

    private final String id;
    private final EmbeddedScript script;
    private final Instant createdAt;
    private volatile Instant lastUsedAt;

    /**
     * Opens the underlying {@link EmbeddedScript}.
     * The caller is responsible for executing the {@code connect server} command afterwards.
     */
    public ChcclpSession(String id) throws EmbeddedScriptException {
        this.id = id;
        this.script = new EmbeddedScript();
        this.script.open();
        this.createdAt = Instant.now();
        this.lastUsedAt = createdAt;
    }

    /**
     * Executes a single CHCCLP command and returns its result as a formatted string.
     *
     * <p>The {@link Result} returned by {@link EmbeddedScript#getResult()} is rendered
     * via {@link Result#display(PrintStream, ResultFormatter)} into a {@code String}
     * so it can be serialised in the HTTP response.
     *
     * @param command full CHCCLP command string, including the trailing semicolon
     * @return the formatted result string (may be empty for commands that produce no output)
     * @throws EmbeddedScriptException if CHCCLP reports an error
     */
    public synchronized String execute(String command) throws EmbeddedScriptException {
        script.execute(command);
        lastUsedAt = Instant.now();
        return resultToString(script.getResult());
    }

    /**
     * Closes the underlying {@link EmbeddedScript}. After this call the session
     * must not be used again.
     */
    public void close() {
        script.close();
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Renders a {@link Result} into a plain UTF-8 string using the default
     * {@link ResultFormatter}.
     */
    private static String resultToString(Result result) {
        if (result == null) {
            return "";
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8)) {
            result.display(ps, new ResultFormatter());
        }
        return baos.toString(StandardCharsets.UTF_8);
    }
}
