package com.example.chcclp;

import com.ibm.replication.cdc.scripting.EmbeddedScriptException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the CHCCLP session and command execution API.
 *
 * <pre>
 * POST   /sessions                  – open a new session and connect to an access server
 * GET    /sessions                  – list all active sessions
 * GET    /sessions/{id}             – get metadata for a specific session
 * POST   /sessions/{id}/execute     – execute a CHCCLP command in the session
 * DELETE /sessions/{id}             – disconnect and close the session
 * </pre>
 */
@RestController
@RequestMapping("/sessions")
public class ChcclpController {

    private final SessionManager sessionManager;
    private final ConnectDefaults connectDefaults;

    public ChcclpController(SessionManager sessionManager, ConnectDefaults connectDefaults) {
        this.sessionManager = sessionManager;
        this.connectDefaults = connectDefaults;
    }

    // -------------------------------------------------------------------------
    // POST /sessions — create session and connect to access server
    // -------------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<?> createSession(@RequestBody ConnectRequest req) {
        // Apply .env defaults for any field the caller left blank / zero
        if (req.getHostname() == null || req.getHostname().isBlank()) {
            req.setHostname(connectDefaults.getHostname());
        }
        if (req.getPort() <= 0) {
            req.setPort(connectDefaults.getPort());
        }
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            req.setUsername(connectDefaults.getUsername());
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            req.setPassword(connectDefaults.getPassword());
        }

        // Validate after defaults have been applied
        if (req.getHostname() == null || req.getHostname().isBlank()) {
            return badRequest("hostname is required");
        }
        if (req.getPort() <= 0) {
            return badRequest("port must be a positive integer");
        }
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            return badRequest("username is required");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            return badRequest("password is required");
        }

        try {
            ChcclpSession session = sessionManager.createSession(req);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new SessionResponse(session.getId(), session.getCreatedAt().toString()));
        } catch (EmbeddedScriptException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(new ErrorResponse("connect_failed", e.getResultCodeAndMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /sessions — list all active sessions
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listSessions() {
        return ResponseEntity.ok(sessionManager.listSessions());
    }

    // -------------------------------------------------------------------------
    // GET /sessions/{id} — get session metadata
    // -------------------------------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<?> getSession(@PathVariable String id) {
        ChcclpSession session = sessionManager.getSession(id);
        if (session == null) {
            return notFound(id);
        }
        return ResponseEntity.ok(Map.of(
                "sessionId", session.getId(),
                "createdAt", session.getCreatedAt().toString(),
                "lastUsedAt", session.getLastUsedAt().toString()));
    }

    // -------------------------------------------------------------------------
    // POST /sessions/{id}/execute — run a CHCCLP command
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/execute")
    public ResponseEntity<?> execute(@PathVariable String id, @RequestBody ExecuteRequest req) {
        if (req.getCommand() == null || req.getCommand().isBlank()) {
            return badRequest("command is required");
        }

        ChcclpSession session = sessionManager.getSession(id);
        if (session == null) {
            return notFound(id);
        }

        try {
            String result = session.execute(req.getCommand());
            return ResponseEntity.ok(new ExecuteResponse(result, Instant.now().toString()));
        } catch (EmbeddedScriptException e) {
            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponse("command_failed", e.getResultCodeAndMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /sessions/{id} — disconnect and close the session
    // -------------------------------------------------------------------------

    @DeleteMapping("/{id}")
    public ResponseEntity<?> closeSession(@PathVariable String id) {
        if (sessionManager.getSession(id) == null) {
            return notFound(id);
        }
        sessionManager.closeSession(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ResponseEntity<ErrorResponse> badRequest(String detail) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("bad_request", detail));
    }

    private ResponseEntity<ErrorResponse> notFound(String id) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("session_not_found", "No session with id: " + id));
    }
}
