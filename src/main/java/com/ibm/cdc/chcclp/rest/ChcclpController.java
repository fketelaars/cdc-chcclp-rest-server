package com.ibm.cdc.chcclp.rest;

import com.ibm.replication.cdc.scripting.EmbeddedScriptException;
import jakarta.servlet.http.HttpServletRequest;
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
 * POST   /connect                   – open a new session, connect to an access server,
 *                                     and return a Bearer token
 * GET    /sessions                  – list all active sessions  [requires token]
 * GET    /sessions/{id}             – get metadata for a specific session  [requires token]
 * POST   /sessions/{id}/execute     – execute a CHCCLP command in the session  [requires token]
 * DELETE /sessions/{id}             – disconnect and close the session  [requires token]
 * </pre>
 *
 * <p>Every endpoint except {@code POST /connect} requires an
 * {@code Authorization: Bearer <token>} header.  The {@link AuthFilter} validates
 * the token and stores the session ID as the request attribute
 * {@link AuthFilter#SESSION_ID_ATTR}.
 */
@RestController
public class ChcclpController {

    private final SessionManager sessionManager;
    private final ConnectDefaults connectDefaults;
    private final TokenService tokenService;

    public ChcclpController(SessionManager sessionManager,
                            ConnectDefaults connectDefaults,
                            TokenService tokenService) {
        this.sessionManager = sessionManager;
        this.connectDefaults = connectDefaults;
        this.tokenService = tokenService;
    }

    // -------------------------------------------------------------------------
    // POST /connect — create session, connect to access server, return token
    // -------------------------------------------------------------------------

    @PostMapping("/connect")
    public ResponseEntity<?> connect(@RequestBody ConnectRequest req) {
        // accessServerHost and accessServerPort are optional — fall back to .env defaults
        if (req.getAccessServerHost() == null || req.getAccessServerHost().isBlank()) {
            req.setAccessServerHost(connectDefaults.getHostname());
        }
        if (req.getAccessServerPort() <= 0) {
            req.setAccessServerPort(connectDefaults.getPort());
        }

        // Validate after defaults have been applied
        if (req.getAccessServerHost() == null || req.getAccessServerHost().isBlank()) {
            return badRequest("accessServerHost is required");
        }
        if (req.getAccessServerPort() <= 0) {
            return badRequest("accessServerPort must be a positive integer");
        }
        if (req.getAccessServerUser() == null || req.getAccessServerUser().isBlank()) {
            return badRequest("accessServerUser is required");
        }
        if (req.getAccessServerPassword() == null || req.getAccessServerPassword().isBlank()) {
            return badRequest("accessServerPassword is required");
        }

        try {
            ChcclpSession session = sessionManager.createSession(req);
            String token = tokenService.createToken(session.getId());
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new ConnectResponse(token, session.getCreatedAt().toString()));
        } catch (EmbeddedScriptException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(new ErrorResponse("connect_failed", e.getResultCodeAndMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /sessions — list all active sessions
    // -------------------------------------------------------------------------

    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> listSessions() {
        return ResponseEntity.ok(sessionManager.listSessions());
    }

    // -------------------------------------------------------------------------
    // GET /sessions/{id} — get session metadata
    // -------------------------------------------------------------------------

    @GetMapping("/sessions/{id}")
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
    // POST /execute — run a CHCCLP command (session resolved from token)
    // -------------------------------------------------------------------------

    @PostMapping("/execute")
    public ResponseEntity<?> execute(@RequestBody ExecuteRequest req,
                                     HttpServletRequest httpRequest) {
        if (req.getCommand() == null || req.getCommand().isBlank()) {
            return badRequest("command is required");
        }

        String id = (String) httpRequest.getAttribute(AuthFilter.SESSION_ID_ATTR);
        ChcclpSession session = sessionManager.getSession(id);
        if (session == null) {
            return notFound(id);
        }

        try {
            String command = req.getCommand().stripTrailing();
            if (!command.endsWith(";")) {
                command = command + ";";
            }
            Object result = session.execute(command);
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

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<?> closeSession(@PathVariable String id,
                                          HttpServletRequest httpRequest) {
        // Verify the token's session matches the path parameter
        String tokenSessionId = (String) httpRequest.getAttribute(AuthFilter.SESSION_ID_ATTR);
        if (!id.equals(tokenSessionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("forbidden", "Token does not match the requested session"));
        }

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
