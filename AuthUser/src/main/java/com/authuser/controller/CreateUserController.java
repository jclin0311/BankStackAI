package com.authuser.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.authuser.dto.CreateUserRequest;
import com.authuser.service.Auth0UserService;

/**
 * REST controller responsible for provisioning new users in Auth0.
 *
 * <p>This controller exposes an internal administrative endpoint used by backend
 * services (such as CustomerService after KYC approval) to create new database
 * users in Auth0. It is secured with RBAC and requires the caller to have the
 * {@code admin:users.write} permission on their access token.</p>
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li>This controller is part of the "identity-admin" service and is the only component
 *       allowed to interact directly with the Auth0 Management API.</li>
 *   <li>Access tokens must include the {@code SCOPE_admin:users.write} authority for the request
 *       to be accepted.</li>
 *   <li>The service uses client credentials flow for M2M calls and should never be exposed to
 *       frontend clients directly.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
public class CreateUserController {

    private final Auth0UserService auth0;

    /**
     * Constructs a {@code CreateUserController} with the provided {@link Auth0UserService}.
     *
     * @param auth0 the service component that communicates with Auth0 Management API
     */
    public CreateUserController(Auth0UserService auth0) {
        this.auth0 = auth0;
    }

    /**
     * Creates a new user in Auth0 using the Management API.
     *
     * <p>This endpoint is protected with {@link PreAuthorize} and requires
     * {@code SCOPE_admin:users.write} authority. Typically invoked by internal backend
     * services after a customer completes KYC verification.</p>
     *
     * <h3>Request</h3>
     * <pre>
     * POST /api/v1/iam/users
     * Authorization: Bearer &lt;access_token&gt;
     * Content-Type: application/json
     *
     * {
     *   "email": "customer@example.com",
     *   "password": "Temp@1234",
     *   "customerId": "ext-12345"
     * }
     * </pre>
     *
     * <h3>Response</h3>
     * Returns a JSON map containing the created Auth0 user's details, including the
     * generated {@code user_id}.
     *
     * @param req the user creation request payload containing email, password, and customerId
     * @return a {@link ResponseEntity} containing Auth0's user object
     */
    @PreAuthorize("hasAuthority('SCOPE_admin:users.write')")
    @PostMapping("/iam/users")
    public ResponseEntity<Map> createUser(@RequestBody CreateUserRequest req) {
        // Delegate to service layer to create the user in Auth0's database connection.
        Map u = auth0.createDbUser(req.getEmail(), req.getPassword(), req.getCustomerId());

        // Return the Auth0 user object as JSON.
        return ResponseEntity.ok(u);
    }

}
