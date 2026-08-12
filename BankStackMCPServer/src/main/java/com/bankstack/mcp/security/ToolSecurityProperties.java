package com.bankstack.mcp.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@ConfigurationProperties(prefix = "bankstack.tool-security")
@Validated
public record ToolSecurityProperties(
        @NotBlank String customerRole,
        @NotBlank String adminRole,
        @NotBlank String internalStaffRole,
        @DefaultValue List<String> customerAuthorities,
        @DefaultValue List<String> adminAuthorities,
        @DefaultValue List<String> internalStaffAuthorities,
        @NotBlank String customerExternalIdClaim
) {}
