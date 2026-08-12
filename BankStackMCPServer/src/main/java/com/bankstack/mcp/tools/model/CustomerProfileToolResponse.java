package com.bankstack.mcp.tools.model;

public record CustomerProfileToolResponse(
        String externalId,
        String fullName,
        String email,
        String phone,
        String address,
        Boolean active,
        String kycStatus,
        String message
) {}