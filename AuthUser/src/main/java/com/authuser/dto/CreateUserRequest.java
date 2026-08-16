package com.authuser.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public  class CreateUserRequest {
        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        private String email;

        @NotBlank(message = "password is required")
        private String password;

        // The customerId doubles as the Auth0 username, and Auth0 rejects usernames
        // longer than 15 characters. Validating here turns that into a 400 with a
        // usable message instead of a 500 carrying a raw Auth0 error.
        @NotBlank(message = "customerId is required")
        @Size(max = 15, message = "customerId must be 15 characters or fewer (it is used as the Auth0 username)")
        private String customerId;

        // Deliberately has no default: provisioning an admin and provisioning a customer
        // must be an explicit decision at the call site, not something a caller falls into.
        @NotBlank(message = "role is required (ROLE_CUSTOMER or ROLE_ADMIN)")
        private String role;
    }
