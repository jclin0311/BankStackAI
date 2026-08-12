package com.authuser.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for obtaining and caching an Auth0 Management API token.
 *
 * <p>This service uses the OAuth2 <b>Client Credentials</b> grant to authenticate as a machine-to-machine (M2M) client
 * and request a Management API access token from Auth0. It is a backend-only service and is <b>not</b> exposed
 * as a public endpoint.</p>
 *
 * <p><b>Design intent:</b></p>
 * <ul>
 *   <li>This class is used internally by {@link Auth0UserService} to call Auth0's Management API (e.g., to create users or assign roles).</li>
 *   <li>Only this service holds the client credentials for the Management API — no other microservice should know them.</li>
 *   <li>The access token is cached in memory to avoid repeated calls to Auth0 on every request.</li>
 * </ul>
 */
@Service
public class ManagementTokenService {

    /**
     * Auth0 domain (tenant base URL), e.g. {@code https://dev-wgk04dj5v68sbhre.us.auth0.com}
     */
    @Value("${auth0.domain}")
    private String domain;

    /**
     * Client ID of the Machine-to-Machine application authorized to call the Management API.
     */
    @Value("${auth0.mgmt.client-id}")
    private String clientId;

    /**
     * Client Secret of the Machine-to-Machine application.
     */
    @Value("${auth0.mgmt.client-secret}")
    private String clientSecret;

    /**
     * Audience for the Auth0 Management API, usually {@code https://YOUR_TENANT_DOMAIN/api/v2/}
     */
    @Value("${auth0.mgmt.audience}")
    private String mgmtAudience;

    private final RestTemplate rt = new RestTemplate();

    /**
     * Cached access token to avoid fetching a new one for every request.
     */
    private volatile String cachedToken;

    /**
     * Expiry time (in seconds since epoch) when the cached token will expire.
     */
    private volatile long expiresAtSec;

    /**
     * Obtains a valid bearer token for Auth0 Management API calls.
     *
     * <p>This method implements a simple in-memory caching strategy:
     * <ul>
     *   <li>If a valid token is already cached and not near expiry, it returns that.</li>
     *   <li>Otherwise, it makes a POST request to {@code /oauth/token} to fetch a new token.</li>
     * </ul>
     * </p>
     *
     * <h3>HTTP Request</h3>
     * <pre>
     * POST {domain}/oauth/token
     * Content-Type: application/json
     *
     * {
     *   "grant_type": "client_credentials",
     *   "client_id": "...",
     *   "client_secret": "...",
     *   "audience": "https://dev-wgk04dj5v68sbhre.us.auth0.com/api/v2/"
     * }
     * </pre>
     *
     * <h3>Response</h3>
     * <pre>
     * {
     *   "access_token": "...",
     *   "token_type": "Bearer",
     *   "expires_in": 86400
     * }
     * </pre>
     *
     * @return a {@code Bearer <token>} string ready to be used in Authorization headers
     */
    public synchronized String getBearer() {
        long now = System.currentTimeMillis() / 1000;

        // 1️⃣ Reuse cached token if still valid (30s buffer before expiry)
        if (cachedToken != null && now < (expiresAtSec - 30)) {
            return "Bearer " + cachedToken;
        }

        // 2️⃣ Build the token request headers and body
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> b = new HashMap<>();
        b.put("grant_type", "client_credentials");
        b.put("client_id", clientId);
        b.put("client_secret", clientSecret);
        b.put("audience", mgmtAudience);

        // 3️⃣ Call Auth0 /oauth/token to get a new Management API token
        ResponseEntity<Map> resp = rt.postForEntity(
                domain + "/oauth/token",
                new HttpEntity<>(b, h),
                Map.class
        );

        Map body = resp.getBody();
        if (body == null || body.get("access_token") == null) {
            throw new RuntimeException("Failed to obtain Auth0 management token");
        }

        // 4️⃣ Cache token and calculate expiry time
        cachedToken = (String) body.get("access_token");
        Integer exp = (Integer) body.get("expires_in");
        expiresAtSec = now + (exp == null ? 300 : exp);

        // ✅ Return the bearer string
        return "Bearer " + cachedToken;
    }
}
