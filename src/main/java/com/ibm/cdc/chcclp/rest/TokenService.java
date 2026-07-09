package com.ibm.cdc.chcclp.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Generates and validates Bearer tokens used to authenticate API requests.
 *
 * <p>Token format (before Base64 encoding):
 * <pre>
 *   {@code <sessionId>:<secret>:<hmac>}
 * </pre>
 * where {@code hmac} is the hex-encoded HMAC-SHA256 of
 * {@code "<sessionId>:<secret>"} keyed with {@code auth.token.signing-key}.
 *
 * <p>Clients receive the token from {@code POST /connect} and must supply it
 * in every subsequent request as:
 * <pre>
 *   Authorization: Bearer <base64-encoded-token>
 * </pre>
 */
@Service
public class TokenService {

    private static final String HMAC_ALGO = "HmacSHA256";

    @Value("${auth.token.signing-key}")
    private String signingKey;

    /**
     * Creates a new token embedding the given {@code sessionId} together with a
     * freshly generated secret.
     *
     * @param sessionId the session UUID
     * @return opaque Bearer token string
     */
    public String createToken(String sessionId) {
        String secret = UUID.randomUUID().toString().replace("-", "");
        String payload = sessionId + ":" + secret;
        String hmac = hmac(payload);
        String raw = payload + ":" + hmac;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates a Bearer token and, if valid, returns the embedded session ID.
     *
     * @param token the raw token string from the {@code Authorization} header
     *              (without the {@code "Bearer "} prefix)
     * @return the session ID embedded in the token, or {@code null} if the token
     *         is malformed or the HMAC does not match
     */
    public String validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String raw;
        try {
            raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }

        // Format: sessionId:secret:hmac  — split on last two colons
        int lastColon = raw.lastIndexOf(':');
        if (lastColon < 0) return null;
        String suppliedHmac = raw.substring(lastColon + 1);
        String payload = raw.substring(0, lastColon);

        // payload must still contain exactly one more colon (sessionId:secret)
        if (payload.indexOf(':') < 0) return null;

        String expectedHmac = hmac(payload);
        if (!constantTimeEquals(expectedHmac, suppliedHmac)) {
            return null;
        }

        // payload = sessionId:secret — return the sessionId portion
        return payload.substring(0, payload.indexOf(':'));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 not available", e);
        }
    }

    /** Constant-time string comparison to prevent timing attacks. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
