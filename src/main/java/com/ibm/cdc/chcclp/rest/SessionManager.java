package com.ibm.cdc.chcclp.rest;

import com.ibm.replication.cdc.scripting.EmbeddedScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of all active {@link ChcclpSession} instances.
 *
 * <p>Sessions are stored in a {@link ConcurrentHashMap} keyed by their UUID.
 * A background scheduler evicts sessions that have been idle longer than
 * {@code chcclp.session.timeout-minutes} (default 30 minutes).
 */
@Service
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private final ConcurrentHashMap<String, ChcclpSession> sessions = new ConcurrentHashMap<>();

    @Value("${chcclp.session.timeout-minutes:30}")
    private long sessionTimeoutMinutes;

    /**
     * Creates a new CHCCLP session, opens the scripting engine, and connects to the
     * access server using the supplied credentials.
     *
     * @param req connection details
     * @return the newly created session
     * @throws EmbeddedScriptException if the scripting engine fails to open or the connect command fails
     */
    public ChcclpSession createSession(ConnectRequest req) throws EmbeddedScriptException {
        String id = UUID.randomUUID().toString();
        ChcclpSession session = new ChcclpSession(id);
        String connectCmd = String.format(
                "connect server hostname %s port %d username %s password %s;",
                req.getAccessServerHost(), req.getAccessServerPort(),
                req.getAccessServerUser(), req.getAccessServerPassword());
        session.execute(connectCmd);
        sessions.put(id, session);
        log.info("Session {} created, connected to {}:{}", id, req.getAccessServerHost(), req.getAccessServerPort());
        return session;
    }

    /**
     * Returns the session with the given id, or {@code null} if it does not exist.
     */
    public ChcclpSession getSession(String id) {
        return sessions.get(id);
    }

    /**
     * Disconnects from the access server, closes the scripting engine, and removes the session.
     * Safe to call on a session that has already been removed.
     *
     * @param id session id
     */
    public void closeSession(String id) {
        ChcclpSession session = sessions.remove(id);
        if (session != null) {
            try {
                session.execute("disconnect server;");
            } catch (EmbeddedScriptException e) {
                log.warn("Session {}: disconnect server failed ({}), proceeding with close",
                        id, e.getResultCodeAndMessage());
            } finally {
                session.close();
                log.info("Session {} closed", id);
            }
        }
    }

    /**
     * Returns a snapshot of all active session ids and their last-used timestamps.
     */
    public List<Map<String, Object>> listSessions() {
        return sessions.values().stream()
                .map(s -> Map.of(
                        "sessionId", (Object) s.getId(),
                        "createdAt", s.getCreatedAt().toString(),
                        "lastUsedAt", s.getLastUsedAt().toString()))
                .toList();
    }

    /**
     * Runs every minute and evicts sessions that have been idle longer than
     * {@code chcclp.session.timeout-minutes}.
     */
    @Scheduled(fixedDelay = 60_000)
    public void evictIdleSessions() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(sessionTimeoutMinutes));
        sessions.keySet().stream()
                .filter(id -> sessions.get(id).getLastUsedAt().isBefore(cutoff))
                .forEach(id -> {
                    log.info("Session {} idle timeout — evicting", id);
                    closeSession(id);
                });
    }
}
