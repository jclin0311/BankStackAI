package com.authuser.service;

import com.authuser.config.Auth0RoleProperties;
import com.commons.exception.BadRequestException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service responsible for interacting with the Auth0 Management API.
 *
 * <p>This class encapsulates all calls to Auth0's Management API for:
 * <ul>
 *   <li>Creating new database (Username-Password-Authentication) users</li>
 *   <li>Assigning roles to users after creation</li>
 * </ul>
 *
 * <p>By centralizing Auth0 API calls here, we ensure that:
 * <ul>
 *   <li>Only this service knows about the Management API credentials.</li>
 *   <li>All microservices can delegate identity provisioning to this service instead of calling Auth0 directly.</li>
 *   <li>Our code follows the principle of least privilege — one service holds admin capabilities.</li>
 * </ul>
 */
@Service
@Slf4j
public class Auth0UserService {

    /**
     * Auth0 domain (tenant URL), e.g. https://dev-wgk04dj5v68sbhre.us.auth0.com
     * Configured in application.yml as auth0.domain
     */
    @Value("${auth0.domain}")
    private String domain;

    /** Role name → Auth0 role id, configured per tenant. */
    private final Auth0RoleProperties roleProperties;

    private final ManagementTokenService tokens;
    private final RestTemplate rt = new RestTemplate();

    /**
     * Constructor injection of the ManagementTokenService, which is responsible for
     * obtaining a valid Management API access token using the Client Credentials flow.
     *
     * @param tokens service that provides the "Bearer" token for Auth0 Management API calls
     */
    public Auth0UserService(ManagementTokenService tokens, Auth0RoleProperties roleProperties) {
        this.tokens = tokens;
        this.roleProperties = roleProperties;
    }

    /**
     * Creates a new user in Auth0's "Username-Password-Authentication" database connection.
     *
     * <p>This is typically called after a customer completes KYC in CustomerService.
     * The method uses the Management API to provision the user with an email, password,
     * username (we map to customerId), and optional custom metadata.</p>
     *
     * <h3>HTTP Request</h3>
     * POST {domain}/api/v2/users
     *
     * @param email       the user's email
     * @param password    the initial password
     * @param customerId  external customer ID (used as username and stored in app_metadata)
     * @param role        role name to grant, resolved against auth0.mgmt.roles
     * @return a {@link Map} containing Auth0's created user object (user_id, email, etc.)
     */
    public Map createDbUser(String email, String password, String customerId, String role) {
        // 1️⃣ Retrieve the Management API bearer token
        String auth = tokens.getBearer();

        // 2️⃣ Set up HTTP headers
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Authorization", auth);

        // 3️⃣ Build the request body for the new user
        Map<String, Object> body = new HashMap<>();
        List<String> roles =new ArrayList<String>();
       
        body.put("email", email);
        body.put("password", password);
        body.put("username", customerId); // we use customerId as username
        body.put("connection", "Username-Password-Authentication");
        
        // 4️⃣ Include custom metadata (e.g., customer_id) to link Auth0 user back to our domain model
        Map<String, Object> appMeta = new HashMap<>();
        appMeta.put("customer_id", customerId);

        body.put("app_metadata", appMeta);

        // 5️⃣ Call Auth0's Management API to create the user
        ResponseEntity<Map> resp = rt.postForEntity(
                domain + "/api/v2/users",
                new HttpEntity<>(body, h),
                Map.class
        );

        // 6️⃣ Throw an exception if creation failed
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Auth0 user creation failed: " + resp.getStatusCode());
        }

        
        Map createdUser = resp.getBody();
        String userId = (String) createdUser.get("user_id"); // e.g. auth0|abc123
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Auth0 user created but user_id missing");
        }

        assignRequestedRole(userId, role, auth);
        
        
        // ✅ Return the created user object
        return resp.getBody();
    }

    
    /**
     * Grants the caller-requested role to a newly created user.
     *
     * <p>If assignment fails the user is deleted again, so the caller sees a single failed
     * operation instead of an account that exists but carries no entitlements — and so a
     * retry is not blocked by a 409 "user already exists". When the Management token lacks
     * {@code delete:users} the account cannot be removed; the error then names the user id
     * so an operator can finish or delete it by hand.</p>
     */
    private void assignRequestedRole(String userId, String role, String bearer) {
        String roleId = resolveRoleId(role, userId, bearer);

        try {
            assignRole(userId, roleId, bearer);
        } catch (RestClientException assignFailure) {
            log.error("Role {} ({}) could not be assigned to user {}; rolling the user back",
                    role, roleId, userId, assignFailure);
            rollback(userId, bearer, assignFailure, roleId);
            throw new IllegalStateException(
                    "Auth0 role " + roleId + " could not be assigned; user creation was rolled back. "
                            + "Check auth0.mgmt.roles." + role + " exists in this tenant.", assignFailure);
        }
    }

    /**
     * Resolves a caller-supplied role name to its tenant role id. An unknown name is a
     * client error, so the half-created user is removed before reporting it.
     */
    private String resolveRoleId(String role, String userId, String bearer) {
        String roleId = roleProperties.getRoles().get(role);
        if (roleId == null || roleId.isBlank()) {
            log.warn("Unknown role {} requested; rolling user {} back", role, userId);
            rollback(userId, bearer, null, null);
            throw new BadRequestException("Unknown role '" + role + "'. Known roles: "
                    + roleProperties.getRoles().keySet());
        }
        return roleId;
    }

    /** Removes a user created moments ago, so a failed provisioning leaves nothing behind. */
    private void rollback(String userId, String bearer, RestClientException cause, String roleId) {
        try {
            deleteUser(userId, bearer);
        } catch (RestClientException deleteFailure) {
            log.error("Rollback failed; Auth0 user {} exists with no role", userId, deleteFailure);
            throw new IllegalStateException(
                    "Auth0 user " + userId + " was created but role " + roleId
                            + " could not be assigned, and the user could not be removed. "
                            + "Assign the role or delete the user manually.", cause);
        }
    }

    private void assignRole(String userId, String roleId, String bearer) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Authorization", bearer);

        Map<String, Object> body = Map.of("roles", List.of(roleId));

        // Pass the raw user id (auth0|abc123): RestTemplate encodes the URI template
        // itself, so pre-encoding the '|' here would double-escape it and Auth0 rejects
        // the path with "invalid_uri".
        String url = domain + "/api/v2/users/" + userId + "/roles";

        rt.postForEntity(
            url,
            new HttpEntity<>(body, h),
            Void.class
        );
    }

    private void deleteUser(String userId, String bearer) {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", bearer);

        rt.exchange(
            domain + "/api/v2/users/" + userId,
            HttpMethod.DELETE,
            new HttpEntity<>(h),
            Void.class
        );
    }
    
    
}
