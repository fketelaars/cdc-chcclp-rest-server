package com.ibm.cdc.chcclp.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that enforces Bearer-token authentication on every request
 * except {@code POST /connect}.
 *
 * <p>When the token is valid the extracted session ID is stored as the request
 * attribute {@value #SESSION_ID_ATTR} so that downstream handlers can retrieve
 * it without re-parsing the token.
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    /** Request attribute key under which the validated session ID is stored. */
    public static final String SESSION_ID_ATTR = "chcclp.sessionId";

    private final TokenService tokenService;

    public AuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // POST /connect is public — all other paths require a valid token
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/connect".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).strip();
        }

        String sessionId = tokenService.validateToken(token);
        if (sessionId == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"unauthorized\",\"detail\":\"Missing or invalid Authorization token\"}");
            return;
        }

        request.setAttribute(SESSION_ID_ATTR, sessionId);
        filterChain.doFilter(request, response);
    }
}
